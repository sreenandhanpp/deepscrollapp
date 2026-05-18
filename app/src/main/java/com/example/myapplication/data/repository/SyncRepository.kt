package com.example.myapplication.data.repository

import com.example.myapplication.data.local.StatsDao
import com.example.myapplication.data.local.SyncQueueDao
import com.example.myapplication.data.remote.DeepScrollApi
import com.example.myapplication.data.remote.SyncRequest
import com.example.myapplication.data.remote.SyncStatDto

class SyncRepository(
    private val api: DeepScrollApi,
    private val queueDao: SyncQueueDao,
    private val statsDao: StatsDao
) {
    suspend fun syncPending(deviceId: String): Boolean {
        val pending = queueDao.nextBatch(limit = 30)
        if (pending.isEmpty()) return true
        val stats = pending.mapNotNull { item ->
            statsDao.getByDate(item.date)?.let {
                SyncStatDto(it.date, it.reelsViewed, it.deepScrollCount, it.usageMinutes, it.sessions)
            }
        }
        if (stats.isEmpty()) return true
        val response = api.sync(SyncRequest(deviceId, stats))
        return if (response.isSuccessful) {
            queueDao.deleteByIds(pending.map { it.id })
            true
        } else {
            queueDao.incrementRetry(pending.map { it.id })
            false
        }
    }
}
