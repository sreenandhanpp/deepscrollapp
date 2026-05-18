package com.example.myapplication.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.app.AppContainer
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.local.DailyStatEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = AppContainer(app)
    private val context = app.applicationContext
    private val TAG = "MainViewModel"

    // 🔔 Notification Settings State
    val timeReminderEnabled = NotificationSettingsStore.timeReminderEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rapidSwipeEnabled = NotificationSettingsStore.rapidSwipeEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val zoneOutEnabled = NotificationSettingsStore.zoneOutEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val roboticEnabled = NotificationSettingsStore.roboticEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val deepDiveEnabled = NotificationSettingsStore.deepDiveEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val mindlessEnabled = NotificationSettingsStore.mindlessEnabled(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifyAfterReels = NotificationSettingsStore.notifyAfterReelsFlow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    // ⚙️ Actions
    fun setNotifyAfterReels(reels: Int) = viewModelScope.launch {
        NotificationSettingsStore.setNotifyAfterReels(context, reels)
    }

    fun setTimeReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setTimeReminderEnabled(context, enabled)
    }

    fun setRapidSwipeEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setRapidSwipeEnabled(context, enabled)
    }

    fun setZoneOutEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setZoneOutEnabled(context, enabled)
    }

    fun setRoboticEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setRoboticEnabled(context, enabled)
    }

    fun setDeepDiveEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setDeepDiveEnabled(context, enabled)
    }

    fun setMindlessEnabled(enabled: Boolean) = viewModelScope.launch {
        NotificationSettingsStore.setMindlessEnabled(context, enabled)
    }

    val childId: StateFlow<String> = container.registrationRepository.childIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Get all stats
    val stats: StateFlow<List<DailyStatEntity>> = container.localStatsDataSource.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Get today's stats - FIXED with proper date format
    val today = stats.map { statsList ->
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        Log.d(TAG, "Looking for stats for date: $todayDate")
        Log.d(TAG, "Available stats: ${statsList.map { it.date }}")

        val todayStats = statsList.find { it.date == todayDate }

        if (todayStats == null) {
            Log.d(TAG, "No stats found for today, creating default")
            DailyStatEntity(
                date = todayDate,
                usageMinutes = 0,
                reelsViewed = 0,
                deepScrollCount = 0,
                sessions = 0,
                rapidScrollCount = 0,
                mindlessMinutes = 0f
            )
        } else {
            Log.d(TAG, "Found stats: ${todayStats.reelsViewed} reels, ${todayStats.usageMinutes} mins")
            todayStats
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyStatEntity(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            usageMinutes = 0,
            reelsViewed = 0,
            deepScrollCount = 0,
            sessions = 0,
            rapidScrollCount = 0,
            mindlessMinutes = 0f
        )
    )

    fun registerIfNeeded(label: String = "My Kid") = viewModelScope.launch {
        container.registrationRepository.ensureRegistered(label)
    }

    // Debug function to check stats
    fun debugPrintStats() = viewModelScope.launch {
        val allStats = stats.value
        Log.d(TAG, "=== CURRENT STATS ===")
        allStats.forEach { stat ->
            Log.d(TAG, "Date: ${stat.date}, Reels: ${stat.reelsViewed}, DeepScroll: ${stat.deepScrollCount}, Minutes: ${stat.usageMinutes}, Sessions: ${stat.sessions}")
        }
    }
}