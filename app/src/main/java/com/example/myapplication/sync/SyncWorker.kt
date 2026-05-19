package com.example.myapplication.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.app.AppContainer
import kotlinx.coroutines.flow.first

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        val childId = container.registrationRepository.childIdFlow.first()
        val deviceId = container.registrationRepository.deviceIdFlow.first() ?: return Result.failure()

        if (childId.isNullOrBlank()) return Result.retry()

        return if (container.syncRepository.syncPending(deviceId)) Result.success() else Result.retry()
    }
}
