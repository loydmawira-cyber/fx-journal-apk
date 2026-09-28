package com.example.data

import com.example.R
import com.example.model.Trade
import com.example.model.TradeDirection
import com.example.model.TradeVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TradeRepository(private val tradeDao: TradeDao) {

    val allTrades: Flow<List<Trade>> = tradeDao.getAllTrades().map { list ->
        list.map { it.toDomain() }
    }

    val publicTrades: Flow<List<Trade>> = tradeDao.getPublicTrades().map { list ->
        list.map { it.toDomain() }
    }

    val privateTrades: Flow<List<Trade>> = tradeDao.getPrivateTrades().map { list ->
        list.map { it.toDomain() }
    }

    fun getTradeById(id: Long): Flow<Trade?> = tradeDao.getTradeById(id).map { it?.toDomain() }

    suspend fun insertTrade(trade: Trade): Long {
        return tradeDao.insertTrade(TradeEntity.fromDomain(trade))
    }

    suspend fun updateTrade(trade: Trade) {
        tradeDao.updateTrade(TradeEntity.fromDomain(trade))
    }

    suspend fun toggleUpvote(tradeId: Long, currentUpvoted: Boolean) {
        val delta = if (currentUpvoted) -1 else 1
        tradeDao.updateUpvote(tradeId, delta, !currentUpvoted)
    }

    suspend fun toggleDownvote(tradeId: Long, currentDownvoted: Boolean) {
        val delta = if (currentDownvoted) -1 else 1
        tradeDao.updateDownvote(tradeId, delta, !currentDownvoted)
    }

    suspend fun toggleBookmark(tradeId: Long, currentBookmarked: Boolean) {
        tradeDao.updateBookmark(tradeId, !currentBookmarked)
    }

    fun checkAndSeedInitialData(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val existing = tradeDao.getAllTrades().first()
            if (existing.isEmpty()) {
                val seedList = getInitialSeedTrades().map { TradeEntity.fromDomain(it) }
                tradeDao.insertAll(seedList)
            }
        }
    }

    private fun getInitialSeedTrades(): List<Trade> {
        val now = System.currentTimeMillis()
        return listOf(
            Trade(
                id = 1,
                pair = "EUR/USD",
                direction = TradeDirection.LONG,
                setupStrategy = "SMC Order Block",
                session = "London Session",
                timeframe = "15M",
                entryPrice = 1.08420,
                stopLoss = 1.08200,
                takeProfit = 1.09210,
                exitPrice = 1.09210,
                positionSizeLots = 2.50,
                riskRewardRatio = "1:3.81",
                rMultiple = 3.8,
                netGainDollars = 4520.00,
                riskPercent = 1.0,
                maxRiskDollars = 1200.0,
                visibility = TradeVisibility.PUBLIC,
                status = com.example.model.TradeStatus.WINNER,
                timestamp = now - 42 * 60 * 1000,
                timeAgo = "London Session • 42m ago",
                authorName = "Alex Vance",
                authorHandle = "@QuantAlex",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = "Clean liquidity sweep during London open. Price retested the 15m bullish mitigation block with heavy aggressive market orders and negative delta absorption.",
                psychologyNote = "Stuck precisely to the plan. Waited patiently for the London low to be taken out before placing limit order.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("Forex", "PriceAction", "SMC", "Live Metatrader 5"),
                chartDrawableRes = R.drawable.img_eurusd_chart,
                chartOverlayText = "15M Re-accumulation",
                tapeSpeed = "+2.4x",
                upvotes = 342,
                downvotes = 12,
                commentsCount = 84,
                isUpvoted = false,
                isBookmarked = false,
                brokerName = "IC Markets (cTrader Raw)",
                slippagePips = 0.1,
                durationText = "2h 45m"
            ),
            Trade(
                id = 2,
                pair = "BTC/USDT",
                direction = TradeDirection.SHORT,
                setupStrategy = "Liquidity Grab",
                session = "Perpetuals",
                timeframe = "1H",
                entryPrice = 68850.0,
                stopLoss = 69750.0,
                takeProfit = 64200.0,
                exitPrice = 64200.0,
                positionSizeLots = 1.80,
                riskRewardRatio = "1:5.20",
                rMultiple = 5.2,
                netGainDollars = 12840.00,
                riskPercent = 1.2,
                maxRiskDollars = 2400.0,
                visibility = TradeVisibility.PUBLIC,
                status = com.example.model.TradeStatus.WINNER,
                timestamp = now - 2 * 3600 * 1000,
                timeAgo = "Perpetuals • 2h ago",
                authorName = "Satoshi Scalper",
                authorHandle = "@SatoshiScalper",
                authorTier = com.example.model.TraderTier.LIVE,
                isAuthorVerified = true,
                executionThesis = "Weekly high swept with immediate bearish market structure break on the 1-hour chart. Sized up on the retest of the Fair Value Gap.",
                psychologyNote = "Took 75% off at first HTF target, letting 25% runner ride to key weekly support.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("Crypto", "Perps", "FairValueGap", "Macro"),
                chartDrawableRes = R.drawable.img_btcusdt_chart,
                chartOverlayText = "Target Reached • Partial 75%",
                tapeSpeed = "+1.8x",
                upvotes = 512,
                downvotes = 18,
                commentsCount = 119,
                isUpvoted = true,
                isBookmarked = true,
                brokerName = "Bybit Derivatives",
                slippagePips = 0.4,
                durationText = "4h 10m"
            ),
            Trade(
                id = 3,
                pair = "XAU/USD",
                direction = TradeDirection.SHORT,
                setupStrategy = "Scalp Invalidation",
                session = "NY Session",
                timeframe = "15M",
                entryPrice = 2382.40,
                stopLoss = 2378.10,
                takeProfit = 2360.00,
                exitPrice = 2378.10,
                positionSizeLots = 1.10,
                riskRewardRatio = "1:2.0",
                rMultiple = -1.0,
                netGainDollars = -475.00,
                riskPercent = 0.95,
                maxRiskDollars = 500.0,
                visibility = TradeVisibility.PUBLIC,
                status = com.example.model.TradeStatus.STOPPED,
                timestamp = now - 4 * 3600 * 1000,
                timeAgo = "NY Session • 4h ago",
                authorName = "Elena Gold",
                authorHandle = "@ElenaGold",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = "Attempted scalp following early NY volatility spike; price failed to hold below supply cluster.",
                psychologyNote = "Stuck precisely to the predetermined stop loss without manual revenge trading. CPI volatility spiked early; preserved capital for higher probability setups.",
                postMortemReflection = "Good discipline stopping out cleanly. Avoided entering again until HTF 4H close.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("Gold", "NYSession", "Discipline", "PropFirm"),
                chartDrawableRes = null,
                chartOverlayText = "Discipline Log • Risk Respected",
                tapeSpeed = null,
                upvotes = 189,
                downvotes = 5,
                commentsCount = 42,
                isUpvoted = false,
                isBookmarked = false,
                brokerName = "FTMO Server 2",
                slippagePips = 0.2,
                durationText = "35m"
            ),
            Trade(
                id = 4,
                pair = "BTC/USDT",
                direction = TradeDirection.LONG,
                setupStrategy = "Liquidity Sweep",
                session = "London / NY Overlap",
                timeframe = "1H",
                entryPrice = 68850.0,
                stopLoss = 68100.0,
                takeProfit = 71300.0,
                exitPrice = 71300.0,
                positionSizeLots = 2.0,
                riskRewardRatio = "1:3.27",
                rMultiple = 2.1,
                netGainDollars = 4900.00,
                riskPercent = 1.0,
                maxRiskDollars = 1500.0,
                visibility = TradeVisibility.PRIVATE,
                status = com.example.model.TradeStatus.WINNER,
                timestamp = now - 24 * 3600 * 1000,
                timeAgo = "Yesterday, 19:15 · 1H Timeframe",
                authorName = "Alex Vance",
                authorHandle = "@QuantAlex",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = "Pre-CPI, tight risk, mental stop respected without flinching. Trailed stops right into previous daily highs before consolidation.",
                psychologyNote = "Pre-CPI, tight risk, mental stop respected without flinching. Trailed stops right into previous daily highs before consolidation.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("MacroRelease", "Discipline", "Broker Executed"),
                chartDrawableRes = R.drawable.img_btcusdt_chart,
                chartOverlayText = "Broker Executed • Trailed Stop",
                tapeSpeed = "+1.0x",
                upvotes = 88,
                downvotes = 0,
                commentsCount = 12,
                isUpvoted = false,
                isBookmarked = false,
                brokerName = "IC Markets (cTrader Raw)",
                slippagePips = 0.15,
                durationText = "5h 20m"
            ),
            Trade(
                id = 5,
                pair = "GBP/JPY",
                direction = TradeDirection.SHORT,
                setupStrategy = "Breakout Fail",
                session = "London Session",
                timeframe = "15M Scalp",
                entryPrice = 192.420,
                stopLoss = 192.680,
                takeProfit = 191.500,
                exitPrice = 192.680,
                positionSizeLots = 1.50,
                riskRewardRatio = "1:3.5",
                rMultiple = -1.0,
                netGainDollars = -640.00,
                riskPercent = 1.0,
                maxRiskDollars = 640.0,
                visibility = TradeVisibility.PRIVATE,
                status = com.example.model.TradeStatus.STOPPED,
                timestamp = now - 48 * 3600 * 1000,
                timeAgo = "2 days ago · 15M Scalp",
                authorName = "Alex Vance",
                authorHandle = "@QuantAlex",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = "Breakout failure at key 192.50 round number level.",
                psychologyNote = "Cut immediately when HTF 4H candle failed to close below support. Good loss execution. No revenge entry triggered.",
                postMortemReflection = "Cut immediately when HTF 4H candle failed to close below support. Good loss execution. No revenge entry triggered.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("Scalp", "LossReview", "PlanFollowed"),
                chartDrawableRes = null,
                chartOverlayText = "Stop Triggered Cleanly",
                tapeSpeed = null,
                upvotes = 45,
                downvotes = 1,
                commentsCount = 8,
                isUpvoted = false,
                isBookmarked = false,
                brokerName = "IC Markets (cTrader Raw)",
                slippagePips = 0.2,
                durationText = "48m"
            ),
            Trade(
                id = 6,
                pair = "XAU/USD",
                direction = TradeDirection.LONG,
                setupStrategy = "Asia Liquidity",
                session = "Asian Session",
                timeframe = "30M Timeframe",
                entryPrice = 2714.20,
                stopLoss = 2708.00,
                takeProfit = 2741.80,
                exitPrice = 2741.80,
                positionSizeLots = 1.25,
                riskRewardRatio = "1:4.45",
                rMultiple = 4.5,
                netGainDollars = 3825.00,
                riskPercent = 0.8,
                maxRiskDollars = 850.0,
                visibility = TradeVisibility.PUBLIC,
                status = com.example.model.TradeStatus.WINNER,
                timestamp = now - 72 * 3600 * 1000,
                timeAgo = "3 days ago · 30M Timeframe",
                authorName = "Alex Vance",
                authorHandle = "@QuantAlex",
                authorTier = com.example.model.TraderTier.PROP,
                isAuthorVerified = true,
                executionThesis = "Asia low taken with precision followed by aggressive institutional imbalance entering prior to London open.",
                psychologyNote = "Patience paid off. Followed all rules without moving stops.",
                planAdherencePercent = 100,
                disciplineScore = 5.0,
                tags = listOf("Gold", "AsiaSweep", "HighRR"),
                chartDrawableRes = R.drawable.img_eurusd_chart,
                chartOverlayText = "Asia Liquidity Reclaimed",
                tapeSpeed = "+1.5x",
                upvotes = 198,
                downvotes = 4,
                commentsCount = 45,
                isUpvoted = false,
                isBookmarked = false,
                brokerName = "IC Markets (cTrader Raw)",
                slippagePips = 0.1,
                durationText = "3h 15m"
            )
        )
    }
}
