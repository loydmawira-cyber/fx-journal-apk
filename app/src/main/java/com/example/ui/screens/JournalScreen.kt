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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var dailyTrades by remember { mutableStateOf<List<Trade>>(emptyList()) }

    val userTrades = trades.filter { it.authorHandle == (user?.handle ?: "@QuantAlex") }

    val filteredTrades = userTrades.filter { trade ->
        val matchesVisibility = when (journalTab) {
            "private" -> trade.visibility == TradeVisibility.PRIVATE
            "public" -> trade.visibility == TradeVisibility.PUBLIC
            else -> true
        }

        val matchesFilter = if (selectedFilter == null) true else {
            trade.pair.contains(selectedFilter!!, ignoreCase = true) ||
                    trade.setupStrategy.contains(selectedFilter!!, ignoreCase = true)
        }

        matchesVisibility && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("journal_screen_container"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Trader Performance Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AV",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ElectricCyan
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldProfit)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                    text = user?.name ?: "Trader",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${user?.handle ?: "@trader"} · Prop Verified Trader",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(6.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { viewModel.showToast("Journal shared to profile link!") }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Total R-Multiple Summary & Mini Curve
                    val totalRMultiple = filteredTrades.sumOf { it.rMultiple }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TOTAL R-GAINS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "${if (totalRMultiple >= 0) "+" else ""}${String.format("%.1f", totalRMultiple)}R",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 22.sp
                                    ),
                                    color = if (totalRMultiple >= 0) EmeraldProfit else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background((if (totalRMultiple >= 0) EmeraldProfit else MaterialTheme.colorScheme.error).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    val winRate = if (filteredTrades.isNotEmpty()) {
                                        (filteredTrades.count { it.rMultiple > 0 }.toFloat() / filteredTrades.size * 100).toInt()
                                    } else 0
                                    Text(
                                        text = "$winRate% WR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (totalRMultiple >= 0) EmeraldProfit else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = "Performance Cycle",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Luminous Sparkline Graph
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(50.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val points = listOf(
                                    0f to h * 0.85f,
                                    w * 0.15f to h * 0.75f,
                                    w * 0.28f to h * 0.80f,
                                    w * 0.42f to h * 0.60f,
                                    w * 0.55f to h * 0.65f,
                                    w * 0.70f to h * 0.40f,
                                    w * 0.82f to h * 0.48f,
                                    w * 0.92f to h * 0.22f,
                                    w to h * 0.10f
                                )

                                val linePath = Path().apply {
                                    moveTo(points[0].first, points[0].second)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].first, points[i].second)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(linePath)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            EmeraldProfit.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )

                                drawPath(
                                    path = linePath,
                                    color = EmeraldProfit,
                                    style = Stroke(
                                        width = 2.5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                drawCircle(
                                    color = EmeraldProfit,
                                    radius = 4f,
                                    center = androidx.compose.ui.geometry.Offset(w, h * 0.10f)
                                )
                            }
                        }
                    }
                }
            }
        }

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
            }
        }

        // Journal Trades List
        if (journalTab == "calendar") {
            item {
                TradeCalendar(
                    trades = trades.filter { it.authorHandle == "@QuantAlex" },
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
                    TradeCard(
                        trade = trade,
                        onClick = { onTradeClick(trade) },
                        onUpvoteClick = { viewModel.toggleUpvote(trade) },
                        onCommentClick = { onTradeClick(trade) },
                        onBookmarkClick = { viewModel.toggleBookmark(trade) },
                        onShareClick = { viewModel.showToast("Setup copied!") }
                    )
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
