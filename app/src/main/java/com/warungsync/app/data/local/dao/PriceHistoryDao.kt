package com.warungsync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.warungsync.app.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {

    @Query("SELECT * FROM price_history WHERE tokoId = :tokoId AND itemId = :itemId ORDER BY changedAt DESC")
    fun getHistoryForItem(tokoId: String, itemId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE tokoId = :tokoId AND itemId = :itemId ORDER BY changedAt DESC")
    suspend fun getHistoryListForItem(tokoId: String, itemId: String): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history WHERE tokoId = :tokoId AND changedAt > :since")
    suspend fun getHistoriesModifiedSince(tokoId: String, since: Long): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history WHERE changedAt > :since")
    suspend fun getAllHistoriesModifiedSinceGlobal(since: Long): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<PriceHistoryEntity>)
}
