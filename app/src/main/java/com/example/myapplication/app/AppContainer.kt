package com.example.myapplication.app

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.LocalStatsDataSource
import com.example.myapplication.data.remote.DeepScrollApi
import com.example.myapplication.data.remote.NetworkModule
import com.example.myapplication.data.repository.RegistrationRepository
import com.example.myapplication.data.repository.SyncRepository
import com.example.myapplication.domain.UsageTracker
import com.example.myapplication.notifications.DeepScrollNotifier
import com.example.myapplication.sync.ConnectivityObserver

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "deepscroll.db").build()
    private val api: DeepScrollApi = NetworkModule.createApi()

    val localStatsDataSource = LocalStatsDataSource(database.statsDao())
    val syncRepository = SyncRepository(api, database.syncQueueDao(), database.statsDao())
    val registrationRepository = RegistrationRepository(api, appContext)
    val usageTracker = UsageTracker(appContext, localStatsDataSource)
    val notifier = DeepScrollNotifier(appContext)
    val connectivityObserver = ConnectivityObserver(appContext)
}
