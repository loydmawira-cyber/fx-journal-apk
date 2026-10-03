package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TradeEntity
import com.example.data.TradeRepository
import com.example.model.Trade
import com.example.model.TradeComment
import com.example.model.TradeDirection
import com.example.model.TradeStatus
import com.example.model.TradeVisibility
import com.example.model.TraderProfile
import com.example.model.FxUser
import com.example.model.AppNotification
import com.example.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.components.AvatarStore
import com.google.firebase.firestore.FieldValue
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FollowDoc(
    val followerUid: String,
    val traderUid: String,
    val followerName: String,
    val followerHandle: String,
    val traderName: String,
    val traderHandle: String
)

data class VoteDoc(val tradeDocId: String, val voterUid: String, val value: Int)

data class CloudComment(
    val id: String,
    val tradeDocId: String,
    val authorUid: String,
    val authorName: String,
    val authorHandle: String,
    val authorInitials: String,
    val content: String,
    val timestamp: Long,
    val parentId: String,
    val likedBy: List<String>,
    val imageB64: String? = null
)

enum class AppNavScreen {
    FEED, JOURNAL, TRADERS, BREAKDOWN, TRADE_DETAIL, LOG_TRADE, LOGIN, SIGNUP, FORGOT_PASSWORD, SETTINGS
}

class FxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradeRepository
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    // Uses the default Firestore database of the project in google-services.json
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val securityPrefs = application.getSharedPreferences("fx_journal_security", Context.MODE_PRIVATE)

    private fun userRoot() = auth.currentUser?.let { firestore.collection("users").document(it.uid) }
    private val tradeAdapter by lazy { Moshi.Builder().build().adapter(TradeEntity::class.java) }

    // Public posts from OTHER traders, loaded live from Firestore
    private val _communityTrades = MutableStateFlow<List<Trade>>(emptyList())
    private var followedTraderIds: Set<String> = emptySet()
    private var publicTradesListener: ListenerRegistration? = null

    private fun parseTrade(json: String?): Trade? = try {
        if (json == null) null else tradeAdapter.fromJson(json)?.toDomain()
    } catch (e: Exception) {
        android.util.Log.e("FxViewModel", "Could not parse trade", e)
        null
    }

    private fun relativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    }

    private val _votes = MutableStateFlow<List<VoteDoc>>(emptyList())
    private val _commentDocs = MutableStateFlow<List<CloudComment>>(emptyList())
    @Volatile private var communityDocIds: Map<Long, String> = emptyMap()
    private var votesListener: ListenerRegistration? = null
    private var discussionListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private val chartCache = mutableMapOf<String, String>()
    // Other traders' posts the user saved (snapshot copies)
    private val _savedOthers = MutableStateFlow<List<Trade>>(emptyList())
    @Volatile private var savedDocIds: Map<Long, String> = emptyMap()
    private val _privateCommentDocs = MutableStateFlow<List<CloudComment>>(emptyList())
    private val _follows = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // follower -> trader
    private val _followDocs = MutableStateFlow<List<FollowDoc>>(emptyList())
    private var savedListener: ListenerRegistration? = null
    private var privateDiscussionListener: ListenerRegistration? = null
    private var followsListener: ListenerRegistration? = null
    private var traderSource: List<Pair<Trade, String>> = emptyList()

    private fun negativeIdFor(docId: String): Long = -(Math.abs(docId.hashCode().toLong()) + 1)

    // ---------- Chart screenshots (stored as a small JPEG inside the trade document) ----------
    private fun encodeChart(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        chartCache[uriString]?.let { return it }
        return try {
            val resolver = getApplication<Application>().contentResolver
            val uri = Uri.parse(uriString)
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val longest = maxOf(bmp.width, bmp.height)
            val scaled = if (longest > 1000) {
                val r = 1000f / longest
                android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * r).toInt(), (bmp.height * r).toInt(), true)
            } else bmp
            var quality = 65
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 15
            } while (bytes.size > 450_000 && quality >= 20)
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            chartCache[uriString] = b64
            b64
        } catch (e: Exception) {
            android.util.Log.e("FxViewModel", "Could not encode chart image", e)
            null
        }
    }

    /** Copy a picked screenshot into the app's own storage so it never expires (picker links stop working later). */
    private fun copyChartToPrivateStorage(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val b64 = encodeChart(uriString) ?: return uriString
        return try {
            val dir = File(getApplication<Application>().filesDir, "charts").apply { mkdirs() }
            val file = File(dir, "local_${System.currentTimeMillis()}.jpg")
            file.writeBytes(Base64.decode(b64, Base64.DEFAULT))
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            android.util.Log.e("FxViewModel", "Could not keep chart image", e)
            uriString
        }
    }

    private fun restoreChart(docId: String, b64: String?): String? {
        if (b64.isNullOrBlank()) return null
        return try {
            val dir = File(getApplication<Application>().filesDir, "charts").apply { mkdirs() }
            val file = File(dir, "$docId.jpg")
            if (!file.exists()) file.writeBytes(Base64.decode(b64, Base64.DEFAULT))
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            android.util.Log.e("FxViewModel", "Could not restore chart image", e)
            null
        }
    }

    // ---------- Profile pictures (small square JPEG stored in the user's Firestore document) ----------
    private val avatarRequested = mutableSetOf<String>()

    private fun encodeAvatar(uri: Uri): ByteArray? = try {
        val resolver = getApplication<Application>().contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 256) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        if (bmp == null) null else {
            // centre-crop to a square, then shrink to 256px
            val side = minOf(bmp.width, bmp.height)
            val square = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val small = android.graphics.Bitmap.createScaledBitmap(square, 256, 256, true)
            val out = ByteArrayOutputStream()
            small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    } catch (e: Exception) {
        android.util.Log.e("FxViewModel", "Could not process profile photo", e)
        null
    }

    private fun saveAvatarFile(name: String, bytes: ByteArray): String? = try {
        val dir = File(getApplication<Application>().filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "$name.jpg")
        if (!file.exists()) file.writeBytes(bytes)
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        android.util.Log.e("FxViewModel", "Could not save profile photo", e)
        null
    }

    /** Download the profile pictures of other traders (post authors, commenters) so they show everywhere. */
    private fun ensureAvatars(uids: Collection<String>) {
        val me = auth.currentUser?.uid
        uids.filter { it.isNotBlank() && it != me && avatarRequested.add(it) }.forEach { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val handle = doc.getString("handle")
                    val b64 = doc.getString("photoB64")
                    if (!handle.isNullOrBlank() && !b64.isNullOrBlank()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val uri = saveAvatarFile("${uid}_${b64.hashCode()}", Base64.decode(b64, Base64.DEFAULT))
                            if (uri != null) withContext(Dispatchers.Main) { AvatarStore.put(handle, uri) }
                        }
                    }
                }
                .addOnFailureListener { avatarRequested.remove(uid) }
        }
    }

    private fun persistTradeToCloud(trade: Trade) {
        val uid = auth.currentUser?.uid ?: return
        val json = tradeAdapter.toJson(TradeEntity.fromDomain(trade))
        viewModelScope.launch(Dispatchers.IO) {
            val chart = encodeChart(trade.chartImageUri)
            val docId = "${uid}_${trade.id}"
            // 1) Private copy in the user's own journal
            val own = mutableMapOf<String, Any>(
                "payload" to json, "id" to trade.id,
                "visibility" to trade.visibility.name, "timestamp" to trade.timestamp
            )
            if (chart != null) own["chartB64"] = chart
            firestore.collection("users").document(uid).collection("trades").document(trade.id.toString())
                .set(own, SetOptions.merge())
                .addOnFailureListener { android.util.Log.e("FxViewModel", "Saving trade failed", it) }
            // 2) Shared copy in the public feed (only if PUBLIC)
            val publicRef = firestore.collection("publicTrades").document(docId)
            if (trade.visibility == TradeVisibility.PUBLIC) {
                val shared = mutableMapOf<String, Any>(
                    "payload" to json, "ownerUid" to uid,
                    "localId" to trade.id, "timestamp" to trade.timestamp
                )
                if (chart != null) shared["chartB64"] = chart
                publicRef.set(shared, SetOptions.merge())
                    .addOnFailureListener { android.util.Log.e("FxViewModel", "Publishing trade failed", it) }
            } else {
                publicRef.delete()
            }
        }
    }

    // ---------- Helpers for shared (public) trades ----------
    private fun isShared(trade: Trade) = trade.id < 0 || trade.visibility == TradeVisibility.PUBLIC

    private fun tradeDocIdFor(trade: Trade): String? =
        if (trade.id < 0) (communityDocIds[trade.id] ?: savedDocIds[trade.id]) else auth.currentUser?.uid?.let { "${it}_${trade.id}" }

    private fun findTrade(tradeId: Long): Trade? =
        trades.value.firstOrNull { it.id == tradeId } ?: _communityTrades.value.firstOrNull { it.id == tradeId }
            ?: _savedOthers.value.firstOrNull { it.id == tradeId }

    private fun decorateShared(
        t: Trade, docId: String?, votesBy: Map<String, List<VoteDoc>>,
        commentsBy: Map<String, List<CloudComment>>, myUid: String?
    ): Trade {
        if (docId == null) return t.copy(timeAgo = relativeTime(t.timestamp))
        val v = votesBy[docId].orEmpty()
        return t.copy(
            upvotes = v.count { it.value > 0 },
            downvotes = v.count { it.value < 0 },
            isUpvoted = v.any { it.voterUid == myUid && it.value > 0 },
            isDownvoted = v.any { it.voterUid == myUid && it.value < 0 },
            commentsCount = commentsBy[docId]?.size ?: 0,
            timeAgo = relativeTime(t.timestamp)
        )
    }

    private fun parseComment(d: DocumentSnapshot): CloudComment? {
        val tradeDocId = d.getString("tradeDocId") ?: return null
        return CloudComment(
            id = d.id,
            tradeDocId = tradeDocId,
            authorUid = d.getString("authorUid") ?: "",
            authorName = d.getString("authorName") ?: "Trader",
            authorHandle = d.getString("authorHandle") ?: "@trader",
            authorInitials = d.getString("authorInitials") ?: "T",
            content = d.getString("content") ?: "",
            timestamp = d.getLong("timestamp") ?: 0L,
            parentId = d.getString("parentId") ?: "",
            likedBy = (d.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            imageB64 = d.getString("imageB64")
        )
    }

    private fun notifyUser(targetUid: String, type: String, title: String, body: String) {
        val id = "n_${System.currentTimeMillis()}_${(0..9999).random()}"
        val n = AppNotification(id, type, title, body, System.currentTimeMillis(), false)
        firestore.collection("users").document(targetUid).collection("notifications").document(id).set(n)
    }

    val trades: StateFlow<List<Trade>>
    // Own public trades + public trades from other traders (used by the Feed)
    val feedTrades: StateFlow<List<Trade>>
    // Own trades with live vote / comment counts (used by the Journal)
    val myTrades: StateFlow<List<Trade>>
    // Other traders' posts the user saved
    val savedOthers: StateFlow<List<Trade>>

    /** Build a user straight from the saved login so the app can open instantly (no waiting for the network). */
    private fun quickUserFrom(fu: com.google.firebase.auth.FirebaseUser): FxUser {
        val sameAccount = securityPrefs.getString("profile_uid", null) == fu.uid
        val name = (if (sameAccount) securityPrefs.getString("profile_name", null) else null)
            ?: fu.displayName?.takeIf { it.isNotBlank() }
            ?: fu.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Trader"
        val handle = (if (sameAccount) securityPrefs.getString("profile_handle", null) else null)
            ?: "@${name.replace(" ", "").lowercase()}"
        return FxUser(
            uid = fu.uid,
            email = fu.email ?: "",
            name = name,
            handle = handle,
            photoUri = if (sameAccount) securityPrefs.getString("profile_photo", null) else null,
            publicPostAudience = (if (sameAccount) securityPrefs.getString("profile_audience", null) else null) ?: "everyone"
        )
    }

    // Start already signed in if a saved login exists: no login-page flash while the profile loads
    private val _currentUser = MutableStateFlow<FxUser?>(
        runCatching { auth.currentUser }.getOrNull()?.let { quickUserFrom(it) }
    )
    val currentUser: StateFlow<FxUser?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            _currentUser.collect { u ->
                val photo = u?.photoUri
                if (u != null && !photo.isNullOrBlank()) AvatarStore.put(u.handle, photo)
            }
        }
    }

    // People who follow me / people I follow
    val followers: StateFlow<List<FollowDoc>> = combine(_followDocs, _currentUser) { docs, _ ->
        val me = auth.currentUser?.uid
        docs.filter { it.traderUid == me }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val following: StateFlow<List<FollowDoc>> = combine(_followDocs, _currentUser) { docs, _ ->
        val me = auth.currentUser?.uid
        docs.filter { it.followerUid == me }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Defensive Firebase check
        try {
            // Observe Auth State
            auth.addAuthStateListener { firebaseAuth ->
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    if (_currentUser.value?.uid != firebaseUser.uid) _currentUser.value = quickUserFrom(firebaseUser)
                    syncLocalThenLoad(firebaseUser.uid)
                    listenPublicTrades()
                    loadNotifications(firebaseUser.uid)
                    listenFollows()
                    listenSavedPosts(firebaseUser.uid)
                    listenPrivateDiscussion(firebaseUser.uid)
                    // Fetch extra user data from Firestore
                    firestore.collection("users").document(firebaseUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val name = document.getString("name")?.takeIf { it.isNotBlank() }
                                    ?: securityPrefs.getString("profile_name", null)
                                    ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                                    ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                                    ?: "Trader"
                                val handle = document.getString("handle")?.takeIf { it.isNotBlank() }
                                    ?: securityPrefs.getString("profile_handle", null)
                                    ?: "@${name.replace(" ", "").lowercase()}"
                                securityPrefs.edit()
                                    .putString("profile_uid", firebaseUser.uid)
                                    .putString("profile_name", name)
                                    .putString("profile_handle", handle)
                                    .apply()
                                _currentUser.value = FxUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = name,
                                    handle = handle,
                                    photoUri = document.getString("photoB64")?.takeIf { it.isNotBlank() }?.let { b64 ->
                                        saveAvatarFile("${firebaseUser.uid}_${b64.hashCode()}", Base64.decode(b64, Base64.DEFAULT))
                                    } ?: securityPrefs.getString("profile_photo", null),
                                    publicPostAudience = document.getString("publicPostAudience") ?: securityPrefs.getString("profile_audience", "everyone") ?: "everyone"
                                )
                            } else {
                                _currentUser.value = FxUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Trader",
                                    handle = "@${(firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "trader").replace(" ", "").lowercase()}"
                                )
                            }
                        }
                        .addOnFailureListener {
                            // If Firestore fails, at least set the auth user
                            _currentUser.value = FxUser(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Trader",
                        handle = "@${(firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "trader").replace(" ", "").lowercase()}"
                            )
                        }
                } else {
                    _currentUser.value = null
                    publicTradesListener?.remove(); publicTradesListener = null
                    votesListener?.remove(); votesListener = null
                    discussionListener?.remove(); discussionListener = null
                    notificationsListener?.remove(); notificationsListener = null
                    _communityTrades.value = emptyList()
                    _votes.value = emptyList()
                    _commentDocs.value = emptyList()
                    _notifications.value = emptyList()
                    savedListener?.remove(); savedListener = null
                    privateDiscussionListener?.remove(); privateDiscussionListener = null
                    followsListener?.remove(); followsListener = null
                    _savedOthers.value = emptyList(); savedDocIds = emptyMap()
                    _privateCommentDocs.value = emptyList()
                    _follows.value = emptyList()
                    _followDocs.value = emptyList()
                    traderSource = emptyList()
                    _traders.value = emptyList()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FxViewModel", "Firebase initialization failed", e)
        }
        val database = AppDatabase.getDatabase(application)
        repository = TradeRepository(database.tradeDao())
        trades = repository.allTrades.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        myTrades = combine(trades, _votes, _commentDocs, _privateCommentDocs, _currentUser) { own, votes, comments, priv, _ ->
            val myUid = auth.currentUser?.uid
            val votesBy = votes.groupBy { it.tradeDocId }
            val commentsBy = (comments + priv).groupBy { it.tradeDocId }
            own.map { decorateShared(it, myUid?.let { u -> "${u}_${it.id}" }, votesBy, commentsBy, myUid) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savedOthers = combine(_savedOthers, _votes, _commentDocs, _currentUser) { saved, votes, comments, _ ->
            val myUid = auth.currentUser?.uid
            val votesBy = votes.groupBy { it.tradeDocId }
            val commentsBy = comments.groupBy { it.tradeDocId }
            saved.map { decorateShared(it, savedDocIds[it.id], votesBy, commentsBy, myUid).copy(isBookmarked = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val feedBase = combine(myTrades, _communityTrades, _votes, _commentDocs, _currentUser) { own, others, votes, comments, _ ->
            val myUid = auth.currentUser?.uid
            val votesBy = votes.groupBy { it.tradeDocId }
            val commentsBy = comments.groupBy { it.tradeDocId }
            own.filter { it.visibility == TradeVisibility.PUBLIC } +
                others.map { decorateShared(it, communityDocIds[it.id], votesBy, commentsBy, myUid) }
        }
        feedTrades = combine(feedBase, _savedOthers) { list, saved ->
            val savedIds = saved.map { it.id }.toSet()
            list.map { if (it.id < 0 && it.id in savedIds) it.copy(isBookmarked = true) else it }
                .sortedByDescending { it.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        auth.currentUser?.uid?.let { loadCloudTrades(it) }
        auth.currentUser?.uid?.let { loadNotifications(it) }
        auth.currentUser?.uid?.let { loadNotificationPreferences(it) }
    }

    // Upload anything saved only on this phone, then download the cloud journal
    private fun syncLocalThenLoad(uid: String) {
        viewModelScope.launch {
            try {
                repository.allTrades.first().forEach { persistTradeToCloud(it) }
            } catch (e: Exception) {
                android.util.Log.e("FxViewModel", "Local sync failed", e)
            }
            loadCloudTrades(uid)
        }
    }

    private fun loadCloudTrades(uid: String) {
        firestore.collection("users").document(uid).collection("trades").get()
            .addOnSuccessListener { snapshot ->
                viewModelScope.launch {
                    snapshot.documents.mapNotNull { d ->
                        parseTrade(d.getString("payload"))?.let { t ->
                            val uri = restoreChart("${uid}_${t.id}", d.getString("chartB64"))
                            if (uri != null) t.copy(chartImageUri = uri) else t
                        }
                    }.forEach { repository.insertTrade(it) }
                }
            }
            .addOnFailureListener { android.util.Log.e("FxViewModel", "Loading trades failed", it) }
    }

    // Live feed of every trader's public posts (+ votes and comments)
    private fun listenPublicTrades() {
        publicTradesListener?.remove()
        publicTradesListener = firestore.collection("publicTrades")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FxViewModel", "Public feed failed", error)
                    return@addSnapshotListener
                }
                val myUid = auth.currentUser?.uid
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val owner = doc.getString("ownerUid") ?: return@mapNotNull null
                    if (owner == myUid) return@mapNotNull null
                    val trade = parseTrade(doc.getString("payload")) ?: return@mapNotNull null
                    val chartUri = restoreChart(doc.id, doc.getString("chartB64")) ?: trade.chartImageUri
                    Triple(
                        trade.copy(
                            // negative id so it never collides with local trades
                            id = negativeIdFor(doc.id),
                            chartImageUri = chartUri,
                            isUpvoted = false, isDownvoted = false, isBookmarked = false
                        ),
                        owner, doc.id
                    )
                } ?: emptyList()
                communityDocIds = items.associate { it.first.id to it.third }
                _communityTrades.value = items.map { it.first }
                ensureAvatars(items.map { it.second })
                traderSource = items.map { it.first to it.second }
                rebuildTraders()
            }
        listenVotes()
        listenDiscussion()
    }

    private fun listenVotes() {
        votesListener?.remove()
        votesListener = firestore.collection("votes").addSnapshotListener { snapshot, error ->
            if (error != null) { android.util.Log.e("FxViewModel", "Votes failed", error); return@addSnapshotListener }
            _votes.value = snapshot?.documents?.mapNotNull { d ->
                VoteDoc(
                    tradeDocId = d.getString("tradeDocId") ?: return@mapNotNull null,
                    voterUid = d.getString("voterUid") ?: return@mapNotNull null,
                    value = (d.getLong("value") ?: 0L).toInt()
                )
            } ?: emptyList()
        }
    }

    private fun listenDiscussion() {
        discussionListener?.remove()
        discussionListener = firestore.collectionGroup("discussion").addSnapshotListener { snapshot, error ->
            if (error != null) { android.util.Log.e("FxViewModel", "Comments failed", error); return@addSnapshotListener }
            _commentDocs.value = snapshot?.documents?.mapNotNull { parseComment(it) } ?: emptyList()
            ensureAvatars(_commentDocs.value.map { it.authorUid })
        }
    }

    // Build the Traders list from the people who have posted publicly
    private fun rebuildTraders() {
        val follows = _follows.value
        _traders.value = traderSource.groupBy({ it.second }, { it.first }).map { (ownerUid, list) ->
            val latest = list.maxByOrNull { it.timestamp }!!
            val wins = list.count { it.rMultiple > 0 }
            val winRate = if (list.isNotEmpty()) (wins * 100 / list.size) else 0
            val followerCount = follows.count { it.second == ownerUid }
            TraderProfile(
                id = ownerUid,
                name = latest.authorName,
                handle = latest.authorHandle,
                tier = latest.authorTier,
                tierLabel = latest.authorTier.label,
                winRate = "$winRate%",
                streak = "${list.size} posts",
                netRGain = String.format("%+.1fR", list.sumOf { it.rMultiple }),
                followers = followerCount.toString(),
                isVerified = latest.isAuthorVerified,
                isFollowing = ownerUid in followedTraderIds
            )
        }
    }

    // Who follows whom (one small document per follow)
    private fun listenFollows() {
        followsListener?.remove()
        followsListener = firestore.collection("follows").addSnapshotListener { snapshot, error ->
            if (error != null) { android.util.Log.e("FxViewModel", "Follows failed", error); return@addSnapshotListener }
            val docs = snapshot?.documents?.mapNotNull { d ->
                val f = d.getString("followerUid") ?: return@mapNotNull null
                val t = d.getString("traderUid") ?: return@mapNotNull null
                val known = { uid: String -> traderSource.firstOrNull { it.second == uid }?.first }
                FollowDoc(
                    followerUid = f,
                    traderUid = t,
                    followerName = d.getString("followerName") ?: known(f)?.authorName ?: "Trader",
                    followerHandle = d.getString("followerHandle") ?: known(f)?.authorHandle ?: "@trader",
                    traderName = d.getString("traderName") ?: known(t)?.authorName ?: "Trader",
                    traderHandle = d.getString("traderHandle") ?: known(t)?.authorHandle ?: "@trader"
                )
            } ?: emptyList()
            _followDocs.value = docs
            val pairs = docs.map { it.followerUid to it.traderUid }
            _follows.value = pairs
            val me = auth.currentUser?.uid
            followedTraderIds = pairs.filter { it.first == me }.map { it.second }.toSet()
            rebuildTraders()
        }
    }

    // Other traders' posts the user saved
    private fun listenSavedPosts(uid: String) {
        savedListener?.remove()
        savedListener = firestore.collection("users").document(uid).collection("savedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { android.util.Log.e("FxViewModel", "Saved posts failed", error); return@addSnapshotListener }
                val items = snapshot?.documents?.mapNotNull { d ->
                    val trade = parseTrade(d.getString("payload")) ?: return@mapNotNull null
                    val chartUri = restoreChart(d.id, d.getString("chartB64")) ?: trade.chartImageUri
                    trade.copy(
                        id = negativeIdFor(d.id), chartImageUri = chartUri,
                        isBookmarked = true, isUpvoted = false, isDownvoted = false
                    ) to d.id
                } ?: emptyList()
                savedDocIds = items.associate { it.first.id to it.second }
                _savedOthers.value = items.map { it.first }
            }
    }

    // Comments on the user's own private trades
    private fun listenPrivateDiscussion(uid: String) {
        privateDiscussionListener?.remove()
        privateDiscussionListener = firestore.collection("users").document(uid).collection("privateDiscussion")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { android.util.Log.e("FxViewModel", "Private comments failed", error); return@addSnapshotListener }
                _privateCommentDocs.value = snapshot?.documents?.mapNotNull { parseComment(it) } ?: emptyList()
            }
    }

    private fun loadNotifications(uid: String) {
        notificationsListener?.remove()
        notificationsListener = firestore.collection("users").document(uid).collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { android.util.Log.e("FxViewModel", "Notifications failed", error); return@addSnapshotListener }
                _notifications.value = snapshot?.documents?.mapNotNull { it.toObject(AppNotification::class.java) } ?: emptyList()
            }
    }

    private fun loadNotificationPreferences(uid: String) {
        firestore.collection("users").document(uid).collection("preferences").document("notifications").get()
            .addOnSuccessListener { document ->
                val values = _notificationPreferences.value.toMutableMap()
                notificationKeys.forEach { key -> document.getBoolean(key)?.let { values[key] = it; securityPrefs.edit().putBoolean("notify_$key", it).apply() } }
                _notificationPreferences.value = values
            }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppNavScreen.FEED)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    private val _selectedTrade = MutableStateFlow<Trade?>(null)
    // Live version of the open trade (votes/comment counts update in real time)
    val selectedTrade: StateFlow<Trade?> = combine(_selectedTrade, feedTrades, myTrades, savedOthers) { sel, feed, own, saved ->
        sel?.let { s -> feed.firstOrNull { it.id == s.id } ?: own.firstOrNull { it.id == s.id } ?: saved.firstOrNull { it.id == s.id } ?: s }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Theme Mode
    private val _themeMode = MutableStateFlow(
        securityPrefs.getString("theme_mode", null)
            ?.let { saved -> runCatching { ThemeMode.valueOf(saved) }.getOrNull() }
            ?: ThemeMode.AMOLED
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Modals & Bottom Sheets
    private val _showCreateLogSheet = MutableStateFlow(false)
    val showCreateLogSheet: StateFlow<Boolean> = _showCreateLogSheet.asStateFlow()

    private val _showThemeModal = MutableStateFlow(false)
    val showThemeModal: StateFlow<Boolean> = _showThemeModal.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()
    private val notificationKeys = listOf("followingPosts", "likes", "comments", "newFollowers")
    private val _notificationPreferences = MutableStateFlow(notificationKeys.associateWith { key -> securityPrefs.getBoolean("notify_$key", true) })
    val notificationPreferences: StateFlow<Map<String, Boolean>> = _notificationPreferences.asStateFlow()
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    val unreadNotificationCount: Int get() = _notifications.value.count { !it.read }

    // Feed Filters
    private val _feedTab = MutableStateFlow("public") // "public" or "following"
    val feedTab: StateFlow<String> = _feedTab.asStateFlow()

    private val _feedPairFilter = MutableStateFlow("All Pairs")
    val feedPairFilter: StateFlow<String> = _feedPairFilter.asStateFlow()

    private val _feedStrategyFilter = MutableStateFlow("All")
    val feedStrategyFilter: StateFlow<String> = _feedStrategyFilter.asStateFlow()

    // Journal Filters
    private val _journalTab = MutableStateFlow("all") // "all", "private", "public"
    val journalTab: StateFlow<String> = _journalTab.asStateFlow()

    private val _journalPairFilter = MutableStateFlow<String?>(null)
    val journalPairFilter: StateFlow<String?> = _journalPairFilter.asStateFlow()

    // Toast notification message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Discussion comments are loaded per trade from Firestore and start empty.
    val tradeComments: StateFlow<List<TradeComment>> =
        combine(_selectedTrade, _commentDocs, _privateCommentDocs, _currentUser) { sel, docs, priv, _ ->
            if (sel == null) emptyList()
            else {
                val docId = tradeDocIdFor(sel)
                val mine = auth.currentUser?.uid
                val ownerUid = docId?.substringBefore('_')
                fun toModel(c: CloudComment, replies: List<TradeComment> = emptyList()) = TradeComment(
                    id = c.id,
                    authorName = c.authorName,
                    authorHandle = c.authorHandle,
                    authorInitials = c.authorInitials,
                    isAuthorBadge = c.authorUid == ownerUid,
                    timeAgo = relativeTime(c.timestamp),
                    content = c.content,
                    likesCount = c.likedBy.size,
                    isLiked = mine != null && mine in c.likedBy,
                    replies = replies,
                    imageUri = c.imageB64?.takeIf { it.isNotBlank() }?.let { restoreChart("cmt_${c.id}", it) }
                )
                val forTrade = if (docId == null) emptyList() else (docs + priv).filter { it.tradeDocId == docId }
                forTrade.filter { it.parentId.isBlank() }.sortedByDescending { it.timestamp }.map { c ->
                    toModel(c, forTrade.filter { it.parentId == c.id }.sortedBy { it.timestamp }.map { toModel(it) })
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _recentTradeIds = MutableStateFlow<List<Long>>(emptyList())
    val recentTradeIds: StateFlow<List<Long>> = _recentTradeIds.asStateFlow()
    // Community profiles are loaded from Firestore and start empty.
    private val _traders = MutableStateFlow<List<TraderProfile>>(emptyList())
    val traders: StateFlow<List<TraderProfile>> = _traders.asStateFlow()

    private val _appLocked = MutableStateFlow(securityPrefs.getString("pin_hash", null) != null)
    val appLocked: StateFlow<Boolean> = _appLocked.asStateFlow()
    val hasAppPin: Boolean get() = securityPrefs.getString("pin_hash", null) != null
    // Observable copy so Settings updates the moment a PIN is saved or removed
    private val _hasAppPinState = MutableStateFlow(securityPrefs.getString("pin_hash", null) != null)
    val hasAppPinState: StateFlow<Boolean> = _hasAppPinState.asStateFlow()

    fun setAppPin(pin: String, onResult: (Boolean) -> Unit = {}) {
        if (!pin.matches(Regex("\\d{4}"))) { onResult(false); return }
        securityPrefs.edit().putString("pin_hash", hashPin(pin)).apply()
        _hasAppPinState.value = true
        onResult(true)
        showToast("4-digit app lock enabled")
    }

    fun removeAppPin() {
        securityPrefs.edit().remove("pin_hash").apply()
        _hasAppPinState.value = false
        _appLocked.value = false
        showToast("App lock disabled")
    }

    fun lockAppIfConfigured() { if (hasAppPin) _appLocked.value = true }

    fun unlockApp(pin: String): Boolean {
        val valid = securityPrefs.getString("pin_hash", null) == hashPin(pin)
        if (valid) _appLocked.value = false
        return valid
    }

    private fun hashPin(pin: String): String = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()), Base64.NO_WRAP
    )

    fun updateProfile(name: String, handle: String, photoUri: Uri?, audience: String, onDone: () -> Unit = {}) {
        if (photoUri == null) {
            applyProfile(name, handle, null, null, audience, onDone)
            return
        }
        // Shrink the picked photo, keep a permanent copy on the phone and upload it so everyone sees it
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = encodeAvatar(photoUri)
            val b64 = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            val local = if (bytes != null && b64 != null) {
                saveAvatarFile("${auth.currentUser?.uid ?: "me"}_${b64.hashCode()}", bytes)
            } else null
            withContext(Dispatchers.Main) {
                applyProfile(name, handle, local?.let { Uri.parse(it) } ?: photoUri, b64, audience, onDone)
            }
        }
    }

    private fun applyProfile(name: String, handle: String, photoUri: Uri?, photoB64: String?, audience: String, onDone: () -> Unit) {
        val cleanName = name.trim().ifBlank { "Trader" }
        val cleanHandle = "@${handle.trim().removePrefix("@").replace(" ", "").lowercase()}"
        val localUser = _currentUser.value?.copy(
            name = cleanName,
            handle = cleanHandle,
            photoUri = photoUri?.toString() ?: _currentUser.value?.photoUri,
            publicPostAudience = audience
        )
        if (localUser != null) _currentUser.value = localUser
        securityPrefs.edit()
            .putString("profile_uid", auth.currentUser?.uid)
            .putString("profile_name", cleanName)
            .putString("profile_handle", cleanHandle)
            .putString("profile_audience", audience)
            .putString("profile_photo", photoUri?.toString() ?: _currentUser.value?.photoUri)
            .apply()
        onDone()
        showToast("Profile updated")

        val user = auth.currentUser ?: return
        val updates = hashMapOf<String, Any>(
            "name" to cleanName,
            "handle" to cleanHandle,
            "publicPostAudience" to audience
        )
        if (photoB64 != null) updates["photoB64"] = photoB64
        firestore.collection("users").document(user.uid).set(updates, SetOptions.merge())
            .addOnFailureListener { showToast("Saved on this device; cloud sync will retry later") }
    }

    fun navigateTo(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    // Firebase Auth Methods
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                auth.currentUser?.uid?.let { loadCloudTrades(it) }
                showToast("Welcome back!")
                onSuccess()
            }
            .addOnFailureListener {
                showToast("Login failed: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun signup(email: String, password: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "handle" to "@${name.replace(" ", "").lowercase()}",
                        "uid" to user.uid
                    )
                    firestore.collection("users").document(user.uid).set(userData)
                        .addOnSuccessListener {
                            showToast("Account created!")
                            onSuccess()
                        }
                }
            }
            .addOnFailureListener {
                showToast("Signup failed: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun logout() {
        viewModelScope.launch { repository.clearAll() }
        followedTraderIds = emptySet()
        auth.signOut()
        showToast("Logged out successfully")
        navigateTo(AppNavScreen.LOGIN)
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                showToast("Reset failed: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    /** Posts the signed-in user created have local ids >= 0; community / saved posts use negative ids. */
    fun isMyTrade(trade: Trade) = trade.id >= 0

    /** Switch one of my own posts between Public and Private (updates the phone and the cloud). */
    fun changeTradeVisibility(trade: Trade, newVisibility: TradeVisibility) {
        if (!isMyTrade(trade)) return
        viewModelScope.launch {
            val base = trades.value.firstOrNull { it.id == trade.id } ?: trade
            if (base.visibility == newVisibility) return@launch
            val updated = base.copy(visibility = newVisibility)
            repository.updateTrade(updated)
            persistTradeToCloud(updated)
            showToast(if (newVisibility == TradeVisibility.PUBLIC) "Post is now public" else "Post is now private")
        }
    }

    /** Permanently delete one of my own posts from the phone, my journal and the public feed. */
    fun deleteMyTrade(trade: Trade) {
        if (!isMyTrade(trade)) return
        viewModelScope.launch {
            repository.deleteTradeById(trade.id)
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("users").document(uid).collection("trades")
                    .document(trade.id.toString()).delete()
                firestore.collection("publicTrades").document("${uid}_${trade.id}").delete()
            }
            if (_selectedTrade.value?.id == trade.id) _selectedTrade.value = null
            showToast("Post deleted")
        }
    }

    fun openTradeDetail(trade: Trade) {
        _selectedTrade.value = trade
        _recentTradeIds.value = (listOf(trade.id) + _recentTradeIds.value.filterNot { it == trade.id }).take(10)
        userRoot()?.collection("recentViews")?.document(trade.id.toString())?.set(mapOf("tradeId" to trade.id, "viewedAt" to System.currentTimeMillis()), SetOptions.merge())
        _currentScreen.value = AppNavScreen.TRADE_DETAIL
    }

    fun closeTradeDetail() {
        _currentScreen.value = AppNavScreen.FEED
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        securityPrefs.edit().putString("theme_mode", mode.name).apply()
        showToast("Theme switched to: ${mode.title}")
    }

    fun toggleThemeModal(show: Boolean) {
        _showThemeModal.value = show
    }

    fun toggleCreateLogSheet(show: Boolean) {
        _showCreateLogSheet.value = show
    }

    fun toggleNotifications(show: Boolean) {
        _showNotifications.value = show
    }

    fun setNotificationPreference(key: String, enabled: Boolean) {
        if (key !in notificationKeys) return
        _notificationPreferences.value = _notificationPreferences.value.toMutableMap().apply { put(key, enabled) }
        securityPrefs.edit().putBoolean("notify_$key", enabled).apply()
        userRoot()?.collection("preferences")?.document("notifications")?.set(_notificationPreferences.value, SetOptions.merge())
    }

    fun addNotification(type: String, title: String, body: String) {
        if (_notificationPreferences.value[type] != true) return
        val notification = AppNotification("n_${System.currentTimeMillis()}", type, title, body, System.currentTimeMillis(), false)
        _notifications.value = (listOf(notification) + _notifications.value).take(50)
        userRoot()?.collection("notifications")?.document(notification.id)?.set(notification, SetOptions.merge())
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
        _notifications.value.forEach { notification -> userRoot()?.collection("notifications")?.document(notification.id)?.set(notification, SetOptions.merge()) }
    }

    fun setFeedTab(tab: String) {
        _feedTab.value = tab
    }

    fun setFeedPairFilter(pair: String) {
        _feedPairFilter.value = pair
    }

    fun setFeedStrategyFilter(strat: String) {
        _feedStrategyFilter.value = strat
    }

    fun setJournalTab(tab: String) {
        _journalTab.value = tab
    }

    fun setJournalPairFilter(pair: String?) {
        _journalPairFilter.value = if (_journalPairFilter.value == pair) null else pair
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    val draftCount: Int
        get() = securityPrefs.getInt("draft_count", 0)

    fun saveDraft() {
        securityPrefs.edit().putInt("draft_count", draftCount + 1).apply()
        userRoot()?.collection("drafts")?.document("draft_${System.currentTimeMillis()}")?.set(mapOf("createdAt" to System.currentTimeMillis(), "count" to draftCount + 1), SetOptions.merge())
        showToast("Draft saved locally. You can continue it from Drafts.")
    }

    private fun castVote(trade: Trade, value: Int) {
        val uid = auth.currentUser?.uid
        val docId = tradeDocIdFor(trade)
        if (uid == null || docId == null) { showToast("Sign in to vote"); return }
        val ref = firestore.collection("votes").document("${docId}_$uid")
        val already = if (value > 0) trade.isUpvoted else trade.isDownvoted
        if (already) {
            ref.delete()
        } else {
            ref.set(mapOf("tradeDocId" to docId, "voterUid" to uid, "value" to value))
            val owner = docId.substringBefore('_')
            if (value > 0 && owner != uid) {
                notifyUser(owner, "likes", "Someone liked your post", "${_currentUser.value?.name ?: "A trader"} liked your ${trade.pair} setup.")
            }
        }
    }

    fun toggleUpvote(trade: Trade) {
        if (isShared(trade)) { castVote(trade, 1); return }
        viewModelScope.launch {
            repository.toggleUpvote(trade.id, trade.isUpvoted)
            persistTradeToCloud(trade.copy(upvotes = trade.upvotes + if (trade.isUpvoted) -1 else 1, isUpvoted = !trade.isUpvoted))
            if (!trade.isUpvoted && trade.authorHandle == _currentUser.value?.handle) addNotification("likes", "Someone liked your post", "Your ${trade.pair} setup received a like.")
            // also update selectedTrade if viewing detail
            if (_selectedTrade.value?.id == trade.id) {
                val delta = if (trade.isUpvoted) -1 else 1
                _selectedTrade.value = trade.copy(
                    upvotes = trade.upvotes + delta,
                    isUpvoted = !trade.isUpvoted
                )
            }
        }
    }

    fun toggleBookmark(trade: Trade) {
        if (trade.id < 0) {
            val uid = auth.currentUser?.uid
            val docId = tradeDocIdFor(trade)
            if (uid == null || docId == null) { showToast("Sign in to save posts"); return }
            val ref = firestore.collection("users").document(uid).collection("savedPosts").document(docId)
            if (trade.isBookmarked) {
                ref.delete()
                showToast("Bookmark removed")
            } else {
                val json = tradeAdapter.toJson(TradeEntity.fromDomain(trade))
                viewModelScope.launch(Dispatchers.IO) {
                    val data = mutableMapOf<String, Any>("payload" to json, "savedAt" to System.currentTimeMillis())
                    encodeChart(trade.chartImageUri)?.let { data["chartB64"] = it }
                    ref.set(data)
                }
                showToast("Saved to your Journal")
            }
            return
        }
        viewModelScope.launch {
            repository.toggleBookmark(trade.id, trade.isBookmarked)
            persistTradeToCloud(trade.copy(isBookmarked = !trade.isBookmarked))
            if (_selectedTrade.value?.id == trade.id) {
                _selectedTrade.value = trade.copy(isBookmarked = !trade.isBookmarked)
            }
            showToast(if (!trade.isBookmarked) "Trade setup bookmarked!" else "Bookmark removed")
        }
    }

    fun shareTradeToFeed(trade: Trade) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showToast("Sign in to share to your feed")
            return
        }
        firestore.collection("users").document(uid).collection("sharedPosts").document("share_${trade.id}_${System.currentTimeMillis()}")
            .set(mapOf("tradeId" to trade.id, "pair" to trade.pair, "strategy" to trade.setupStrategy, "sharedAt" to System.currentTimeMillis(), "authorHandle" to trade.authorHandle), SetOptions.merge())
            .addOnSuccessListener { showToast("Post shared to your feed") }
            .addOnFailureListener { showToast("Could not share post") }
    }

    fun toggleDownvote(trade: Trade) {
        if (isShared(trade)) { castVote(trade, -1); return }
        viewModelScope.launch {
            repository.toggleDownvote(trade.id, trade.isDownvoted)
            persistTradeToCloud(trade.copy(downvotes = trade.downvotes + if (trade.isDownvoted) -1 else 1, isDownvoted = !trade.isDownvoted))
            if (_selectedTrade.value?.id == trade.id) {
                val delta = if (trade.isDownvoted) -1 else 1
                _selectedTrade.value = trade.copy(
                    downvotes = trade.downvotes + delta,
                    isDownvoted = !trade.isDownvoted
                )
            }
        }
    }

    fun toggleFollowTrader(traderId: String) {
        _traders.value = _traders.value.map {
            if (it.id == traderId) {
                val newFollowing = !it.isFollowing
                followedTraderIds = if (newFollowing) followedTraderIds + it.id else followedTraderIds - it.id
                securityPrefs.edit().putBoolean("following_${it.id}", newFollowing).apply()
                userRoot()?.collection("following")?.document(it.id)?.set(mapOf("traderId" to it.id, "handle" to it.handle, "isFollowing" to newFollowing), SetOptions.merge())
                auth.currentUser?.uid?.let { me ->
                    val followRef = firestore.collection("follows").document("${me}_${it.id}")
                    if (newFollowing) followRef.set(mapOf(
                        "followerUid" to me, "traderUid" to it.id,
                        "followerName" to (_currentUser.value?.name ?: "Trader"),
                        "followerHandle" to (_currentUser.value?.handle ?: "@trader"),
                        "traderName" to it.name, "traderHandle" to it.handle,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    else followRef.delete()
                }
                if (newFollowing) notifyUser(it.id, "newFollowers", "New follower", "${_currentUser.value?.handle ?: "Someone"} started following you.")
                showToast(if (newFollowing) "Following ${it.handle}" else "Unfollowed ${it.handle}")
                it.copy(isFollowing = newFollowing)
            } else it
        }
    }

    private fun initialsOf(name: String) =
        name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "T" }

    private fun postComment(tradeId: Long, content: String, parentId: String, imageUri: String? = null) {
        if (content.isBlank() && imageUri == null) return
        val trade = findTrade(tradeId) ?: return
        val user = _currentUser.value
        val uid = auth.currentUser?.uid
        val docId = tradeDocIdFor(trade)
        if (uid == null || user == null || docId == null) { showToast("Sign in to comment"); return }
        val shared = isShared(trade)
        // Public posts: visible to everyone. Private trades: stored only in your own journal.
        val collection = if (shared) firestore.collection("publicTrades").document(docId).collection("discussion")
            else firestore.collection("users").document(uid).collection("privateDiscussion")
        val id = "c_${System.currentTimeMillis()}_${uid.take(4)}"
        viewModelScope.launch {
            // Shrink the attached photo first so it fits in the comment
            val imageB64 = if (imageUri != null) withContext(Dispatchers.IO) { encodeChart(imageUri) } else null
            if (imageUri != null && imageB64 == null) { showToast("Could not read that image"); return@launch }
            val data = mutableMapOf<String, Any>(
                "tradeDocId" to docId, "authorUid" to uid,
                "authorName" to user.name, "authorHandle" to user.handle,
                "authorInitials" to initialsOf(user.name),
                "content" to content.trim(), "timestamp" to System.currentTimeMillis(),
                "parentId" to parentId, "likedBy" to emptyList<String>()
            )
            if (imageB64 != null) data["imageB64"] = imageB64
            collection.document(id)
                .set(data)
                .addOnSuccessListener { showToast(if (parentId.isBlank()) "Comment posted!" else "Reply posted") }
                .addOnFailureListener { showToast("Could not post comment: ${it.message}") }
        }
        val owner = docId.substringBefore('_')
        if (shared && owner != uid) {
            notifyUser(owner, "comments", "New comment on your post", "${user.name} commented on your ${trade.pair} setup.")
        }
    }

    fun addComment(tradeId: Long, content: String, imageUri: String? = null) = postComment(tradeId, content, "", imageUri)

    fun addCommentReply(tradeId: Long, commentId: String, content: String, imageUri: String? = null) = postComment(tradeId, content, commentId, imageUri)

    fun toggleCommentLike(tradeId: Long, commentId: String) {
        val uid = auth.currentUser?.uid ?: run { showToast("Sign in to like comments"); return }
        val shared = _commentDocs.value.firstOrNull { it.id == commentId }
        val doc = shared ?: _privateCommentDocs.value.firstOrNull { it.id == commentId } ?: return
        val ref = if (shared != null) {
            firestore.collection("publicTrades").document(doc.tradeDocId).collection("discussion").document(commentId)
        } else {
            firestore.collection("users").document(uid).collection("privateDiscussion").document(commentId)
        }
        ref.update("likedBy", if (uid in doc.likedBy) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid))
    }

    fun savedFolder(tradeId: Long): String = securityPrefs.getString("saved_folder_$tradeId", "General") ?: "General"
    fun savedNote(tradeId: Long): String = securityPrefs.getString("saved_note_$tradeId", "") ?: ""

    fun saveSavedMetadata(tradeId: Long, folder: String, note: String) {
        securityPrefs.edit()
            .putString("saved_folder_$tradeId", folder.ifBlank { "General" })
            .putString("saved_note_$tradeId", note)
            .apply()
        userRoot()?.collection("saved")?.document(tradeId.toString())?.set(mapOf("tradeId" to tradeId, "folder" to folder.ifBlank { "General" }, "note" to note), SetOptions.merge())
        showToast("Saved post updated")
    }

    fun syncUserData() {
        val user = auth.currentUser ?: run { showToast("Sign in to sync your data"); return }
        val bookmarkedIds = trades.value.filter { it.isBookmarked }.map { it.id }
        val root = firestore.collection("users").document(user.uid)
        val payload = hashMapOf<String, Any>(
            "bookmarkedTradeIds" to bookmarkedIds,
            "recentTradeIds" to _recentTradeIds.value,
            "draftCount" to draftCount,
            "themeMode" to _themeMode.value.name,
            "feedTab" to _feedTab.value,
            "journalTab" to _journalTab.value,
            "syncedAt" to System.currentTimeMillis()
        )
        trades.value.forEach { persistTradeToCloud(it) }
        _traders.value.forEach { trader -> root.collection("following").document(trader.id).set(mapOf("traderId" to trader.id, "handle" to trader.handle, "isFollowing" to trader.isFollowing), SetOptions.merge()) }
        root.set(payload, SetOptions.merge())
            .addOnSuccessListener { showToast("Journal, bookmarks and profile synced") }
            .addOnFailureListener { showToast("Sync failed: ${it.message}") }
    }

    fun updateTradeStatus(trade: Trade, newStatus: TradeStatus) {
        viewModelScope.launch {
            val rMultiple = if (newStatus == TradeStatus.OPEN || newStatus == TradeStatus.BREAKEVEN || newStatus == TradeStatus.CANCELLED) {
                0.0
            } else if (trade.direction == TradeDirection.LONG) {
                val risk = trade.entryPrice - trade.stopLoss
                val reward = trade.takeProfit - trade.entryPrice
                if (risk > 0) {
                    if (newStatus == TradeStatus.STOPPED) -1.0
                    else Math.round((reward / risk) * 100.0) / 100.0
                } else 2.5
            } else {
                val risk = trade.stopLoss - trade.entryPrice
                val reward = trade.entryPrice - trade.takeProfit
                if (risk > 0) {
                    if (newStatus == TradeStatus.STOPPED) -1.0
                    else Math.round((reward / risk) * 100.0) / 100.0
                } else 2.5
            }

            val updatedTrade = trade.copy(
                status = newStatus,
                rMultiple = rMultiple,
                durationText = if (newStatus == TradeStatus.OPEN) "Running" else "Closed"
            )
            repository.updateTrade(updatedTrade)
            persistTradeToCloud(updatedTrade)
            _selectedTrade.value = updatedTrade
            showToast("Trade status updated to ${newStatus.name}")
        }
    }

    fun saveNewTrade(
        pair: String,
        direction: TradeDirection,
        entryPrice: Double = 0.0,
        stopLoss: Double = 0.0,
        takeProfit: Double = 0.0,
        positionSizeLots: Double,
        setupStrategy: String,
        session: String = "London / NY Overlap",
        timeframe: String = "15M",
        executionThesis: String,
        psychologyNotes: String,
        visibility: TradeVisibility,
        status: TradeStatus,
        selectedTags: List<String>,
        chartImageUri: String?,
        winRatePercent: Double? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // R:R is only calculated from prices the trader actually entered
            val plannedRR: Double? = if (entryPrice > 0.0 && stopLoss > 0.0 && takeProfit > 0.0) {
                val risk = if (direction == TradeDirection.LONG) entryPrice - stopLoss else stopLoss - entryPrice
                val reward = if (direction == TradeDirection.LONG) takeProfit - entryPrice else entryPrice - takeProfit
                if (risk > 0.0 && reward > 0.0) Math.round((reward / risk) * 100.0) / 100.0 else null
            } else null
            val rMultiple = when {
                status == TradeStatus.OPEN || plannedRR == null -> 0.0
                status == TradeStatus.STOPPED -> -1.0
                else -> plannedRR
            }
            val maxRisk = Math.round(positionSizeLots * 480.0 * 100.0) / 100.0
            val netGain = Math.round(maxRisk * rMultiple * 100.0) / 100.0

            val storedChart = withContext(Dispatchers.IO) { copyChartToPrivateStorage(chartImageUri) }
            val trade = Trade(
                pair = pair,
                direction = direction,
                setupStrategy = setupStrategy,
                session = session,
                timeframe = timeframe,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                positionSizeLots = 0.0,
                riskRewardRatio = plannedRR?.let { "1 : $it" } ?: "",
                rMultiple = rMultiple,
                netGainDollars = 0.0,
                riskPercent = 0.0,
                maxRiskDollars = 0.0,
                visibility = visibility,
                publicPostAudience = if (visibility == TradeVisibility.PUBLIC) (_currentUser.value?.publicPostAudience ?: "everyone") else "everyone",
                winRatePercent = winRatePercent,
                status = status,
                timestamp = System.currentTimeMillis(),
                timeAgo = "Just now",
                authorName = _currentUser.value?.name ?: "Trader",
                authorHandle = _currentUser.value?.handle ?: "@trader",
                executionThesis = executionThesis.trim(),
                psychologyNote = psychologyNotes.trim().ifBlank { null },
                tags = selectedTags,
                chartImageUri = storedChart
            )

            val localId = repository.insertTrade(trade)
            persistTradeToCloud(trade.copy(id = localId))
            showToast("Trade successfully logged to ${if (visibility == TradeVisibility.PUBLIC) "Public Feed & Journal" else "Private Journal"}!")
            onSuccess()
        }
    }
}
