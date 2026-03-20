package com.example.myapplication.data.analytics

import android.content.Context
import android.util.Log
import com.example.myapplication.data.sync.StatDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class UsageRepository(context: Context) {

    private val dao = ScrollDatabase.getDatabase(context).statsDao()

    private fun today(): String = LocalDate.now().toString()

    /* ---------------- OBSERVE / GET ---------------- */
    fun observeTodayStats(): Flow<ScrollDailyStats?> = dao.observeStatsForDate(today())

    suspend fun getTodayStats(): ScrollDailyStats = withContext(Dispatchers.IO) {
        dao.getStatsForDate(today()) ?: ScrollDailyStats(date = today())
    }

    suspend fun getCurrentMonthStats(): List<ScrollDailyStats> = withContext(Dispatchers.IO) {
        val start = LocalDate.now().withDayOfMonth(1).toString()
        dao.getStatsFrom(start)
    }

    suspend fun getLastYearStats(): List<ScrollDailyStats> = withContext(Dispatchers.IO) {
        val start = LocalDate.now().minusDays(365).toString()
        dao.getStatsFrom(start)
    }

    /* ---------------- INCREMENTS (NOW SAFE) ---------------- */
    suspend fun incrementReelsViewed() {
        val date = today()
        withContext(Dispatchers.IO) {
            if (dao.getStatsForDate(date) == null) {
                dao.insert(ScrollDailyStats(date = date, reelsViewed = 1, isSynced = false))
            } else {
                dao.incrementReelsViewed(date)
                dao.markAsDirty(date)
            }
            Log.d("DB_WRITE", "Reels incremented for $date")
        }
    }

    suspend fun incrementDeepScroll() {
        val date = today()
        withContext(Dispatchers.IO) {
            if (dao.getStatsForDate(date) == null) {
                dao.insert(ScrollDailyStats(date = date, deepScrollCount = 1, isSynced = false))
            } else {
                dao.incrementDeepScrollCount(date)
                dao.markAsDirty(date)
            }
            Log.d("DB_WRITE", "Deep scroll incremented for $date")
        }
    }

    suspend fun addSessionTime(durationMs: Long) {
        val minutes = (durationMs / 60_000L).toInt().coerceAtLeast(1)
        val date = today()
        withContext(Dispatchers.IO) {
            if (dao.getStatsForDate(date) == null) {
                dao.insert(ScrollDailyStats(date = date, usageMinutes = minutes, isSynced = false))
            } else {
                dao.addUsageMinutes(date, minutes)
                dao.markAsDirty(date)
            }
            Log.d("DB_WRITE", "Added $minutes min for $date")
        }
    }

    suspend fun incrementSession() {
        val date = today()
        withContext(Dispatchers.IO) {
            if (dao.getStatsForDate(date) == null) {
                dao.insert(ScrollDailyStats(date = date, sessions = 1, isSynced = false))
            } else {
                dao.incrementSessions(date)
                dao.markAsDirty(date)
            }
            Log.d("DB_WRITE", "Session count incremented for $date")
        }
    }

    /* ---------------- SYNC ---------------- */
    suspend fun getUnsyncedStats(): List<ScrollDailyStats> = withContext(Dispatchers.IO) {
        dao.getUnsyncedStats()
    }

    suspend fun markSynced(stats: List<ScrollDailyStats>) {
        val dates = stats.map { it.date }
        withContext(Dispatchers.IO) {
            dao.markSynced(dates)
        }
    }

    fun mapToDto(stats: List<ScrollDailyStats>): List<StatDto> = stats.map {
        StatDto(
            date = it.date,
            reelsViewed = it.reelsViewed,
            deepScrollCount = it.deepScrollCount,
            usageMinutes = it.usageMinutes,
            sessions = it.sessions
        )
    }
}