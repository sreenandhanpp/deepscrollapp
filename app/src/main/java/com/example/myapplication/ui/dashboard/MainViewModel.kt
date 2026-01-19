package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.OnboardingStore
import com.example.myapplication.data.ScrollDataStore
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

    /* ---------------- Tracking toggle (UI only) ---------------- */

    var isTrackingEnabled = mutableStateOf(false)
        private set

    fun toggleTracking() {
        isTrackingEnabled.value = !isTrackingEnabled.value
    }

    /* ---------------- Scroll stats ---------------- */

    val scrollCount =
        ScrollDataStore
            .scrollCountFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )

    /* ---------------- Reel notification interval (UI only for now) ---------------- */

    var reelsNotifyInterval = mutableStateOf(10)
        private set

    fun updateReelInterval(value: Int) {
        if (value > 0) {
            reelsNotifyInterval.value = value
        }
    }
}
