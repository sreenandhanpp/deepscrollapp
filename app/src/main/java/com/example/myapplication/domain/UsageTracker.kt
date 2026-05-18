package com.example.myapplication.domain

import com.example.myapplication.data.local.LocalStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

class UsageTracker(
    private val context: android.content.Context,
    private val localDataSource: LocalStatsDataSource
) {
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState

    suspend fun onFocusedReel(itemId: String, visiblePercent: Float, watchDurationMs: Long) {
        if (visiblePercent < 0.6f || watchDurationMs < 1200) return
        val state = _sessionState.value
        if (state.lastCountedReelId == itemId) return

        val updated = state.copy(
            reelsViewed = state.reelsViewed + 1,
            lastCountedReelId = itemId
        )
        _sessionState.value = updated
        persist()
    }

    suspend fun onScroll(speedPxPerSec: Float, isContinuous: Boolean, elapsedMs: Long) {
        val state = _sessionState.value
        val intensity = ((speedPxPerSec / 4500f) + (state.reelsViewed / 40f)).coerceIn(0f, 1f)
        val deep = isContinuous && elapsedMs > 30_000 && speedPxPerSec > 1600
        val nextDeepCount = if (deep) state.deepScrollCount + 1 else state.deepScrollCount
        _sessionState.value = state.copy(
            deepScrollCount = nextDeepCount,
            intensityScore = intensity,
            deepScrollStreak = if (deep) state.deepScrollStreak + 1 else 0
        )
        persist()
    }

    suspend fun onSessionMinute() {
        val state = _sessionState.value
        _sessionState.value = state.copy(usageMinutes = max(0, state.usageMinutes + 1))
        persist()
    }

    private suspend fun persist() {
        val s = _sessionState.value
        localDataSource.updateToday {
            it.copy(
                reelsViewed = s.reelsViewed,
                deepScrollCount = s.deepScrollCount,
                usageMinutes = s.usageMinutes,
                sessions = s.sessions,
                deepScrollStreak = s.deepScrollStreak,
                intensityScore = s.intensityScore
            )
        }
    }
}

data class SessionState(
    val reelsViewed: Int = 0,
    val deepScrollCount: Int = 0,
    val usageMinutes: Int = 0,
    val sessions: Int = 1,
    val deepScrollStreak: Int = 0,
    val intensityScore: Float = 0f,
    val lastCountedReelId: String? = null
)
