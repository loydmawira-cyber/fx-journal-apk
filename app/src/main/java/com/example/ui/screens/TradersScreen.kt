package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TraderProfile
import com.example.ui.FxViewModel
import com.example.ui.theme.CrimsonLossBright
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.OnElectricCyan

@Composable
fun TradersScreen(viewModel: FxViewModel) {
    val traders by viewModel.traders.collectAsStateWithLifecycle()
    val followers by viewModel.followers.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    var dialogOnFollowers by remember { mutableStateOf<Boolean?>(null) }

    val sorted = traders.sortedByDescending { it.followers.toIntOrNull() ?: 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("traders_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Summary card: my followers / following + number of traders
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp)
            ) {
                SummaryStat("Followers", followers.size.toString(), Modifier.weight(1f)) { dialogOnFollowers = true }
                SummaryDivider()
                SummaryStat("Following", following.size.toString(), Modifier.weight(1f)) { dialogOnFollowers = false }
                SummaryDivider()
                SummaryStat("Traders", traders.size.toString(), Modifier.weight(1f), onClick = null)
            }
            Text(
                text = "Tap Followers or Following to see who they are",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
            )
        }

        if (sorted.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No other traders yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Traders show up here once they post a trade publicly. Invite friends to join and share your own trades to the feed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "COMMUNITY TRADERS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }
            items(sorted, key = { it.id }) { trader ->
                TraderCardItem(
                    trader = trader,
                    onFollowToggle = { viewModel.toggleFollowTrader(trader.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    dialogOnFollowers?.let { onFollowers ->
        FollowListDialog(viewModel, startOnFollowers = onFollowers, onDismiss = { dialogOnFollowers = null })
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier, onClick: (() -> Unit)?) {
    Column(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (onClick != null) ElectricCyan else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .align(Alignment.CenterVertically)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun TraderCardItem(
    trader: TraderProfile,
    onFollowToggle: () -> Unit
) {
    val winRateValue = trader.winRate.removeSuffix("%").toIntOrNull() ?: 0
    val netPositive = !trader.netRGain.startsWith("-")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("trader_item_${trader.id}")
    ) {
        // Top row: avatar, name/handle, follow button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val initials = trader.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                    .take(2).joinToString("").uppercase().ifBlank { "T" }
                com.example.ui.components.AvatarContent(
                    handle = trader.handle,
                    initials = initials,
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    textColor = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trader.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = trader.handle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (trader.isFollowing) {
                OutlinedButton(
                    onClick = onFollowToggle,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Following",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onFollowToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = OnElectricCyan),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Follow",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats: 4 equal columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(vertical = 10.dp)
        ) {
            TraderStat("WIN RATE", trader.winRate, if (winRateValue >= 50) EmeraldProfit else CrimsonLossBright, Modifier.weight(1f))
            TraderStat("TOTAL R", trader.netRGain, if (netPositive) EmeraldProfit else CrimsonLossBright, Modifier.weight(1f))
            TraderStat("POSTS", trader.streak, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            TraderStat("FOLLOWERS", trader.followers, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TraderStat(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
