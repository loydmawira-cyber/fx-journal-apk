package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Trade
import com.example.model.TradeDirection
import com.example.model.TradeStatus
import com.example.model.TradeVisibility
import com.example.ui.theme.CrimsonLossBright
import com.example.ui.theme.CrimsonLossContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.EmeraldProfitContainer
import com.example.ui.theme.OnElectricCyan

@Composable
fun TradeCard(
    trade: Trade,
    onClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: (Trade) -> Unit,
    isOwner: Boolean = false,
    onChangeVisibility: (TradeVisibility) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "PUBLIC", "PRIVATE" or "DELETE"
    val status = trade.status
    val outcomeColor = when(status) {
        TradeStatus.OPEN -> ElectricCyan
        TradeStatus.WINNER -> EmeraldProfit
        TradeStatus.STOPPED -> CrimsonLossBright
        TradeStatus.BREAKEVEN, TradeStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("trade_card_${trade.id}")
    ) {
        pendingAction?.let { action ->
            AlertDialog(
                onDismissRequest = { pendingAction = null },
                title = {
                    Text(
                        when (action) {
                            "PUBLIC" -> "Make this post public?"
                            "PRIVATE" -> "Make this post private?"
                            else -> "Delete this post?"
                        }
                    )
                },
                text = {
                    Text(
                        when (action) {
                            "PUBLIC" -> "Everyone will be able to see it in the community feed, and vote and comment on it."
                            "PRIVATE" -> "It will be removed from the community feed. Only you will see it in your journal."
                            else -> "This permanently deletes the post from your journal and the feed. This can't be undone."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingAction = null
                            when (action) {
                                "PUBLIC" -> onChangeVisibility(TradeVisibility.PUBLIC)
                                "PRIVATE" -> onChangeVisibility(TradeVisibility.PRIVATE)
                                else -> onDelete()
                            }
                        }
                    ) {
                        Text(
                            text = when (action) {
                                "PUBLIC" -> "Make Public"
                                "PRIVATE" -> "Make Private"
                                else -> "Delete"
                            },
                            color = if (action == "DELETE") CrimsonLossBright else ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
                }
            )
        }

        // High-Frequency Edge Indicator Accent Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(outcomeColor)
        )

        // Trader Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Trader Avatar Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = trade.authorName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                    AvatarContent(
                        handle = trade.authorHandle,
                        initials = initials.ifBlank { "TR" },
                        textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textColor = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trade.authorHandle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = trade.timeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Badges & Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trade.visibility == TradeVisibility.PUBLIC) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "PUBLIC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricCyan
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "PRIVATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(28.dp).testTag("trade_menu_${trade.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            val makePublic = trade.visibility != TradeVisibility.PUBLIC
                            DropdownMenuItem(
                                text = { Text(if (makePublic) "Make Public" else "Make Private") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (makePublic) Icons.Default.Public else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    pendingAction = if (makePublic) "PUBLIC" else "PRIVATE"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete post", color = CrimsonLossBright) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = CrimsonLossBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    pendingAction = "DELETE"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Pair & Direction Badge Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trade.pair,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Direction Pill
                val dirBg = if (trade.rMultiple < 0) {
                    CrimsonLossContainer.copy(alpha = 0.4f)
                } else if (trade.direction == TradeDirection.LONG) {
                    EmeraldProfitContainer.copy(alpha = 0.3f)
                } else {
                    CrimsonLossContainer.copy(alpha = 0.4f)
                }

                val dirTextColor = if (trade.rMultiple < 0) CrimsonLossBright else if (trade.direction == TradeDirection.LONG) EmeraldProfit else CrimsonLossBright

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dirBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (trade.rMultiple < 0) Icons.Default.Block else if (trade.direction == TradeDirection.LONG) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = dirTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (trade.rMultiple < 0) "STOPPED" else if (trade.direction == TradeDirection.LONG) "BUY" else "SELL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = dirTextColor
                    )
                }
            }

            // Strategy Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = trade.setupStrategy,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Only show the figures the trader actually filled in
        val showEntry = trade.entryPrice != 0.0
        val showTarget = (if (trade.rMultiple < 0) trade.stopLoss else trade.takeProfit) != 0.0
        val ratioValue = trade.riskRewardRatio.split(":").lastOrNull()?.trim()?.toDoubleOrNull() ?: 0.0
        val showRatio = if (trade.rMultiple < 0) {
            trade.entryPrice != 0.0 && trade.stopLoss != 0.0
        } else {
            trade.entryPrice != 0.0 && trade.stopLoss != 0.0 && trade.takeProfit != 0.0 && ratioValue > 0.0
        }
        val showOutcome = status != TradeStatus.OPEN

        // Quantitative Telemetry Grid (hidden columns when not set)
        if (showEntry || showTarget || showRatio || showOutcome) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showEntry) Column {
                Text(
                    text = "ENTRY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (trade.entryPrice == 0.0) "Not provided" else String.format("%.5f", trade.entryPrice).trimEnd('0').trimEnd('.'),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (showTarget) Column {
                Text(
                    text = if (trade.rMultiple < 0) "STOP HIT" else "TARGET",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if ((if (trade.rMultiple < 0) trade.stopLoss else trade.takeProfit) == 0.0) "Not provided" else String.format("%.5f", if (trade.rMultiple < 0) trade.stopLoss else trade.takeProfit).trimEnd('0').trimEnd('.'),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = outcomeColor
                )
            }

            if (showRatio) Column {
                Text(
                    text = if (trade.rMultiple < 0) "RISK %" else "R:R RATIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (trade.rMultiple < 0) "${trade.riskPercent}%" else trade.riskRewardRatio,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = ElectricCyan
                )
            }

            if (showOutcome) Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "OUTCOME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = outcomeColor
                )
            }
        }

        // High-Res Chart Snapshot (if available)
        if (trade.chartImageUri != null || trade.chartDrawableRes != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                if (trade.chartImageUri != null) {
                    AsyncImage(
                        model = trade.chartImageUri,
                        contentDescription = "Chart analysis",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else if (trade.chartDrawableRes != null) {
                    Image(
                        painter = painterResource(id = trade.chartDrawableRes),
                        contentDescription = "Chart analysis",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay pill top left
                if (trade.chartOverlayText != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(outcomeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = trade.chartOverlayText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Overlay pill bottom right (e.g. Tape Speed)
                if (trade.tapeSpeed != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Tape Speed: ${trade.tapeSpeed}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Thesis & Psychology Callout
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            if (trade.rMultiple < 0 && trade.psychologyNote != null) {
                // Psychology Review Callout Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                    ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Discipline review", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ElectricCyan)
                        Text("Psychology Review: ${trade.psychologyNote}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else if (trade.executionThesis.isNotBlank()) {
                Row {
                    Text(
                        text = "Execution thesis: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = ElectricCyan
                    )
                    Text(
                        text = trade.executionThesis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Social & Tactical Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Upvote / Downvote Segment
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onUpvoteClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Upvote",
                        tint = if (trade.isUpvoted) EmeraldProfit else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trade.upvotes}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = if (trade.isUpvoted) EmeraldProfit else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )

                Row(
                    modifier = Modifier
                        .clickable { onDownvoteClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Downvote",
                        tint = if (trade.isDownvoted) CrimsonLossBright else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trade.downvotes}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                        ),
                        color = if (trade.isDownvoted) CrimsonLossBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Comments & Bookmark & Share
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { onCommentClick() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trade.commentsCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { onBookmarkClick() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trade.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (trade.isBookmarked) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { onShareClick(trade) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
