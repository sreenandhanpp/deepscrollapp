package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.data.analytics.ScrollDailyStats
import com.example.myapplication.data.analytics.UsageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)

    /* ---------------- MONTH + YEAR ANALYTICS ---------------- */

    private val _showUserIdScreen = MutableStateFlow(false)
    val showUserIdScreen = _showUserIdScreen.asStateFlow()

    fun openUserIdScreen() {
        _showUserIdScreen.value = true
    }

    fun closeUserIdScreen() {
        _showUserIdScreen.value = false
    }

    private val _monthStats = MutableStateFlow<List<ScrollDailyStats>>(emptyList())
    val monthStats: StateFlow<List<ScrollDailyStats>> = _monthStats

    private val _yearStats = MutableStateFlow<List<ScrollDailyStats>>(emptyList())
    val yearStats: StateFlow<List<ScrollDailyStats>> = _yearStats

    fun loadMonthStats() {
        viewModelScope.launch {
            _monthStats.value = repository.getCurrentMonthStats()
        }
    }

    fun loadYearStats() {
        viewModelScope.launch {
            _yearStats.value = repository.getLastYearStats()
        }
    }

    /* ---------------- TODAY ANALYTICS (ROOM) ---------------- */

    val todayStats: StateFlow<ScrollDailyStats?> =
        repository.observeTodayStats()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    /* ---------------- ONBOARDING ---------------- */

    val onboardingCompleted = OnboardingStore
        .onboardingCompletedFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            OnboardingStore.setOnboardingCompleted(getApplication())
        }
    }

    /* ---------------- UPGRADE SYSTEM ---------------- */

    val upgradeSeen = UpgradeStore
        .upgradeSeenFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _showUpgrade = MutableStateFlow(false)
    val showUpgrade = _showUpgrade.asStateFlow()

    private var upgradeShownInMemory = false

    fun showUpgradeOnce() {
        if (!upgradeShownInMemory) {
            upgradeShownInMemory = true
            _showUpgrade.value = true
        }
    }

    fun closeUpgrade() {
        _showUpgrade.value = false
        viewModelScope.launch {
            UpgradeStore.markUpgradeSeen(getApplication())
        }
    }

    /* ---------------- USAGE STATS ---------------- */

    val totalUsageMinutesToday = UsageDataStore
        .totalTimeMinutesFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val deepScrollCount = UsageDataStore
        .deepScrollCountFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /* ---------------- REEL REMINDER ---------------- */

    val notifyAfterReels = NotificationSettingsStore
        .notifyAfterReelsFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    fun updateNotifyAfterReels(reels: Int) {
        viewModelScope.launch {
            NotificationSettingsStore.setNotifyAfterReels(
                getApplication(),
                reels
            )
        }
    }

    /* ---------------- NOTIFICATION TOGGLES ---------------- */

    private val _timeReminderEnabled = MutableStateFlow(true)
    val timeReminderEnabled = _timeReminderEnabled.asStateFlow()

    private val _rapidSwipeEnabled = MutableStateFlow(true)
    val rapidSwipeEnabled = _rapidSwipeEnabled.asStateFlow()

    private val _zoneOutEnabled = MutableStateFlow(true)
    val zoneOutEnabled = _zoneOutEnabled.asStateFlow()

    private val _roboticEnabled = MutableStateFlow(true)
    val roboticEnabled = _roboticEnabled.asStateFlow()

    private val _deepDiveEnabled = MutableStateFlow(true)
    val deepDiveEnabled = _deepDiveEnabled.asStateFlow()

    private val _mindlessEnabled = MutableStateFlow(true)
    val mindlessEnabled = _mindlessEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            loadNotificationToggles()
        }

        loadYearStats()
        loadMonthStats()
    }

    private suspend fun loadNotificationToggles() {

        _timeReminderEnabled.value = true
        _rapidSwipeEnabled.value = true
        _zoneOutEnabled.value = true
        _roboticEnabled.value = true
        _deepDiveEnabled.value = true
        _mindlessEnabled.value = true
    }

    fun setTimeReminderEnabled(enabled: Boolean) {
        _timeReminderEnabled.value = enabled
    }

    fun setRapidSwipeEnabled(enabled: Boolean) {
        _rapidSwipeEnabled.value = enabled
    }

    fun setZoneOutEnabled(enabled: Boolean) {
        _zoneOutEnabled.value = enabled
    }

    fun setRoboticEnabled(enabled: Boolean) {
        _roboticEnabled.value = enabled
    }

    fun setDeepDiveEnabled(enabled: Boolean) {
        _deepDiveEnabled.value = enabled
    }

    fun setMindlessEnabled(enabled: Boolean) {
        _mindlessEnabled.value = enabled
    }

    /* ---------------- NOTIFICATION SETTINGS SCREEN ---------------- */

    private val _showNotificationSettings = MutableStateFlow(false)
    val showNotificationSettings = _showNotificationSettings.asStateFlow()

    fun openNotificationSettings() {
        _showNotificationSettings.value = true
    }

    fun closeNotificationSettings() {
        _showNotificationSettings.value = false
    }
}