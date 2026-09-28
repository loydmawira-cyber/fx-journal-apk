package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.AppNotification
import com.example.ui.FxViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationCenter(notifications: List<AppNotification>, viewModel: FxViewModel, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Notifications") },
        text = {
            if (notifications.isEmpty()) {
                Text("You have no notifications yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(notifications, key = { it.id }) { notification ->
                        Row(
                            Modifier.fillMaxWidth().background(if (notification.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer).padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(notification.title, style = MaterialTheme.typography.titleSmall)
                                Text(notification.body, style = MaterialTheme.typography.bodySmall)
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(notification.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.markAllNotificationsRead(); onClose() }) { Text("Mark all read") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
