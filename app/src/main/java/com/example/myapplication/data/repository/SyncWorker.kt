package com.example.myapplication.data.repository

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.remote.RetrofitClient

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = RetrofitClient.apiService
        val deepScrollApi = RetrofitClient.deepScrollApi
        val registrationRepository = RegistrationRepository(deepScrollApi, applicationContext)
        val statsRepository = StatsRepository(database.statsDao(), apiService, registrationRepository)

        val success = statsRepository.syncWithBackend()
        return if (success) Result.success() else Result.retry()
    }
}
