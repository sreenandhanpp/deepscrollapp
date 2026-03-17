package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.analytics.UsageRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        return try {

            val repository = UsageRepository(applicationContext)

            val unsyncedStats = repository.getUnsyncedStats()

            if (unsyncedStats.isEmpty()) {
                return Result.success()
            }

            val deviceId = DeviceIdManager.getDeviceId(applicationContext)

            val dtoList = repository.mapToDto(unsyncedStats)

            val request = SyncRequest(
                deviceId = deviceId,
                stats = dtoList
            )

            val response = ApiClient.syncApi.syncStats(request)

            if (response.isSuccessful) {

                repository.markSynced(unsyncedStats)

                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            Result.retry()
        }
    }
}