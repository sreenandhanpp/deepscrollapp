package com.example.myapplication.data.analytics

import android.content.Context
import com.example.myapplication.data.sync.StatDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class UsageRepository(context: Context) {

    private val dao = ScrollDatabase
        .getDatabase(context)
        .statsDao()

    /* ---------------- DATE HELPER ---------------- */

    private fun today(): String {
        return LocalDate.now().toString()
    }

    /* ---------------- CURRENT MONTH ---------------- */

    suspend fun getCurrentMonthStats(): List<ScrollDailyStats> {

        val startDate = LocalDate.now()
            .withDayOfMonth(1)
            .toString()

        return withContext(Dispatchers.IO) {
            dao.getStatsFrom(startDate)
        }
    }

    /* ---------------- OBSERVE TODAY ---------------- */

    fun observeTodayStats(): Flow<ScrollDailyStats?> {
        return dao.observeStatsForDate(today())
    }

    /* ---------------- GET TODAY ---------------- */

    suspend fun getTodayStats(): ScrollDailyStats {
        return withContext(Dispatchers.IO) {
            dao.getStatsForDate(today())
                ?: ScrollDailyStats(date = today())
        }
    }

    /* ---------------- INCREMENT REELS ---------------- */

    suspend fun incrementReelsViewed() {

        withContext(Dispatchers.IO) {

            val date = today()
            val stats = dao.getStatsForDate(date)

            if (stats == null) {

                dao.insert(
                    ScrollDailyStats(
                        date = date,
                        reelsViewed = 1,
                        deepScrollCount = 0,
                        usageMinutes = 0,
                        sessions = 0,
                        isSynced = false
                    )
                )

            } else {

                dao.insert(
                    stats.copy(
                        reelsViewed = stats.reelsViewed + 1,
                        isSynced = false
                    )
                )
            }
        }
    }

    /* ---------------- INCREMENT DEEP SCROLL ---------------- */

    suspend fun incrementDeepScroll() {

        withContext(Dispatchers.IO) {

            val date = today()
            val stats = dao.getStatsForDate(date)

            if (stats == null) {

                dao.insert(
                    ScrollDailyStats(
                        date = date,
                        reelsViewed = 0,
                        deepScrollCount = 1,
                        usageMinutes = 0,
                        sessions = 0,
                        isSynced = false
                    )
                )

            } else {

                dao.insert(
                    stats.copy(
                        deepScrollCount = stats.deepScrollCount + 1,
                        isSynced = false
                    )
                )
            }
        }
    }

    /* ---------------- HEATMAP DATA (365 DAYS) ---------------- */

    suspend fun getLastYearStats(): List<ScrollDailyStats> {

        val startDate = LocalDate
            .now()
            .minusDays(365)
            .toString()

        return withContext(Dispatchers.IO) {
            dao.getStatsFrom(startDate)
        }
    }

    /* ---------------- UNSYNCED DATA ---------------- */

    suspend fun getUnsyncedStats(): List<ScrollDailyStats> {
        return withContext(Dispatchers.IO) {
            dao.getUnsyncedStats()
        }
    }

    /* ---------------- MARK AS SYNCED ---------------- */

    suspend fun markSynced(stats: List<ScrollDailyStats>) {

        val dates = stats.map { it.date }

        withContext(Dispatchers.IO) {
            dao.markSynced(dates)
        }
    }

    /* ---------------- DTO MAPPER ---------------- */

    fun mapToDto(stats: List<ScrollDailyStats>): List<StatDto> {
        return stats.map {
            StatDto(
                date = it.date,
                reelsViewed = it.reelsViewed,
                deepScrollCount = it.deepScrollCount,
                usageMinutes = it.usageMinutes,
                sessions = it.sessions
            )
        }
    }

    suspend fun addSessionTime(durationMs: Long) {

        val minutes = (durationMs / 60_000L).toInt().coerceAtLeast(1)
        val date = LocalDate.now().toString()
        val stats = dao.getStatsForDate(date)

        if (stats == null) {
            dao.insert(
                ScrollDailyStats(
                    date = date,
                    usageMinutes = minutes,
                    isSynced = false
                )
            )
        } else {
            dao.insert(
                stats.copy(
                    usageMinutes = stats.usageMinutes + minutes,
                    isSynced = false
                )
            )
        }
    }
}