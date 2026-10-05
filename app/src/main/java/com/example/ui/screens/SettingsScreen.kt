package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.ui.FxViewModel
import com.example.ui.theme.ElectricCyan
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: FxViewModel, onLogout: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val trades by viewModel.trades.collectAsStateWithLifecycle()
    val hasPin by viewModel.hasAppPinState.collectAsStateWithLifecycle()
    var showProfileEditor by remember { mutableStateOf(false) }
    var showPinEditor by remember { mutableStateOf(false) }
    var showPrivacyEditor by remember { mutableStateOf(false) }
    var showNotificationEditor by remember { mutableStateOf(false) }
    val notificationPreferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()
    var showPerformance by remember { mutableStateOf(false) }
    var showHelpCenter by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val profileStats = remember(trades) { calculateProfileStatistics(trades) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("settings_screen_container"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(user?.name ?: "Trader", user?.photoUri, 80.dp)
                Spacer(Modifier.height(12.dp))
                Text(user?.name ?: "Trader", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(user?.handle ?: "@trader", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsGroup("Account") {
                SettingsItem(Icons.Default.Person, "Edit Profile", "Name, handle and profile picture") { showProfileEditor = true }
                SettingsItem(Icons.Default.Security, "App Lock PIN", if (hasPin) "Enabled • 4 digits" else "Not configured") { showPinEditor = true }
                SettingsItem(Icons.Default.Visibility, "Public post visibility", if (user?.publicPostAudience == "followers") "Followers only" else "Everyone") { showPrivacyEditor = true }
            }
        }
        item {
            SettingsGroup("Preferences") {
                SettingsItem(Icons.Default.Palette, "Theme Settings", themeMode.name.lowercase().replaceFirstChar { it.uppercase() }) { viewModel.toggleThemeModal(true) }
                SettingsItem(Icons.Default.Notifications, "Notifications", "Choose which activity appears in your notification bell") { showNotificationEditor = true }
            }
        }
        item {
            SettingsGroup("Profile statistics") {
                val winRate = profileStats.winRatePercent?.let { String.format(Locale.US, "%.0f%% win rate", it) } ?: "No decisive results yet"
                SettingsItem(Icons.Default.Analytics, "Performance", "${profileStats.totalTrades} journal trades • $winRate") { showPerformance = true }
                SettingsItem(Icons.Default.CloudSync, "Backup and sync", "Bookmarks, profile and recently viewed posts") { viewModel.syncUserData() }
            }
        }
        item {
            SettingsGroup("Support") {
                SettingsItem(Icons.Default.Help, "Help Center", "FAQs and contact support") { showHelpCenter = true }
                SettingsItem(Icons.Default.Info, "About FX Journal", "Version ${BuildConfig.VERSION_NAME}") { showAbout = true }
            }
        }
        item {
            Spacer(Modifier.height(32.dp))
            Button(onClick = { viewModel.logout(); onLogout() }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("logout_button"), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .2f), contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign Out", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(64.dp))
        }
    }
    if (showProfileEditor) ProfileEditor(user?.name ?: "", user?.handle ?: "", user?.photoUri, user?.publicPostAudience ?: "everyone", viewModel) { showProfileEditor = false }
    if (showPinEditor) PinEditor(viewModel) { showPinEditor = false }
    if (showPrivacyEditor) PrivacyEditor(user?.publicPostAudience ?: "everyone", viewModel) { showPrivacyEditor = false }
    if (showNotificationEditor) NotificationPreferencesDialog(notificationPreferences, viewModel) { showNotificationEditor = false }
    if (showPerformance) ProfilePerformanceDialog(profileStats) { showPerformance = false }
    if (showHelpCenter) HelpCenterDialog(onEmailSupport = { openSupportEmail(context) }) { showHelpCenter = false }
    if (showAbout) AboutDialog(onClose = { showAbout = false }, onEmailSupport = { openSupportEmail(context) })
}

private const val SUPPORT_EMAIL = "smarttechlab.apps@gmail.com"

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$SUPPORT_EMAIL")
        putExtra(Intent.EXTRA_SUBJECT, "FX Journal support")
        putExtra(
            Intent.EXTRA_TEXT,
            "Hi FX Journal Support,\n\nApp version: ${BuildConfig.VERSION_NAME}\n\nPlease describe how we can help.\n"
        )
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found. You can email $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun ProfilePerformanceDialog(stats: ProfileStatistics, onClose: () -> Unit) {
    val moneyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val winRate = stats.winRatePercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
    val averageR = stats.averageR?.let { String.format(Locale.US, "%.2fR", it) } ?: "—"
    val netR = String.format(Locale.US, "%.2fR", stats.netR)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Profile performance") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Calculated from your personal journal. Community posts are not included.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceMetric("Journal trades", stats.totalTrades.toString(), Modifier.weight(1f))
                    PerformanceMetric("Win rate", winRate, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceMetric("Open", stats.openTrades.toString(), Modifier.weight(1f))
                    PerformanceMetric("Completed", stats.completedTrades.toString(), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceMetric("Net P&L", moneyFormat.format(stats.netProfitDollars), Modifier.weight(1f))
                    PerformanceMetric("Net R", netR, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceMetric("Wins", stats.wins.toString(), Modifier.weight(1f))
                    PerformanceMetric("Losses", stats.losses.toString(), Modifier.weight(1f))
                }
                Text("Average R: $averageR", style = MaterialTheme.typography.bodyMedium)
                Text("Break-even: ${stats.breakevens}  •  Cancelled: ${stats.cancelled}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Win rate = wins ÷ (wins + losses); break-even, open, and cancelled trades are excluded. P&L and R totals include completed trades only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (stats.totalTrades == 0) {
                    Text("Log a trade to start seeing your performance statistics.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}

@Composable
private fun PerformanceMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

@Composable
private fun HelpCenterDialog(onEmailSupport: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Help Center") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpAnswer("How do I log a trade?", "Tap the + button, enter the setup and outcome details, then save it to your journal.")
                HelpAnswer("Where are my journal entries?", "Open Personal Journal from the bottom navigation. Entries are stored on this device and can sync to your signed-in account when you use Backup and sync.")
                HelpAnswer("How do I share a setup?", "Open a journal trade and use its share action. Review the audience before publishing; public posts can be visible to other traders.")
                HelpAnswer("How do I get more help?", "Email support with the app version and a short description of the issue. Never send your password or brokerage login details.")
                Text("Support email", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer { Text(SUPPORT_EMAIL, style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = onEmailSupport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Email support")
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}

@Composable
private fun HelpAnswer(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(question, style = MaterialTheme.typography.titleSmall)
        Text(answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutDialog(onClose: () -> Unit, onEmailSupport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("About FX Journal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                Text("A journal for recording forex setups, reviewing trading decisions, and learning from your history.", style = MaterialTheme.typography.bodyMedium)
                Text("Support: $SUPPORT_EMAIL", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        dismissButton = { TextButton(onClick = onEmailSupport) { Text("Contact support") } }
    )
}

@Composable
private fun NotificationPreferencesDialog(preferences: Map<String, Boolean>, viewModel: FxViewModel, onClose: () -> Unit) {
    val options = listOf(
        "followingPosts" to "Posts from people I follow",
        "likes" to "Likes on my posts",
        "comments" to "Comments and replies on my posts",
        "newFollowers" to "New followers"
    )
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Notification preferences") },
        text = { Column { options.forEach { (key, label) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = preferences[key] == true, onCheckedChange = { viewModel.setNotificationPreference(key, it) })
            }
        } } },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
}

@Composable
private fun ProfileAvatar(name: String, photoUri: String?, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), Alignment.Center) {
        if (!photoUri.isNullOrBlank()) AsyncImage(photoUri, contentDescription = "Profile picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "TR" }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = ElectricCyan)
    }
}

@Composable
private fun ProfileEditor(name: String, handle: String, currentPhoto: String?, audience: String, viewModel: FxViewModel, onClose: () -> Unit) {
    var editedName by remember { mutableStateOf(name) }
    var editedHandle by remember { mutableStateOf(handle) }
    var photo by remember { mutableStateOf<android.net.Uri?>(currentPhoto?.let(android.net.Uri::parse)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { photo = it } }
    AlertDialog(onDismissRequest = onClose, title = { Text("Edit profile") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(), Alignment.Center) { ProfileAvatar(editedName, photo?.toString(), 76.dp) }
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text("Choose profile picture") }
            OutlinedTextField(editedName, { editedName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(editedHandle, { editedHandle = it }, label = { Text("Handle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { viewModel.updateProfile(editedName, editedHandle, photo, audience, onClose) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun PrivacyEditor(current: String, viewModel: FxViewModel, onClose: () -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Who can see my public posts?") }, text = {
        Column {
            Text("Choose the default audience for posts marked public.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("everyone" to "Everyone", "followers" to "Followers only").forEach { (value, label) ->
                Row(Modifier.fillMaxWidth().clickable { selected = value }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == value, { selected = value }); Text(label)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { viewModel.updateProfile(current, viewModel.currentUser.value?.handle ?: "", null, selected, onClose) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}

@Composable
private fun PinEditor(viewModel: FxViewModel, onClose: () -> Unit) {
    val hasPin by viewModel.hasAppPinState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (hasPin) "Change app lock PIN" else "Set app lock PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The app will ask for this PIN after it goes to the background.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("New 4-digit PIN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.ui.components.PinInput(value = pin, onValueChange = { pin = it; error = null }, isError = error != null, autoFocus = true)
                Spacer(Modifier.height(2.dp))
                Text("Confirm PIN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.ui.components.PinInput(value = confirm, onValueChange = { confirm = it; error = null }, isError = error != null)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length != 4 || pin != confirm) error = "PINs must match and contain exactly 4 digits"
                else viewModel.setAppPin(pin) { onClose() }
            }) { Text("Save PIN") }
        },
        dismissButton = {
            Row {
                if (hasPin) TextButton(onClick = { viewModel.removeAppPin(); onClose() }) { Text("Disable") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) { content() }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), Alignment.Center) { Icon(icon, null, tint = ElectricCyan, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
