package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ScrollDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    var isTrackingEnabled = mutableStateOf(false)
        private set

    fun toggleTracking() {
        isTrackingEnabled.value = !isTrackingEnabled.value
    }

    val scrollCount =
        ScrollDataStore
            .scrollCountFlow(application)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0
            )
    var reelsNotifyInterval = mutableStateOf(10)
        private set

    fun updateReelInterval(value: Int) {
        if (value > 0) {
            reelsNotifyInterval.value = value
        }
    }

}
