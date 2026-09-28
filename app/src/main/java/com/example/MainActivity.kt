package com.example

import android.os.Bundle
import com.google.firebase.FirebaseApp
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppNavScreen
import com.example.ui.FxViewModel
import com.example.ui.components.CreateLogBottomSheet
import com.example.ui.components.FxBottomBar
import com.example.ui.components.FxHeader
import com.example.ui.components.ThemeSelectorModal
import com.example.ui.screens.BreakdownScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LogTradeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationCenter
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.TradeDetailScreen
import com.example.ui.screens.TradersScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.FXJournalTheme
import kotlinx.coroutines.delay

private val MaxContentWidth = 640.dp

class MainActivity : ComponentActivity() {
    private lateinit var appViewModel: FxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Firebase initialization failed", e)
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: FxViewModel = viewModel()
            appViewModel = viewModel
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            FXJournalTheme(themeMode = themeMode) {
                FXJournalApp(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::appViewModel.isInitialized) appViewModel.lockAppIfConfigured()
    }
}

@Composable
fun FXJournalApp(viewModel: FxViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedTrade by viewModel.selectedTrade.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val showCreateLogSheet by viewModel.showCreateLogSheet.collectAsStateWithLifecycle()
    val showThemeModal by viewModel.showThemeModal.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val appLocked by viewModel.appLocked.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotifications.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    // Authentication Redirect Logic
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            if (currentScreen != AppNavScreen.LOGIN && 
                currentScreen != AppNavScreen.SIGNUP && 
                currentScreen != AppNavScreen.FORGOT_PASSWORD) {
                viewModel.navigateTo(AppNavScreen.LOGIN)
            }
        } else {
            if (currentScreen == AppNavScreen.LOGIN || 
                currentScreen == AppNavScreen.SIGNUP || 
                currentScreen == AppNavScreen.FORGOT_PASSWORD) {
                viewModel.navigateTo(AppNavScreen.FEED)
            }
        }
    }

    // Handle Android system back gesture / button
    BackHandler(enabled = currentScreen != AppNavScreen.FEED && currentScreen != AppNavScreen.LOGIN) {
        when (currentScreen) {
            AppNavScreen.TRADE_DETAIL -> viewModel.closeTradeDetail()
            AppNavScreen.LOG_TRADE -> viewModel.navigateTo(AppNavScreen.FEED)
            AppNavScreen.JOURNAL -> viewModel.navigateTo(AppNavScreen.FEED)
            AppNavScreen.TRADERS -> viewModel.navigateTo(AppNavScreen.FEED)
            AppNavScreen.BREAKDOWN -> viewModel.navigateTo(AppNavScreen.FEED)
            AppNavScreen.SETTINGS -> viewModel.navigateTo(AppNavScreen.FEED)
            AppNavScreen.SIGNUP -> viewModel.navigateTo(AppNavScreen.LOGIN)
            AppNavScreen.FORGOT_PASSWORD -> viewModel.navigateTo(AppNavScreen.LOGIN)
            else -> {}
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3500)
            viewModel.dismissToast()
        }
    }

    val headerTitle = when (currentScreen) {
        AppNavScreen.FEED -> "Community Feed"
        AppNavScreen.JOURNAL -> "Personal Journal"
        AppNavScreen.TRADERS -> "Verified Traders"
        AppNavScreen.BREAKDOWN -> "Performance Audit"
        AppNavScreen.SETTINGS -> "User Settings"
        AppNavScreen.TRADE_DETAIL -> "Trade Detail"
        AppNavScreen.LOG_TRADE -> "Log Trade"
        AppNavScreen.LOGIN -> "FX Journal"
        AppNavScreen.SIGNUP -> "FX Journal"
        AppNavScreen.FORGOT_PASSWORD -> "FX Journal"
    }

    val headerSubtitle = when (currentScreen) {
        AppNavScreen.FEED -> "Public Setups"
        AppNavScreen.JOURNAL -> "My Trades"
        AppNavScreen.TRADERS -> "Leaderboard"
        AppNavScreen.BREAKDOWN -> "Quantitative Analysis"
        AppNavScreen.SETTINGS -> "Configuration"
        AppNavScreen.TRADE_DETAIL -> "Execution"
        AppNavScreen.LOG_TRADE -> "v2.4 Live"
        AppNavScreen.LOGIN -> "Authentication"
        AppNavScreen.SIGNUP -> "Onboarding"
        AppNavScreen.FORGOT_PASSWORD -> "Reset Access"
    }

    val showHeaderBack = currentScreen == AppNavScreen.TRADE_DETAIL || 
                       currentScreen == AppNavScreen.LOG_TRADE ||
                       currentScreen == AppNavScreen.SETTINGS ||
                       currentScreen == AppNavScreen.FORGOT_PASSWORD

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
    Scaffold(
        modifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxWidth()
            .fillMaxHeight(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentScreen != AppNavScreen.LOGIN && currentScreen != AppNavScreen.SIGNUP) {
                FxHeader(
                    title = headerTitle,
                    subtitle = headerSubtitle,
                    showBack = showHeaderBack,
                    onBackClick = {
                        if (currentScreen == AppNavScreen.TRADE_DETAIL) {
                            viewModel.closeTradeDetail()
                        } else if (currentScreen == AppNavScreen.FORGOT_PASSWORD) {
                            viewModel.navigateTo(AppNavScreen.LOGIN)
                        } else {
                            viewModel.navigateTo(AppNavScreen.FEED)
                        }
                    },
                    themeMode = themeMode,
                    onThemeClick = { viewModel.toggleThemeModal(true) },
                    onNotificationsClick = { viewModel.toggleNotifications(true) },
                    onLogClick = { viewModel.toggleCreateLogSheet(true) },
                    onProfileClick = {
                        viewModel.navigateTo(AppNavScreen.SETTINGS)
                    },
                    profileName = currentUser?.name ?: "Trader"
                )
            }
        },
        bottomBar = {
            // Show bottom navigation bar on all main tab screens
            if (currentScreen != AppNavScreen.LOG_TRADE && 
                currentScreen != AppNavScreen.LOGIN && 
                currentScreen != AppNavScreen.SIGNUP &&
                currentScreen != AppNavScreen.FORGOT_PASSWORD) {
                FxBottomBar(
                    currentScreen = currentScreen,
                    onTabSelected = { screen -> viewModel.navigateTo(screen) },
                    onPlusClick = { viewModel.toggleCreateLogSheet(true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppNavScreen.FEED -> {
                    FeedScreen(
                        viewModel = viewModel,
                        onTradeClick = { trade -> viewModel.openTradeDetail(trade) }
                    )
                }
                AppNavScreen.JOURNAL -> {
                    JournalScreen(
                        viewModel = viewModel,
                        onTradeClick = { trade -> viewModel.openTradeDetail(trade) }
                    )
                }
                AppNavScreen.TRADERS -> {
                    TradersScreen(viewModel = viewModel)
                }
                AppNavScreen.BREAKDOWN -> {
                    BreakdownScreen(viewModel = viewModel)
                }
                AppNavScreen.TRADE_DETAIL -> {
                    selectedTrade?.let { trade ->
                        TradeDetailScreen(
                            trade = trade,
                            viewModel = viewModel,
                            onBack = { viewModel.closeTradeDetail() }
                        )
                    } ?: run {
                        FeedScreen(
                            viewModel = viewModel,
                            onTradeClick = { trade -> viewModel.openTradeDetail(trade) }
                        )
                    }
                }
                AppNavScreen.LOG_TRADE -> {
                    LogTradeScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppNavScreen.FEED) }
                    )
                }
                AppNavScreen.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToSignup = { viewModel.navigateTo(AppNavScreen.SIGNUP) },
                        onNavigateToForgotPassword = { viewModel.navigateTo(AppNavScreen.FORGOT_PASSWORD) },
                        onLoginSuccess = { viewModel.navigateTo(AppNavScreen.FEED) }
                    )
                }
                AppNavScreen.SIGNUP -> {
                    SignupScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { viewModel.navigateTo(AppNavScreen.LOGIN) },
                        onSignupSuccess = { viewModel.navigateTo(AppNavScreen.FEED) }
                    )
                }
                AppNavScreen.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppNavScreen.LOGIN) }
                    )
                }
                AppNavScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogout = { viewModel.navigateTo(AppNavScreen.LOGIN) }
                    )
                }
            }

            if (showNotifications) {
                NotificationCenter(notifications, viewModel) { viewModel.toggleNotifications(false) }
            }

            // Interactive Toast / Notification Snackbar
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                toastMessage?.let { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("app_toast_notification"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldProfit,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.dismissToast() }
                        ) {
                            Text(
                                text = "DISMISS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            )
                        }
                    }
                }
            }
        }
    }
        }

    // Theme Selector Modal Sheet
    if (showThemeModal) {
        ThemeSelectorModal(
            currentTheme = themeMode,
            onSelectTheme = { mode -> viewModel.setThemeMode(mode) },
            onDismiss = { viewModel.toggleThemeModal(false) }
        )
    }

    // Create & Log Bottom Action Sheet
    if (showCreateLogSheet) {
        CreateLogBottomSheet(
            onDismiss = { viewModel.toggleCreateLogSheet(false) },
            onLogNewTradeClick = {
                viewModel.toggleCreateLogSheet(false)
                viewModel.navigateTo(AppNavScreen.LOG_TRADE)
            },
            onShareChartClick = {
                viewModel.toggleCreateLogSheet(false)
                viewModel.showToast("Select chart screenshot to analyze")
            },
            onPsychologyNoteClick = {
                viewModel.toggleCreateLogSheet(false)
                viewModel.showToast("Psychology check-in: Logged in Flow State (94%)")
            },
            onImportBrokerClick = {
                viewModel.toggleCreateLogSheet(false)
                viewModel.showToast("MT5 / cTrader Auto-Sync: 3 new executions detected")
            }
        )
    }
    if (appLocked) {
        AppLockScreen(onUnlock = { pin -> viewModel.unlockApp(pin) })
    }
    }
}
