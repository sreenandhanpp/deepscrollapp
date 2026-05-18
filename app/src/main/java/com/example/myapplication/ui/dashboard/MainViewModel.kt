package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.DailyStatsEntity
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.RegistrationRepository
import com.example.myapplication.data.repository.StatsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.apiService
    private val registrationRepository = RegistrationRepository(application, apiService)
    private val statsRepository = StatsRepository(database.statsDao(), apiService, registrationRepository)

    /* ---------------- Stats ---------------- */
    
    val allStats: StateFlow<List<DailyStatsEntity>> = statsRepository.getAllStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStats: StateFlow<DailyStatsEntity?> = allStats.map { list ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        list.find { it.date == today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /* ---------------- Registration ---------------- */

    val childId: StateFlow<String?> = registrationRepository.childIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /* ---------------- Notification timing preference ---------------- */

    val notifyAfterReels =
        NotificationSettingsStore
            .notifyAfterReelsFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 50
            )

    fun updateNotifyAfterReels(reels: Int) {
        viewModelScope.launch {
            NotificationSettingsStore.setNotifyAfterReels(getApplication(), reels)
        }
    }

    /* =========================================================
       🔔 Notification Settings logic
       ========================================================= */
    private val _showNotificationSettings = MutableStateFlow(false)
    val showNotificationSettings: StateFlow<Boolean> = _showNotificationSettings.asStateFlow()

    fun openNotificationSettings() { _showNotificationSettings.value = true }
    fun closeNotificationSettings() { _showNotificationSettings.value = false }

    /* =========================================================
       🎛 Notification Toggles
       ========================================================= */
    // These should ideally be in a SettingsRepository/Store
    private val _timeReminderEnabled = MutableStateFlow(true)
    val timeReminderEnabled = _timeReminderEnabled.asStateFlow()
    fun setTimeReminderEnabled(value: Boolean) { _timeReminderEnabled.value = value }

    private val _rapidSwipeEnabled = MutableStateFlow(true)
    val rapidSwipeEnabled = _rapidSwipeEnabled.asStateFlow()
    fun setRapidSwipeEnabled(value: Boolean) { _rapidSwipeEnabled.value = value }

    private val _zoneOutEnabled = MutableStateFlow(true)
    val zoneOutEnabled = _zoneOutEnabled.asStateFlow()
    fun setZoneOutEnabled(value: Boolean) { _zoneOutEnabled.value = value }

    private val _roboticEnabled = MutableStateFlow(true)
    val roboticEnabled = _roboticEnabled.asStateFlow()
    fun setRoboticEnabled(value: Boolean) { _roboticEnabled.value = value }

    private val _deepDiveEnabled = MutableStateFlow(true)
    val deepDiveEnabled = _deepDiveEnabled.asStateFlow()
    fun setDeepDiveEnabled(value: Boolean) { _deepDiveEnabled.value = value }

    private val _mindlessEnabled = MutableStateFlow(true)
    val mindlessEnabled = _mindlessEnabled.asStateFlow()
    fun setMindlessEnabled(value: Boolean) { _mindlessEnabled.value = value }
    
    // Onboarding logic (simplified for brevity)
    val onboardingCompleted = OnboardingStore.onboardingCompletedFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun completeOnboarding() {
        viewModelScope.launch { OnboardingStore.setOnboardingCompleted(getApplication()) }
    }
}
