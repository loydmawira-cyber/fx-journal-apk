package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.Trade

private fun shareText(trade: Trade): String = buildString {
    append("${trade.pair} ${if (trade.direction == com.example.model.TradeDirection.LONG) "BUY" else "SELL"} setup by ${trade.authorName} (${trade.authorHandle})\n")
    append("Strategy: ${trade.setupStrategy}\n")
    append("Timeframe: ${trade.timeframe}${if (trade.riskRewardRatio.isNotBlank()) " • R:R ${trade.riskRewardRatio}" else ""}\n")
    append("Entry: ${trade.entryPrice} | SL: ${trade.stopLoss} | TP: ${trade.takeProfit}\n")
    append("Shared from FX Journal")
}

private fun launchExternalShare(context: Context, trade: Trade, target: String?) {
    val text = shareText(trade)
    val intent = if (target == "whatsapp") {
        Intent(Intent.ACTION_SEND).apply { type = "text/plain"; setPackage("com.whatsapp"); putExtra(Intent.EXTRA_TEXT, text) }
    } else if (target == "email") {
        Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:"); putExtra(Intent.EXTRA_SUBJECT, "FX Journal trade setup: ${trade.pair}"); putExtra(Intent.EXTRA_TEXT, text) }
    } else {
        Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    }
    try { context.startActivity(if (target == null) Intent.createChooser(intent, "Share FX Journal post") else intent) }
    catch (_: Exception) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share FX Journal post")) }
}

@Composable
fun SharePostDialog(trade: Trade, onShareToFeed: (Trade) -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share ${trade.pair} setup") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = { onShareToFeed(trade); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Share to Feed") }
                TextButton(onClick = { launchExternalShare(context, trade, "whatsapp"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Share to WhatsApp") }
                TextButton(onClick = { launchExternalShare(context, trade, "email"); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Share by Email") }
                TextButton(onClick = { launchExternalShare(context, trade, null); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("More sharing options") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
