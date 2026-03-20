package com.example.myapplication.data.analytics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollStatsDao {

    /* ---------------- LIVE OBSERVATION ---------------- */
    @Query("SELECT * FROM scroll_daily_stats WHERE date = :date")
    fun observeStatsForDate(date: String): Flow<ScrollDailyStats?>

    /* ---------------- SINGLE FETCH ---------------- */
    @Query("SELECT * FROM scroll_daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): ScrollDailyStats?

    /* ---------------- INSERT ---------------- */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: ScrollDailyStats)

    /* ---------------- INCREMENT QUERIES (NEW & SAFE) ---------------- */
    @Query("UPDATE scroll_daily_stats SET reelsViewed = reelsViewed + :value WHERE date = :date")
    suspend fun incrementReelsViewed(date: String, value: Int = 1): Int

    @Query("UPDATE scroll_daily_stats SET deepScrollCount = deepScrollCount + :value WHERE date = :date")
    suspend fun incrementDeepScrollCount(date: String, value: Int = 1): Int

    @Query("UPDATE scroll_daily_stats SET usageMinutes = usageMinutes + :value WHERE date = :date")
    suspend fun addUsageMinutes(date: String, value: Int): Int

    @Query("UPDATE scroll_daily_stats SET sessions = sessions + :value WHERE date = :date")
    suspend fun incrementSessions(date: String, value: Int = 1): Int

    @Query("UPDATE scroll_daily_stats SET isSynced = 0 WHERE date = :date")
    suspend fun markAsDirty(date: String)

    /* ---------------- ANALYTICS QUERIES ---------------- */
    @Query("SELECT * FROM scroll_daily_stats ORDER BY date DESC")
    suspend fun getAllStats(): List<ScrollDailyStats>

    @Query("SELECT * FROM scroll_daily_stats WHERE date >= :startDate")
    suspend fun getStatsFrom(startDate: String): List<ScrollDailyStats>

    @Query("SELECT * FROM scroll_daily_stats WHERE isSynced = 0")
    suspend fun getUnsyncedStats(): List<ScrollDailyStats>

    @Query("UPDATE scroll_daily_stats SET isSynced = 1 WHERE date IN (:dates)")
    suspend fun markSynced(dates: List<String>)
}