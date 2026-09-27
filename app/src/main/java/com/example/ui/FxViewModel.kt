package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TradeRepository
import com.example.model.Trade
import com.example.model.TradeComment
import com.example.model.TradeDirection
import com.example.model.TradeStatus
import com.example.model.TradeVisibility
import com.example.model.TraderProfile
import com.example.model.FxUser
import com.example.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                    // Fetch extra user data from Firestore
                    firestore.collection("users").document(firebaseUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val name = document.getString("name") ?: firebaseUser.displayName ?: "Trader"
                                val handle = document.getString("handle") ?: "@anonymous"
                                _currentUser.value = FxUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = name,
                                    handle = handle
                                )
                            } else {
                                _currentUser.value = FxUser(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    name = firebaseUser.displayName ?: "Trader"
                                )
                            }
                        }
                        .addOnFailureListener {
                            // If Firestore fails, at least set the auth user
                            _currentUser.value = FxUser(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                name = firebaseUser.displayName ?: "Trader"
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
        repository.checkAndSeedInitialData(viewModelScope)

        trades = repository.allTrades.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
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

    // Discussion comments for trade detail
    private val _tradeComments = MutableStateFlow<List<TradeComment>>(
        listOf(
            TradeComment(
                id = "c1",
                authorName = "Marcus K.",
                authorHandle = "@marcus_fx",
                authorInitials = "MK",
                isAuthorBadge = false,
                timeAgo = "18m ago",
                content = "Textbook execution on the London open low sweep. Did you take partials before CPI release or hold straight to target?",
                likesCount = 29,
                isLiked = false,
                authorReply = TradeComment(
                    id = "c1_reply",
                    authorName = "Alex Vance",
                    authorHandle = "@QuantAlex",
                    authorInitials = "AV",
                    isAuthorBadge = true,
                    timeAgo = "12m ago",
                    content = "Took 50% off at +2R right at 1.1012 liquidity cluster, let the remaining runner take full TP!",
                    likesCount = 14,
                    isLiked = true
                )
            ),
            TradeComment(
                id = "c2",
                authorName = "Sophia Lin",
                authorHandle = "@sophia_trades",
                authorInitials = "SL",
                isAuthorBadge = false,
                timeAgo = "45m ago",
                content = "Patience pays off. Loved the clean risk management here—never chasing extended wicks.",
                likesCount = 14,
                isLiked = false
            )
        )
    )
    val tradeComments: StateFlow<List<TradeComment>> = _tradeComments.asStateFlow()

    // Traders Community List
    private val _traders = MutableStateFlow<List<TraderProfile>>(
        listOf(
            TraderProfile(
                id = "t1",
                name = "Alex Vance",
                handle = "@QuantAlex",
                tier = com.example.model.TraderTier.PROP,
                tierLabel = "Prop Firm Verified",
                winRate = "68.4%",
                streak = "8W 🔥",
                netRGain = "+63.2R",
                followers = "14.2k",
                isVerified = true,
                isFollowing = true
            ),
            TraderProfile(
                id = "t2",
                name = "Satoshi Scalper",
                handle = "@SatoshiScalper",
                tier = com.example.model.TraderTier.LIVE,
                tierLabel = "Live Account Verified",
                winRate = "72.1%",
                streak = "5W 🔥",
                netRGain = "+84.5R",
                followers = "28.5k",
                isVerified = true,
                isFollowing = true
            ),
            TraderProfile(
                id = "t3",
                name = "Elena Gold",
                handle = "@ElenaGold",
                tier = com.example.model.TraderTier.PROP,
                tierLabel = "Prop Firm Elite",
                winRate = "64.0%",
                streak = "4W",
                netRGain = "+42.1R",
                followers = "9.8k",
                isVerified = true,
                isFollowing = true
            ),
            TraderProfile(
                id = "t4",
                name = "Marcus K.",
                handle = "@marcus_fx",
                tier = com.example.model.TraderTier.VERIFIED,
                tierLabel = "Professional Verified",
                winRate = "61.5%",
                streak = "3W",
                netRGain = "+28.4R",
                followers = "6.1k",
                isVerified = true,
                isFollowing = false
            ),
            TraderProfile(
                id = "t5",
                name = "Sophia Lin",
                handle = "@sophia_trades",
                tier = com.example.model.TraderTier.VERIFIED,
                tierLabel = "Retail Verified",
                winRate = "66.7%",
                streak = "6W 🔥",
                netRGain = "+35.6R",
                followers = "11.4k",
                isVerified = true,
                isFollowing = false
            )
        )
    )
    val traders: StateFlow<List<TraderProfile>> = _traders.asStateFlow()

    fun navigateTo(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    // Firebase Auth Methods
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
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

    fun toggleUpvote(trade: Trade) {
        viewModelScope.launch {
            repository.toggleUpvote(trade.id, trade.isUpvoted)
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
            if (_selectedTrade.value?.id == trade.id) {
                _selectedTrade.value = trade.copy(isBookmarked = !trade.isBookmarked)
            }
            showToast(if (!trade.isBookmarked) "Trade setup bookmarked!" else "Bookmark removed")
        }
    }

    fun toggleFollowTrader(traderId: String) {
        _traders.value = _traders.value.map {
            if (it.id == traderId) {
                val newFollowing = !it.isFollowing
                showToast(if (newFollowing) "Following ${it.handle}" else "Unfollowed ${it.handle}")
                it.copy(isFollowing = newFollowing)
            } else it
        }
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        val newComment = TradeComment(
            id = "c_${System.currentTimeMillis()}",
            authorName = "Alex Vance",
            authorHandle = "@QuantAlex",
            authorInitials = "AV",
            isAuthorBadge = true,
            timeAgo = "Just now",
            content = content.trim(),
            likesCount = 0,
            isLiked = false
        )
        _tradeComments.value = listOf(newComment) + _tradeComments.value
        showToast("Comment posted!")
    }

    fun toggleCommentLike(commentId: String) {
        _tradeComments.value = _tradeComments.value.map { c ->
            if (c.id == commentId) {
                val liked = !c.isLiked
                c.copy(isLiked = liked, likesCount = if (liked) c.likesCount + 1 else c.likesCount - 1)
            } else c
        }
    }

    fun updateTradeStatus(trade: Trade, newStatus: TradeStatus) {
        viewModelScope.launch {
            val rMultiple = if (newStatus == TradeStatus.OPEN) {
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
            _selectedTrade.value = updatedTrade
            showToast("Trade status updated to ${newStatus.name}")
        }
    }

    fun saveNewTrade(
        pair: String,
        direction: TradeDirection,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        positionSizeLots: Double,
        setupStrategy: String,
        executionThesis: String,
        psychologyNotes: String,
        visibility: TradeVisibility,
        status: TradeStatus,
        selectedTags: List<String>,
        chartImageUri: String?,
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
                session = "London / NY Overlap",
                timeframe = "15M",
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
                status = status,
                timestamp = System.currentTimeMillis(),
                timeAgo = "Just now",
                authorName = "Alex Vance",
                authorHandle = "@QuantAlex",
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

            repository.insertTrade(trade)
            showToast("Trade successfully logged to ${if (visibility == TradeVisibility.PUBLIC) "Public Feed & Journal" else "Private Journal"}!")
            onSuccess()
        }
    }
}
