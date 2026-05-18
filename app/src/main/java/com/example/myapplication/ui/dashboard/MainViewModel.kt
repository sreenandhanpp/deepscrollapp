package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.app.AppContainer
import com.example.myapplication.data.local.DailyStatEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = AppContainer(app)

    val childId: StateFlow<String> = container.registrationRepository.childIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val stats: StateFlow<List<DailyStatEntity>> = container.localStatsDataSource.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val today = stats.map { it.firstOrNull() ?: DailyStatEntity("", 0, 0, 0, 0, 0, 0f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStatEntity("", 0, 0, 0, 0, 0, 0f))

    fun registerIfNeeded(label: String = "My Kid") = viewModelScope.launch {
        container.registrationRepository.ensureRegistered(label)
    }
}
