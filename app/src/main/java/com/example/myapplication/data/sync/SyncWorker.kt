package com.example.myapplication.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.analytics.UsageRepository
import retrofit2.HttpException

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        Log.d("SyncWorker", "Worker started")

        return try {

            val repository = UsageRepository(applicationContext)

            val unsyncedStats = repository.getUnsyncedStats()

            Log.d("SyncWorker", "Unsynced count: ${unsyncedStats.size}")

            if (unsyncedStats.isEmpty()) {
                return Result.success()
            }

            val deviceId = DeviceIdManager.getDeviceId(applicationContext)

            val dtoList = repository.mapToDto(unsyncedStats)

            val request = SyncRequest(
                deviceId = deviceId,
                stats = dtoList
            )

            Log.d("SyncWorker", "Sending request: $request")

            val response = ApiClient.syncApi.syncStats(request)

            Log.d("SyncWorker", "Response: ${response.code()}")

            if (response.isSuccessful) {

                repository.markSynced(unsyncedStats)

                Log.d("SyncWorker", "Sync success")

                Result.success()

            } else {

                Log.e("SyncWorker", "Sync failed")

                Result.retry()
            }

        } catch (e: HttpException) {
            Log.e("SyncWorker", "HTTP error", e)
            Result.retry()

        } catch (e: Exception) {
            Log.e("SyncWorker", "Error", e)
            Result.retry()
        }
    }
}