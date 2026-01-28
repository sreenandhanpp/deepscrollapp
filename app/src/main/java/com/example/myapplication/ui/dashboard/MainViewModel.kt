package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /* ---------------- Onboarding ---------------- */

    private val _showUpgrade = MutableStateFlow(false)
    val showUpgrade = _showUpgrade.asStateFlow()

    fun showUpgradeOnce() {
        _showUpgrade.value = true
    }

    fun closeUpgrade() {
        _showUpgrade.value = false
    }

    val onboardingCompleted =
        OnboardingStore
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

    /* ---------------- Meaningful reflection data ---------------- */

    val totalUsageMinutesToday =
        UsageDataStore
            .totalTimeMinutesFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0L
            )

    val deepScrollCount =
        UsageDataStore
            .deepScrollCountFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    /* ---------------- Notification timing preference ---------------- */

    val notifyAfterMinutes =
        NotificationSettingsStore
            .notifyAfterMinutesFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 10
            )

    fun updateNotifyAfterMinutes(minutes: Int) {
        viewModelScope.launch {
            NotificationSettingsStore.setNotifyAfterMinutes(
                getApplication(),
                minutes
            )
        }
    }

    /* =========================================================
       🔔 Notification Settings Screen Navigation
       ========================================================= */

    private val _showNotificationSettings = MutableStateFlow(false)
    val showNotificationSettings: StateFlow<Boolean> =
        _showNotificationSettings.asStateFlow()

    fun openNotificationSettings() {
        _showNotificationSettings.value = true
    }

    fun closeNotificationSettings() {
        _showNotificationSettings.value = false
    }

    /* =========================================================
       🎛 Notification Toggles (UI-ready)
       ========================================================= */

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

    fun setTimeReminderEnabled(value: Boolean) {
        _timeReminderEnabled.value = value
    }

    fun setRapidSwipeEnabled(value: Boolean) {
        _rapidSwipeEnabled.value = value
    }

    fun setZoneOutEnabled(value: Boolean) {
        _zoneOutEnabled.value = value
    }

    fun setRoboticEnabled(value: Boolean) {
        _roboticEnabled.value = value
    }

    fun setDeepDiveEnabled(value: Boolean) {
        _deepDiveEnabled.value = value
    }

    fun setMindlessEnabled(value: Boolean) {
        _mindlessEnabled.value = value
    }
}
