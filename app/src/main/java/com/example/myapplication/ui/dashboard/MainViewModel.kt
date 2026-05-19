package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.app.AppContainer
import com.example.myapplication.data.NotificationSettingsStore
import com.example.myapplication.data.local.DailyStatEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = AppContainer(application)
    private val statsRepository = container.statsRepository
    private val registrationRepository = container.registrationRepository

    /* ---------------- Stats ---------------- */
    
    val allStats: StateFlow<List<DailyStatEntity>> = statsRepository.getAllStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val today: StateFlow<DailyStatEntity> = allStats.map { list ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        list.find { it.date == todayStr } ?: DailyStatEntity(date = todayStr)
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        DailyStatEntity(date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    )

    /* ---------------- Registration ---------------- */

    val childId: StateFlow<String> = registrationRepository.childIdFlow
        .map { it ?: "Not Registered" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    fun registerIfNeeded() {
        viewModelScope.launch {
            registrationRepository.registerDeviceIfNeeded()
        }
    }

    /* ---------------- Notification timing preference ---------------- */

    val notifyAfterReels =
        NotificationSettingsStore
            .notifyAfterReelsFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 50
            )

    fun setNotifyAfterReels(reels: Int) {
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
}
