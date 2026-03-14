package com.example.myapplication.data.analytics

import android.content.Context
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

    suspend fun getCurrentMonthStats(): List<ScrollDailyStats> {

        val startDate = LocalDate.now()
            .withDayOfMonth(1)
            .toString()

        return withContext(Dispatchers.IO) {
            dao.getStatsFrom(startDate)
        }
    }
    /* ---------------- OBSERVE TODAY STATS ---------------- */

    fun observeTodayStats(): Flow<ScrollDailyStats?> {
        return dao.observeStatsForDate(today())
    }

    /* ---------------- GET TODAY STATS ---------------- */

    suspend fun getTodayStats(): ScrollDailyStats? {
        return withContext(Dispatchers.IO) {
            dao.getStatsForDate(today())
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
                        deepScrollCount = 0
                    )
                )

            } else {

                dao.insert(
                    stats.copy(
                        reelsViewed = stats.reelsViewed + 1
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
                        deepScrollCount = 1
                    )
                )

            } else {

                dao.insert(
                    stats.copy(
                        deepScrollCount = stats.deepScrollCount + 1
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
}