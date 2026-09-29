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
import com.example.model.TraderTier
import com.example.model.FxUser
import com.example.model.AppNotification
import com.example.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavScreen {
    FEED, JOURNAL, TRADERS, BREAKDOWN, TRADE_DETAIL, LOG_TRADE, LOGIN, SIGNUP, FORGOT_PASSWORD, SETTINGS
}

class FxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradeRepository
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { 
        FirebaseFirestore.getInstance("ai-studio-fxjournal-86f1108b-a538-4c26-b119-4b6d64fa097f")
    }
    private val securityPrefs = application.getSharedPreferences("fx_journal_security", Context.MODE_PRIVATE)

    private fun userRoot() = auth.currentUser?.let { firestore.collection("users").document(it.uid) }
    private fun persistTradeToCloud(trade: Trade) {
        userRoot()?.collection("trades")?.document(trade.id.toString())
            ?.set(TradeEntity.fromDomain(trade), SetOptions.merge())
    }
    private fun persistCommentToCloud(tradeId: Long, comment: TradeComment) {
        userRoot()?.collection("trades")?.document(tradeId.toString())?.collection("comments")?.document(comment.id)
            ?.set(comment, SetOptions.merge())
    }

    val trades: StateFlow<List<Trade>>

    private val _currentUser = MutableStateFlow<FxUser?>(null)
    val currentUser: StateFlow<FxUser?> = _currentUser.asStateFlow()

    init {
        // Defensive Firebase check
        try {
            // Observe Auth State
            auth.addAuthStateListener { firebaseAuth ->
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    loadCloudTrades(firebaseUser.uid)
                    loadFollowing(firebaseUser.uid)
                    loadPublicTraders(firebaseUser.uid)
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
                                _currentUser.value = FxUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = name,
                                    handle = handle,
                                    photoUri = document.getString("photoUri") ?: securityPrefs.getString("profile_photo", null),
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
        auth.currentUser?.uid?.let { loadCloudTrades(it) }
        auth.currentUser?.uid?.let { loadNotifications(it) }
        auth.currentUser?.uid?.let { loadNotificationPreferences(it) }
    }

    private fun loadCloudTrades(uid: String) {
        firestore.collection("users").document(uid).collection("trades").get()
            .addOnSuccessListener { snapshot ->
                viewModelScope.launch {
                    snapshot.documents.mapNotNull { it.toObject(TradeEntity::class.java) }
                        .forEach { repository.insertTrade(it.toDomain()) }
                }
            }
    }

    private fun loadPublicTraders(currentUid: String) {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                _traders.value = snapshot.documents.mapNotNull { document ->
                    if (document.id == currentUid) return@mapNotNull null
                    val name = document.getString("name")?.trim().orEmpty()
                    val handle = document.getString("handle")?.trim().orEmpty()
                    if (name.isBlank() || handle.isBlank()) return@mapNotNull null
                    val tier = document.getString("tier")?.let { value ->
                        runCatching { TraderTier.valueOf(value.uppercase()) }.getOrDefault(TraderTier.FREE)
                    } ?: TraderTier.FREE
                    TraderProfile(
                        id = document.id,
                        name = name,
                        handle = handle,
                        tier = tier,
                        tierLabel = document.getString("tierLabel") ?: tier.label,
                        winRate = document.getString("winRate") ?: "—",
                        streak = document.getString("streak") ?: "—",
                        netRGain = document.getString("netRGain") ?: "—",
                        followers = document.getLong("followers")?.toString() ?: "0",
                        isVerified = document.getBoolean("isVerified") ?: false,
                        isFollowing = securityPrefs.getBoolean("following_${document.id}", false)
                    )
                }.sortedBy { it.name.lowercase() }
            }
    }

    private fun loadFollowing(uid: String) {
        firestore.collection("users").document(uid).collection("following").get()
            .addOnSuccessListener { snapshot ->
                val followedIds = snapshot.documents.filter { it.getBoolean("isFollowing") != false }
                    .mapNotNull { it.getString("traderId") }.toSet()
                _traders.value = _traders.value.map { trader ->
                    trader.copy(isFollowing = if (snapshot.isEmpty) {
                        securityPrefs.getBoolean("following_${trader.id}", false)
                    } else trader.id in followedIds)
                }
                _traders.value.forEach { trader ->
                    securityPrefs.edit().putBoolean("following_${trader.id}", trader.isFollowing).apply()
                }
            }
    }

    private fun loadNotifications(uid: String) {
        firestore.collection("users").document(uid).collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get()
            .addOnSuccessListener { snapshot -> _notifications.value = snapshot.documents.mapNotNull { it.toObject(AppNotification::class.java) } }
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
    val selectedTrade: StateFlow<Trade?> = _selectedTrade.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(ThemeMode.AMOLED)
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
    private val _tradeComments = MutableStateFlow<List<TradeComment>>(emptyList())
    val tradeComments: StateFlow<List<TradeComment>> = _tradeComments.asStateFlow()
    private val commentsByTrade = mutableMapOf<Long, List<TradeComment>>()
    private val _recentTradeIds = MutableStateFlow<List<Long>>(emptyList())
    val recentTradeIds: StateFlow<List<Long>> = _recentTradeIds.asStateFlow()
    // Community profiles are loaded from Firestore and start empty.
    private val _traders = MutableStateFlow<List<TraderProfile>>(emptyList())
    val traders: StateFlow<List<TraderProfile>> = _traders.asStateFlow()

    private val _appLocked = MutableStateFlow(securityPrefs.getString("pin_hash", null) != null)
    val appLocked: StateFlow<Boolean> = _appLocked.asStateFlow()
    val hasAppPin: Boolean get() = securityPrefs.getString("pin_hash", null) != null

    fun setAppPin(pin: String, onResult: (Boolean) -> Unit = {}) {
        if (!pin.matches(Regex("\\d{4}"))) { onResult(false); return }
        securityPrefs.edit().putString("pin_hash", hashPin(pin)).apply()
        onResult(true)
        showToast("4-digit app lock enabled")
    }

    fun removeAppPin() {
        securityPrefs.edit().remove("pin_hash").apply()
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
        if (photoUri != null) updates["photoUri"] = photoUri.toString()
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

    fun openTradeDetail(trade: Trade) {
        _selectedTrade.value = trade
        _recentTradeIds.value = (listOf(trade.id) + _recentTradeIds.value.filterNot { it == trade.id }).take(10)
        userRoot()?.collection("recentViews")?.document(trade.id.toString())?.set(mapOf("tradeId" to trade.id, "viewedAt" to System.currentTimeMillis()), SetOptions.merge())
        _tradeComments.value = commentsByTrade[trade.id] ?: emptyList()
        _currentScreen.value = AppNavScreen.TRADE_DETAIL
    }

    fun closeTradeDetail() {
        _currentScreen.value = AppNavScreen.FEED
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
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

    fun toggleUpvote(trade: Trade) {
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
                securityPrefs.edit().putBoolean("following_${it.id}", newFollowing).apply()
                userRoot()?.collection("following")?.document(it.id)?.set(mapOf("traderId" to it.id, "handle" to it.handle, "isFollowing" to newFollowing), SetOptions.merge())
                showToast(if (newFollowing) "Following ${it.handle}" else "Unfollowed ${it.handle}")
                it.copy(isFollowing = newFollowing)
            } else it
        }
    }

    fun addComment(tradeId: Long, content: String) {
        if (content.isBlank()) return
        val newComment = TradeComment(
            id = "c_${System.currentTimeMillis()}",
            authorName = _currentUser.value?.name ?: "Trader",
            authorHandle = _currentUser.value?.handle ?: "@trader",
            authorInitials = (_currentUser.value?.name ?: "Trader").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
            isAuthorBadge = true,
            timeAgo = "Just now",
            content = content.trim(),
            likesCount = 0,
            isLiked = false
        )
        val updated = listOf(newComment) + _tradeComments.value
        _tradeComments.value = updated
        commentsByTrade[tradeId] = updated
        persistCommentToCloud(tradeId, newComment)
        trades.value.firstOrNull { it.id == tradeId }?.takeIf { it.authorHandle == _currentUser.value?.handle }?.let { addNotification("comments", "New comment on your post", "Someone commented on your ${it.pair} setup.") }
        showToast("Comment posted!")
    }

    fun toggleCommentLike(tradeId: Long, commentId: String) {
        _tradeComments.value = _tradeComments.value.map { c ->
            if (c.id == commentId) {
                val liked = !c.isLiked
                c.copy(isLiked = liked, likesCount = if (liked) c.likesCount + 1 else c.likesCount - 1)
            } else c
        }
        commentsByTrade[tradeId] = _tradeComments.value
        _tradeComments.value.firstOrNull { it.id == commentId }?.let { persistCommentToCloud(tradeId, it) }
    }

    fun addCommentReply(tradeId: Long, commentId: String, content: String) {
        if (content.isBlank()) return
        val reply = TradeComment(
            id = "reply_${System.currentTimeMillis()}",
            authorName = _currentUser.value?.name ?: "Trader",
            authorHandle = _currentUser.value?.handle ?: "@trader",
            authorInitials = (_currentUser.value?.name ?: "Trader").split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
            isAuthorBadge = true,
            timeAgo = "Just now",
            content = content.trim()
        )
        val updated = _tradeComments.value.map { if (it.id == commentId) it.copy(replies = it.replies + reply) else it }
        _tradeComments.value = updated
        commentsByTrade[tradeId] = updated
        persistCommentToCloud(tradeId, reply)
        showToast("Reply posted")
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
        commentsByTrade.forEach { (tradeId, comments) -> comments.forEach { persistCommentToCloud(tradeId, it) } }
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
            val rMultiple = if (status == TradeStatus.OPEN) {
                0.0
            } else if (direction == TradeDirection.LONG) {
                val risk = entryPrice - stopLoss
                val reward = takeProfit - entryPrice
                if (risk > 0) {
                    if (status == TradeStatus.STOPPED) -1.0
                    else Math.round((reward / risk) * 100.0) / 100.0
                } else 2.5
            } else {
                val risk = stopLoss - entryPrice
                val reward = entryPrice - takeProfit
                if (risk > 0) {
                    if (status == TradeStatus.STOPPED) -1.0
                    else Math.round((reward / risk) * 100.0) / 100.0
                } else 2.5
            }

            val maxRisk = Math.round(positionSizeLots * 480.0 * 100.0) / 100.0
            val netGain = Math.round(maxRisk * rMultiple * 100.0) / 100.0

            val trade = Trade(
                pair = pair,
                direction = direction,
                setupStrategy = setupStrategy,
                session = session,
                timeframe = timeframe,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                positionSizeLots = positionSizeLots,
                riskRewardRatio = "1 : $rMultiple",
                rMultiple = rMultiple,
                netGainDollars = netGain,
                riskPercent = 1.0,
                maxRiskDollars = maxRisk,
                visibility = visibility,
                publicPostAudience = if (visibility == TradeVisibility.PUBLIC) (_currentUser.value?.publicPostAudience ?: "everyone") else "everyone",
                winRatePercent = winRatePercent,
                status = status,
                timestamp = System.currentTimeMillis(),
                timeAgo = "Just now",
                authorName = _currentUser.value?.name ?: "Trader",
                authorHandle = _currentUser.value?.handle ?: "@trader",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = executionThesis.ifBlank { "Clean liquidity sweep into order block." },
                psychologyNote = psychologyNotes.ifBlank { "Disciplined execution. Plan respected 100%." },
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = selectedTags,
                chartImageUri = chartImageUri,
                brokerName = "IC Markets (cTrader Raw)",
                slippagePips = 0.1,
                durationText = if (status == TradeStatus.OPEN) "Running" else "Closed"
            )

            val localId = repository.insertTrade(trade)
            persistTradeToCloud(trade.copy(id = localId))
            showToast("Trade successfully logged to ${if (visibility == TradeVisibility.PUBLIC) "Public Feed & Journal" else "Private Journal"}!")
            onSuccess()
        }
    }
}
