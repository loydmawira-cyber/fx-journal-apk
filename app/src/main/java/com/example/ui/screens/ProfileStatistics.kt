package com.example.ui.screens

import com.example.model.Trade
import com.example.model.TradeStatus

/** Journal-derived profile metrics. Community-feed posts are intentionally not included. */
data class ProfileStatistics(
    val totalTrades: Int,
    val openTrades: Int,
    val wins: Int,
    val losses: Int,
    val breakevens: Int,
    val cancelled: Int,
    val completedTrades: Int,
    /** Wins / (wins + losses); breakeven, open, and cancelled trades are excluded. */
    val winRatePercent: Double?,
    /** Sum of net P&L and R-multiples for completed trades only. */
    val netProfitDollars: Double,
    val netR: Double,
    val averageR: Double?
)

fun calculateProfileStatistics(trades: List<Trade>): ProfileStatistics {
    val wins = trades.count { it.status == TradeStatus.WINNER }
    val losses = trades.count { it.status == TradeStatus.STOPPED }
    val breakevens = trades.count { it.status == TradeStatus.BREAKEVEN }
    val openTrades = trades.count { it.status == TradeStatus.OPEN }
    val cancelled = trades.count { it.status == TradeStatus.CANCELLED }
    val completed = trades.filter {
        it.status == TradeStatus.WINNER ||
            it.status == TradeStatus.STOPPED ||
            it.status == TradeStatus.BREAKEVEN
    }
    val decisiveTrades = wins + losses

    return ProfileStatistics(
        totalTrades = trades.size,
        openTrades = openTrades,
        wins = wins,
        losses = losses,
        breakevens = breakevens,
        cancelled = cancelled,
        completedTrades = completed.size,
        winRatePercent = if (decisiveTrades == 0) null else wins * 100.0 / decisiveTrades,
        netProfitDollars = completed.sumOf { it.netGainDollars },
        netR = completed.sumOf { it.rMultiple },
        averageR = completed.takeIf { it.isNotEmpty() }?.map { it.rMultiple }?.average()
    )
}
