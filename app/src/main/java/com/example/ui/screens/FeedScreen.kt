package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Trade
import com.example.ui.FxViewModel
import com.example.ui.components.TradeCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.OnElectricCyan
import com.example.ui.theme.ThemeMode

@Composable
fun FeedScreen(
    viewModel: FxViewModel,
    onTradeClick: (Trade) -> Unit
) {
    val trades by viewModel.feedTrades.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val feedTab by viewModel.feedTab.collectAsStateWithLifecycle()
    val pairFilter by viewModel.feedPairFilter.collectAsStateWithLifecycle()
    val strategyFilter by viewModel.feedStrategyFilter.collectAsStateWithLifecycle()
    val traders by viewModel.traders.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    var showPairSearch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showStrategySearch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var sortByR by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var shareTrade by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Trade?>(null) }

    val followingHandles = traders.filter { it.isFollowing }.map { it.handle }.toSet()
    val followingCount = traders.count { it.isFollowing }
    val filteredTrades = trades.filter { trade ->
        val audienceAllows = trade.visibility != com.example.model.TradeVisibility.PUBLIC ||
            trade.publicPostAudience == "everyone" ||
            trade.authorHandle == user?.handle ||
            trade.authorHandle in followingHandles
        val matchesTab = if (feedTab == "following") {
            trade.authorHandle in followingHandles || trade.authorHandle == user?.handle
        } else true

        val matchesPair = if (pairFilter == "All Pairs") true else trade.pair.contains(pairFilter, ignoreCase = true)
        val matchesStrategy = if (strategyFilter == "All") true else trade.setupStrategy.contains(strategyFilter, ignoreCase = true)

        audienceAllows && matchesTab && matchesPair && matchesStrategy
    }.let { result -> if (sortByR) result.sortedByDescending { it.rMultiple } else result.sortedByDescending { it.timestamp } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("feed_stream_list"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Sticky Pulse Bar + Filter Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Segmented Public Feed vs Following Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (feedTab == "public") MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                                .clickable { viewModel.setFeedTab("public") }
                                .padding(vertical = 7.dp)
                                .testTag("feed_tab_public"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = if (feedTab == "public") ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Community Feed",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (feedTab == "public") ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (feedTab == "following") MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                                .clickable { viewModel.setFeedTab("following") }
                                .padding(vertical = 7.dp)
                                .testTag("feed_tab_following"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (feedTab == "following") ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Following",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = if (feedTab == "following") ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricCyan)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = followingCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = OnElectricCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Filter & Ticker Carousel Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Pairs Selector
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (pairFilter == "All Pairs") MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { showPairSearch = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("filter_all_pairs"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CandlestickChart,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (pairFilter == "All Pairs") "All Pairs" else pairFilter,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Strategy Filter
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showStrategySearch = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (strategyFilter == "All") "Strategy" else strategyFilter,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Top P&L Sort
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sortByR) EmeraldProfit.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest)
                                .clickable { sortByR = !sortByR }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldProfit,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Top R:R",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = EmeraldProfit
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick Pair Tags
                        listOf("EUR/USD", "BTC/USDT", "ICT Silver Bullet", "SRC", "XAU/USD").forEach { tag ->
                            val isSelected = pairFilter.contains(tag, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isSelected) ElectricCyan else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.setFeedPairFilter(if (isSelected) "All Pairs" else tag)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }

            if (feedTab == "following") {
                item {
                    Text("Profiles you follow", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                }
                items(traders.filter { it.isFollowing }, key = { "following_${it.id}" }) { trader ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), Alignment.Center) {
                            Text(trader.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), color = ElectricCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(trader.name, fontWeight = FontWeight.Bold); if (trader.isVerified) Icon(Icons.Default.Verified, null, tint = ElectricCyan, modifier = Modifier.size(15.dp).padding(start = 2.dp)) }
                            Text(trader.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${trader.followers} followers • ${trader.netRGain}", style = MaterialTheme.typography.labelSmall, color = EmeraldProfit)
                        }
                        Button(onClick = { viewModel.toggleFollowTrader(trader.id) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("Following", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                if (traders.none { it.isFollowing }) item { Text("You are not following anyone yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            } else {
                items(filteredTrades, key = { it.id }) { trade ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        TradeCard(trade = trade, onClick = { onTradeClick(trade) }, onUpvoteClick = { viewModel.toggleUpvote(trade) }, onDownvoteClick = { viewModel.toggleDownvote(trade) }, onCommentClick = { onTradeClick(trade) }, onBookmarkClick = { viewModel.toggleBookmark(trade) }, onShareClick = { shareTrade = it }, isOwner = viewModel.isMyTrade(trade), onChangeVisibility = { viewModel.changeTradeVisibility(trade, it) }, onDelete = { viewModel.deleteMyTrade(trade) })
                    }
                }
            }
        }

        if (showPairSearch) {
            FeedFilterDialog(
                title = "Search symbol",
                placeholder = "EUR/USD, BTC/USDT, XAU/USD",
                options = listOf("All Pairs") + trades.map { it.pair }.distinct().sorted(),
                selected = pairFilter,
                onSelect = { viewModel.setFeedPairFilter(it); showPairSearch = false },
                onDismiss = { showPairSearch = false }
            )
        }
        if (showStrategySearch) {
            FeedFilterDialog(
                title = "Search strategy",
                placeholder = "SMC, Liquidity, Scalp...",
                options = listOf("All") + trades.map { it.setupStrategy }.distinct().sorted(),
                selected = strategyFilter,
                onSelect = { viewModel.setFeedStrategyFilter(it); showStrategySearch = false },
                onDismiss = { showStrategySearch = false }
            )
        }
        shareTrade?.let { trade ->
            SharePostDialog(trade = trade, onShareToFeed = { viewModel.shareTradeToFeed(it) }, onDismiss = { shareTrade = null })
        }

        // Tactical Floating Filter Trigger (Bottom Right Reach Zone)
        FloatingActionButton(
            onClick = { viewModel.showToast("Filters: Active pairs, sessions, min R:R") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 85.dp)
                .size(48.dp)
                .testTag("floating_filter_btn"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = ElectricCyan
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
