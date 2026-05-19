package com.example.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stats")
    suspend fun getAllStatsSync(): List<DailyStatEntity>

    @Query("UPDATE daily_stats SET reelsViewed = reelsViewed + 1 WHERE date = :date")
    suspend fun incrementReels(date: String)

    @Query("UPDATE daily_stats SET deepScrollCount = deepScrollCount + 1 WHERE date = :date")
    suspend fun incrementDeepScroll(date: String)

    @Query("UPDATE daily_stats SET usageMinutes = usageMinutes + :minutes WHERE date = :date")
    suspend fun addMinutes(date: String, minutes: Int)

    @Query("UPDATE daily_stats SET sessions = sessions + 1 WHERE date = :date")
    suspend fun incrementSessions(date: String)
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entity: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY enqueuedAt ASC LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id IN (:ids)")
    suspend fun incrementRetry(ids: List<Long>)
}
