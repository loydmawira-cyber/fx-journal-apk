package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Trade
import com.example.model.TradeVisibility
import com.example.ui.FxViewModel
import com.example.ui.components.DailySummaryCard
import com.example.ui.components.TradeCalendar
import com.example.ui.components.TradeCard
import java.util.Date
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ThemeMode

@Composable
fun JournalScreen(
    viewModel: FxViewModel,
    onTradeClick: (Trade) -> Unit
) {
    val trades by viewModel.trades.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val journalTab by viewModel.journalTab.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.journalPairFilter.collectAsStateWithLifecycle()
    val recentIds by viewModel.recentTradeIds.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var dailyTrades by remember { mutableStateOf<List<Trade>>(emptyList()) }
    var savedQuery by remember { mutableStateOf("") }
    var savedFolder by remember { mutableStateOf("All folders") }
    var sortSavedByR by remember { mutableStateOf(false) }
    var editingSaved by remember { mutableStateOf<Trade?>(null) }
    var shareTrade by remember { mutableStateOf<Trade?>(null) }

    val userTrades = trades.filter { it.authorHandle == user?.handle }
    val recentTrades = recentIds.mapNotNull { id -> trades.firstOrNull { it.id == id } }.take(3)

    val filteredTrades = userTrades.filter { trade ->
        val matchesVisibility = when (journalTab) {
            "private" -> trade.visibility == TradeVisibility.PRIVATE
            "public" -> trade.visibility == TradeVisibility.PUBLIC
            "bookmarked" -> trade.isBookmarked
            else -> true
        }

        val matchesFilter = if (selectedFilter == null) true else {
            trade.pair.contains(selectedFilter!!, ignoreCase = true) ||
                    trade.setupStrategy.contains(selectedFilter!!, ignoreCase = true)
        }

        val matchesSavedQuery = journalTab != "bookmarked" || savedQuery.isBlank() || trade.pair.contains(savedQuery, true) || trade.setupStrategy.contains(savedQuery, true) || trade.authorName.contains(savedQuery, true)
        val matchesFolder = journalTab != "bookmarked" || savedFolder == "All folders" || viewModel.savedFolder(trade.id) == savedFolder
        matchesVisibility && matchesFilter && matchesSavedQuery && matchesFolder
    }.let { list -> if (journalTab == "bookmarked" && sortSavedByR) list.sortedByDescending { it.rMultiple } else list }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("journal_screen_container"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Quick Stats Tactical Ribbon (4 Columns)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val wins = userTrades.count { it.rMultiple > 0 }
                val losses = userTrades.count { it.rMultiple < 0 }
                val winRate = if (userTrades.isNotEmpty()) (wins.toFloat() / userTrades.size * 100).toInt() else 0
                val avgRR = if (userTrades.isNotEmpty()) {
                    val totalRR = userTrades.mapNotNull { it.riskRewardRatio.split(":").lastOrNull()?.trim()?.toDoubleOrNull() }.sum()
                    String.format("%.1f", totalRR / userTrades.size)
                } else "0.0"

                RibbonStatItem(
                    label = "WIN RATE",
                    value = "$winRate%",
                    sub = "${wins}W / ${losses}L",
                    valColor = EmeraldProfit,
                    modifier = Modifier.weight(1f)
                )
                RibbonStatItem(
                    label = "TRADES",
                    value = "${userTrades.size}",
                    sub = "Executed",
                    valColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                RibbonStatItem(
                    label = "AVG R:R",
                    value = "1:$avgRR",
                    sub = "Per Setup",
                    valColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                RibbonStatItem(
                    label = "STREAK",
                    value = "3W 🔥",
                    sub = "Current",
                    valColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Segmented Journal Visibility Switcher (All / Private / Shared)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val allCount = userTrades.size
                    val privCount = userTrades.count { it.visibility == TradeVisibility.PRIVATE }
                    val pubCount = userTrades.count { it.visibility == TradeVisibility.PUBLIC }

                    JournalVisTab(
                        title = "All ($allCount)",
                        icon = null,
                        isSelected = journalTab == "all",
                        onClick = { viewModel.setJournalTab("all") },
                        modifier = Modifier.weight(1f)
                    )
                    JournalVisTab(
                        title = "Private ($privCount)",
                        icon = Icons.Default.Lock,
                        isSelected = journalTab == "private",
                        onClick = { viewModel.setJournalTab("private") },
                        modifier = Modifier.weight(1f)
                    )
                    JournalVisTab(
                        title = "Shared ($pubCount)",
                        icon = Icons.Default.Public,
                        isSelected = journalTab == "public",
                        onClick = { viewModel.setJournalTab("public") },
                        modifier = Modifier.weight(1f)
                    )
                    JournalVisTab(
                        title = "Saved (${userTrades.count { it.isBookmarked }})",
                        icon = Icons.Default.Bookmark,
                        isSelected = journalTab == "bookmarked",
                        onClick = { viewModel.setJournalTab("bookmarked") },
                        modifier = Modifier.weight(1f)
                    )
                    JournalVisTab(
                        title = "Calendar",
                        icon = Icons.Default.CalendarMonth,
                        isSelected = journalTab == "calendar",
                        onClick = { viewModel.setJournalTab("calendar") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Filter Pills Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Filter:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    listOf("EUR/USD", "BTC", "Gold", "SRC", "SMC", "Scalp", "Trend").forEach { filterTag ->
                        val isSelected = selectedFilter == filterTag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) ElectricCyan else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { viewModel.setJournalPairFilter(filterTag) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = filterTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                if (journalTab == "bookmarked") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(savedQuery, { savedQuery = it }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) }, placeholder = { Text("Search saved posts") })
                        Icon(Icons.Default.Sort, "Sort saved posts", tint = if (sortSavedByR) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).clickable { sortSavedByR = !sortSavedByR })
                    }
                    Text("Folder: $savedFolder (tap to change)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp).clickable { savedFolder = when (savedFolder) { "All folders" -> "General"; "General" -> "Study later"; "Study later" -> "High R:R"; else -> "All folders" } })
                }
            }
        }

        if (journalTab != "bookmarked" && recentTrades.isNotEmpty()) {
            item {
                Text("Recently viewed", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentTrades.forEach { recent ->
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).clickable { onTradeClick(recent) }.padding(10.dp)) { Text("${recent.pair} • ${recent.setupStrategy}", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        // Journal Trades List
        if (journalTab == "calendar") {
            item {
                TradeCalendar(
                    trades = trades.filter { it.authorHandle == user?.handle },
                    onDateSelected = { date, dayTrades ->
                        selectedDate = date
                        dailyTrades = dayTrades
                    }
                )
            }
            
            if (selectedDate != null) {
                item {
                    DailySummaryCard(
                        date = selectedDate!!,
                        trades = dailyTrades,
                        onClose = { selectedDate = null }
                    )
                }
            }
        } else {
            items(filteredTrades, key = { it.id }) { trade ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Column {
                        TradeCard(trade = trade, onClick = { onTradeClick(trade) }, onUpvoteClick = { viewModel.toggleUpvote(trade) }, onDownvoteClick = { viewModel.toggleDownvote(trade) }, onCommentClick = { onTradeClick(trade) }, onBookmarkClick = { viewModel.toggleBookmark(trade) }, onShareClick = { shareTrade = it })
                        if (journalTab == "bookmarked") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("${viewModel.savedFolder(trade.id)}${viewModel.savedNote(trade.id).let { if (it.isBlank()) "" else " • $it" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                TextButton(onClick = { editingSaved = trade }) { Text("Folder / note") }
                            }
                        }
                    }
                }
            }
        }

        // Export PDF / CSV Strategic Action
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        viewModel.showToast("Audit packet & CSV compiled! Ready to download.")
                    }
                    .padding(16.dp)
                    .testTag("export_journal_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export PDF / CSV Journal",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Structured metrics for Tax & Prop Firm Mentorship",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Export",
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    editingSaved?.let { trade ->
        var folder by remember(trade.id) { mutableStateOf(viewModel.savedFolder(trade.id)) }
        var note by remember(trade.id) { mutableStateOf(viewModel.savedNote(trade.id)) }
        AlertDialog(
            onDismissRequest = { editingSaved = null },
            title = { Text("Saved post organization") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(folder, { folder = it }, label = { Text("Folder") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Private note") }, minLines = 2)
            } },
            confirmButton = { TextButton(onClick = { viewModel.saveSavedMetadata(trade.id, folder, note); editingSaved = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingSaved = null }) { Text("Cancel") } }
        )
    }
    shareTrade?.let { trade ->
        SharePostDialog(trade = trade, onShareToFeed = { viewModel.shareTradeToFeed(it) }, onDismiss = { shareTrade = null })
    }
}

@Composable
private fun RibbonStatItem(
    label: String,
    value: String,
    sub: String,
    valColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            color = valColor,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalVisTab(
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
