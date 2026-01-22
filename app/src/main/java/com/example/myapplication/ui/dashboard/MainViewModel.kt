package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /* ---------------- Onboarding ---------------- */

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

    val totalUsageTime =
        UsageDataStore
            .totalTimeFlow(application)
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
}
