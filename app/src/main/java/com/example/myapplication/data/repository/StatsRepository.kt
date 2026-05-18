package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.DailyStatEntity
import com.example.myapplication.data.local.StatsDao
import com.example.myapplication.data.remote.ApiService
import com.example.myapplication.data.remote.dto.StatDto
import com.example.myapplication.data.remote.dto.SyncRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsRepository(
    private val statsDao: StatsDao,
    private val apiService: ApiService,
    private val registrationRepository: RegistrationRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TAG = "StatsRepository"

    private fun getTodayDate(): String = dateFormat.format(Date())

    fun getAllStats(): Flow<List<DailyStatEntity>> = statsDao.observeAll()

    suspend fun incrementReels() {
        val today = getTodayDate()
        Log.d(TAG, "incrementReels called for date: $today")
        ensureStatsForDate(today)
        statsDao.incrementReels(today)

        // Verify update
        val updated = statsDao.getByDate(today)
        Log.d(TAG, "After increment - reels: ${updated?.reelsViewed}")
    }

    suspend fun incrementDeepScroll() {
        val today = getTodayDate()
        Log.d(TAG, "incrementDeepScroll called for date: $today")
        ensureStatsForDate(today)
        statsDao.incrementDeepScroll(today)

        val updated = statsDao.getByDate(today)
        Log.d(TAG, "After increment - deepScroll: ${updated?.deepScrollCount}")
    }

    suspend fun addUsageMinutes(minutes: Int) {
        val today = getTodayDate()
        Log.d(TAG, "addUsageMinutes called for date: $today, minutes: $minutes")
        ensureStatsForDate(today)
        statsDao.addMinutes(today, minutes)

        val updated = statsDao.getByDate(today)
        Log.d(TAG, "After add - usageMinutes: ${updated?.usageMinutes}")
    }

    suspend fun incrementSessions() {
        val today = getTodayDate()
        Log.d(TAG, "incrementSessions called for date: $today")
        ensureStatsForDate(today)
        statsDao.incrementSessions(today)

        val updated = statsDao.getByDate(today)
        Log.d(TAG, "After increment - sessions: ${updated?.sessions}")
    }

    private suspend fun ensureStatsForDate(date: String) {
        val existing = statsDao.getByDate(date)
        if (existing == null) {
            Log.d(TAG, "Creating new stats entry for date: $date")
            statsDao.upsert(DailyStatEntity(date = date))
        }
    }

    suspend fun getTodayStats(): DailyStatEntity? {
        val today = getTodayDate()
        return statsDao.getByDate(today)
    }

    suspend fun syncWithBackend(): Boolean {
        return try {
            val deviceId = registrationRepository.deviceIdFlow.first() ?: return false
            val allStats = statsDao.getAllStatsSync()

            val statsList = allStats.map {
                StatDto(
                    date = it.date,
                    reelsViewed = it.reelsViewed,
                    deepScrollCount = it.deepScrollCount,
                    usageMinutes = it.usageMinutes,
                    sessions = it.sessions
                )
            }

            val response = apiService.sync(SyncRequest(deviceId, statsList))
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            false
        }
    }
}