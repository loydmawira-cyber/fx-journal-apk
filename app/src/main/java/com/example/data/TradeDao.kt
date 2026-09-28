package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE id = :id LIMIT 1")
    fun getTradeById(id: Long): Flow<TradeEntity?>

    @Query("SELECT * FROM trades WHERE visibility = 'PUBLIC' ORDER BY timestamp DESC")
    fun getPublicTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE visibility = 'PRIVATE' ORDER BY timestamp DESC")
    fun getPrivateTrades(): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trades: List<TradeEntity>)

    @Query("DELETE FROM trades")
    suspend fun deleteAll()

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Query("UPDATE trades SET upvotes = upvotes + :delta, isUpvoted = :isUpvoted WHERE id = :id")
    suspend fun updateUpvote(id: Long, delta: Int, isUpvoted: Boolean)

    @Query("UPDATE trades SET downvotes = downvotes + :delta, isDownvoted = :isDownvoted WHERE id = :id")
    suspend fun updateDownvote(id: Long, delta: Int, isDownvoted: Boolean)

    @Query("UPDATE trades SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteTradeById(id: Long)
}
