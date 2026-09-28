package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.FxViewModel
import com.example.ui.theme.ElectricCyan

@Composable
fun SettingsScreen(viewModel: FxViewModel, onLogout: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val trades by viewModel.trades.collectAsStateWithLifecycle()
    var showProfileEditor by remember { mutableStateOf(false) }
    var showPinEditor by remember { mutableStateOf(false) }
    var showPrivacyEditor by remember { mutableStateOf(false) }
    var showNotificationEditor by remember { mutableStateOf(false) }
    val notificationPreferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()

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
                SettingsItem(Icons.Default.Security, "App Lock PIN", if (viewModel.hasAppPin) "Enabled • 4 digits" else "Not configured") { showPinEditor = true }
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
            val mine = trades.filter { it.authorHandle == user?.handle }
            val wins = mine.count { it.rMultiple > 0 }
            SettingsGroup("Profile statistics") {
                SettingsItem(Icons.Default.Analytics, "Performance", "${mine.size} posts • $wins wins • ${if (mine.isEmpty()) 0 else wins * 100 / mine.size}% win rate") { viewModel.showToast("Profile statistics updated from your journal") }
                SettingsItem(Icons.Default.CloudSync, "Backup and sync", "Bookmarks, profile and recently viewed posts") { viewModel.syncUserData() }
            }
        }
        item {
            SettingsGroup("Support") {
                SettingsItem(Icons.Default.Help, "Help Center") { viewModel.showToast("Opening Help Center...") }
                SettingsItem(Icons.Default.Info, "About FX Journal") { viewModel.showToast("FX Journal v2.4.0") }
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
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (viewModel.hasAppPin) "Change app lock PIN" else "Set app lock PIN") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The app will ask for this PIN after it goes to the background.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it }, label = { Text("New 4-digit PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirm, { if (it.length <= 4 && it.all(Char::isDigit)) confirm = it }, label = { Text("Confirm PIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = { if (pin.length != 4 || pin != confirm) error = "PINs must match and contain exactly 4 digits" else viewModel.setAppPin(pin) { onClose() } }) { Text("Save PIN") } }, dismissButton = { Row { if (viewModel.hasAppPin) TextButton(onClick = { viewModel.removeAppPin(); onClose() }) { Text("Disable") }; TextButton(onClick = onClose) { Text("Cancel") } } })
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
