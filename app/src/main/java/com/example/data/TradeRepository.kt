package com.example.data

import com.example.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TradeRepository(private val tradeDao: TradeDao) {

    val allTrades: Flow<List<Trade>> = tradeDao.getAllTrades().map { list -> list.map { it.toDomain() } }
    val publicTrades: Flow<List<Trade>> = tradeDao.getPublicTrades().map { list -> list.map { it.toDomain() } }
    val privateTrades: Flow<List<Trade>> = tradeDao.getPrivateTrades().map { list -> list.map { it.toDomain() } }

    fun getTradeById(id: Long): Flow<Trade?> = tradeDao.getTradeById(id).map { it?.toDomain() }

    suspend fun insertTrade(trade: Trade): Long = tradeDao.insertTrade(TradeEntity.fromDomain(trade))

    suspend fun clearAll() = tradeDao.deleteAll()

    suspend fun updateTrade(trade: Trade) = tradeDao.updateTrade(TradeEntity.fromDomain(trade))

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
}
