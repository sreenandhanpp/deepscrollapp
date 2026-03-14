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

    /* ---------------- INSERT / UPDATE ---------------- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: ScrollDailyStats)

    /* ---------------- ANALYTICS QUERIES ---------------- */

    @Query("SELECT * FROM scroll_daily_stats ORDER BY date DESC")
    suspend fun getAllStats(): List<ScrollDailyStats>

    @Query("SELECT * FROM scroll_daily_stats WHERE date >= :startDate")
    suspend fun getStatsFrom(startDate: String): List<ScrollDailyStats>
}