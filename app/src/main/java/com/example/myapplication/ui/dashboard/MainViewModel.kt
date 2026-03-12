package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /* ---------------- Onboarding & Upgrade ---------------- */

    val upgradeSeen = UpgradeStore
        .upgradeSeenFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
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

    val onboardingCompleted = OnboardingStore
        .onboardingCompletedFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            OnboardingStore.setOnboardingCompleted(getApplication())
        }
    }

    /* ---------------- Usage Stats ---------------- */

    val totalUsageMinutesToday = UsageDataStore
        .totalTimeMinutesFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0L
        )

    val deepScrollCount = UsageDataStore
        .deepScrollCountFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /* ---------------- Reel Reminder Preference ---------------- */

    val notifyAfterReels = NotificationSettingsStore
        .notifyAfterReelsFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 10
        )

    fun updateNotifyAfterReels(reels: Int) {
        viewModelScope.launch {
            NotificationSettingsStore.setNotifyAfterReels(getApplication(), reels)
        }
    }

    /* ---------------- Notification Toggles (now persisted) ---------------- */

    private val _timeReminderEnabled = MutableStateFlow(true)
    val timeReminderEnabled: StateFlow<Boolean> = _timeReminderEnabled.asStateFlow()

    private val _rapidSwipeEnabled = MutableStateFlow(true)
    val rapidSwipeEnabled: StateFlow<Boolean> = _rapidSwipeEnabled.asStateFlow()

    private val _zoneOutEnabled = MutableStateFlow(true)
    val zoneOutEnabled: StateFlow<Boolean> = _zoneOutEnabled.asStateFlow()

    private val _roboticEnabled = MutableStateFlow(true)
    val roboticEnabled: StateFlow<Boolean> = _roboticEnabled.asStateFlow()

    private val _deepDiveEnabled = MutableStateFlow(true)
    val deepDiveEnabled: StateFlow<Boolean> = _deepDiveEnabled.asStateFlow()

    private val _mindlessEnabled = MutableStateFlow(true)
    val mindlessEnabled: StateFlow<Boolean> = _mindlessEnabled.asStateFlow()

    init {
        // Load persisted toggle states once on creation
        viewModelScope.launch {
            loadNotificationToggles()
        }
    }

    private suspend fun loadNotificationToggles() {
        val app = getApplication<Application>()

        // You can later create a NotificationToggleStore with keys like:
        // time_reminder_enabled, rapid_swipe_enabled, etc.

        // For now – assuming defaults or reading from somewhere
        // Replace with real persistence when you add the keys
        _timeReminderEnabled.value = true    // ← replace with real read
        _rapidSwipeEnabled.value = true
        _zoneOutEnabled.value = true
        _roboticEnabled.value = true
        _deepDiveEnabled.value = true
        _mindlessEnabled.value = true
    }

    fun setTimeReminderEnabled(enabled: Boolean) {
        _timeReminderEnabled.value = enabled
        // TODO: save to DataStore when you add persistence
    }

    fun setRapidSwipeEnabled(enabled: Boolean) {
        _rapidSwipeEnabled.value = enabled
        // TODO: save
    }

    fun setZoneOutEnabled(enabled: Boolean) {
        _zoneOutEnabled.value = enabled
        // TODO: save
    }

    fun setRoboticEnabled(enabled: Boolean) {
        _roboticEnabled.value = enabled
        // TODO: save
    }

    fun setDeepDiveEnabled(enabled: Boolean) {
        _deepDiveEnabled.value = enabled
        // TODO: save
    }

    fun setMindlessEnabled(enabled: Boolean) {
        _mindlessEnabled.value = enabled
        // TODO: save
    }

    /* ---------------- Notification Settings Screen ---------------- */

    private val _showNotificationSettings = MutableStateFlow(false)
    val showNotificationSettings = _showNotificationSettings.asStateFlow()

    fun openNotificationSettings() {
        _showNotificationSettings.value = true
    }

    fun closeNotificationSettings() {
        _showNotificationSettings.value = false
    }
}