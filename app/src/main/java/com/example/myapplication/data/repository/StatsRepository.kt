package com.example.myapplication.data.repository

import com.example.myapplication.data.local.StatsDao
import com.example.myapplication.data.local.DailyStatEntity
import com.example.myapplication.data.remote.DeepScrollApi
import com.example.myapplication.data.remote.SyncRequest
import com.example.myapplication.data.remote.SyncStatDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsRepository(
    private val statsDao: StatsDao,
    private val apiService: DeepScrollApi,
    private val registrationRepository: RegistrationRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getTodayDate(): String = dateFormat.format(Date())

    fun getAllStats(): Flow<List<DailyStatEntity>> = statsDao.observeAll()

    suspend fun incrementReels() {
        val today = getTodayDate()
        ensureStatsForDate(today)
        statsDao.incrementReels(today)
    }

    suspend fun incrementDeepScroll() {
        val today = getTodayDate()
        ensureStatsForDate(today)
        statsDao.incrementDeepScroll(today)
    }

    suspend fun addUsageMinutes(minutes: Int) {
        val today = getTodayDate()
        ensureStatsForDate(today)
        statsDao.addMinutes(today, minutes)
    }

    suspend fun incrementSessions() {
        val today = getTodayDate()
        ensureStatsForDate(today)
        statsDao.incrementSessions(today)
    }

    private suspend fun ensureStatsForDate(date: String) {
        if (statsDao.getByDate(date) == null) {
            statsDao.upsert(DailyStatEntity(date = date))
        }
    }

    suspend fun syncWithBackend(): Boolean {
        return try {
            val deviceId = registrationRepository.deviceIdFlow.first() ?: return false
            val allStats = statsDao.getAllStatsSync()
            
            val statsList = allStats.map {
                SyncStatDto(
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
            false
        }
    }
}
