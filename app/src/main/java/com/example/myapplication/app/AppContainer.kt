package com.example.myapplication.app

import android.content.Context
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.RegistrationRepository
import com.example.myapplication.data.repository.StatsRepository
import com.example.myapplication.data.repository.SyncManager
import com.example.myapplication.data.repository.SyncRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val deepScrollApi = RetrofitClient.deepScrollApi

    val registrationRepository = RegistrationRepository(
        apiService = deepScrollApi,
        context = appContext
    )
    
    val statsRepository = StatsRepository(
        statsDao = database.statsDao(),
        apiService = deepScrollApi,
        registrationRepository = registrationRepository
    )
    
    val syncRepository = SyncRepository(
        api = deepScrollApi,
        queueDao = database.syncQueueDao(),
        statsDao = database.statsDao()
    )

    val syncManager = SyncManager(appContext)
}
