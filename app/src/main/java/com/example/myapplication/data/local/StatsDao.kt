package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAllStatsSync(): List<DailyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStatsEntity)

    @Query("UPDATE daily_stats SET reelsViewed = reelsViewed + 1 WHERE date = :date")
    suspend fun incrementReels(date: String)

    @Query("UPDATE daily_stats SET deepScrollCount = deepScrollCount + 1 WHERE date = :date")
    suspend fun incrementDeepScroll(date: String)
    
    @Query("UPDATE daily_stats SET usageMinutes = usageMinutes + :minutes WHERE date = :date")
    suspend fun addMinutes(date: String, minutes: Int)

    @Query("UPDATE daily_stats SET sessions = sessions + 1 WHERE date = :date")
    suspend fun incrementSessions(date: String)
}
