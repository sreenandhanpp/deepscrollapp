package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.usageDataStore by preferencesDataStore("usage_data")

object UsageDataStore {

    private val TOTAL_SCROLL_MINUTES = longPreferencesKey("total_scroll_minutes")
    private val TOTAL_SCROLL_MS = longPreferencesKey("total_scroll_ms")
    private val DEEP_SCROLL_COUNT = intPreferencesKey("deep_scroll_count")
    private val REELS_SCROLLED_COUNT = intPreferencesKey("reels_scrolled_count")
    private val LAST_UPDATED_DAY = intPreferencesKey("last_updated_day")

    private fun todayKey(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    /** Reset data automatically when day changes */
    private suspend fun resetIfNewDay(context: Context) {
        val today = todayKey()
        context.usageDataStore.edit { prefs ->
            val savedDay = prefs[LAST_UPDATED_DAY]
            if (savedDay != today) {
                prefs[TOTAL_SCROLL_MINUTES] = 0L
                prefs[TOTAL_SCROLL_MS] = 0L
                prefs[DEEP_SCROLL_COUNT] = 0
                prefs[REELS_SCROLLED_COUNT] = 0
                prefs[LAST_UPDATED_DAY] = today
            }
        }
    }

    /**
     * Add session time in **minutes** (rounded up or down as needed)
     */
    suspend fun addSessionTime(context: Context, durationMs: Long) {
        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val currentMs = prefs[TOTAL_SCROLL_MS] ?: 0L
            val updatedMs = currentMs + durationMs
            prefs[TOTAL_SCROLL_MS] = updatedMs
            prefs[TOTAL_SCROLL_MINUTES] = updatedMs / 60_000L
        }
    }

    suspend fun incrementReelsScrolled(context: Context): Int {
        resetIfNewDay(context)
        var updated = 0
        context.usageDataStore.edit { prefs ->
            updated = (prefs[REELS_SCROLLED_COUNT] ?: 0) + 1
            prefs[REELS_SCROLLED_COUNT] = updated
        }
        return updated
    }

    suspend fun incrementDeepScroll(context: Context) {
        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val current = prefs[DEEP_SCROLL_COUNT] ?: 0
            prefs[DEEP_SCROLL_COUNT] = current + 1
        }
    }

    /**
     * Flow of total scrolling time **in minutes** for today
     */
    fun totalTimeMinutesFlow(context: Context): Flow<Long> =
        context.usageDataStore.data.map { prefs ->
            val isToday = prefs[LAST_UPDATED_DAY] == todayKey()
            if (isToday) prefs[TOTAL_SCROLL_MINUTES] ?: 0L else 0L
        }

    fun deepScrollCountFlow(context: Context): Flow<Int> =
        context.usageDataStore.data.map { prefs ->
            val isToday = prefs[LAST_UPDATED_DAY] == todayKey()
            if (isToday) prefs[DEEP_SCROLL_COUNT] ?: 0 else 0
        }

    fun reelsScrolledCountFlow(context: Context): Flow<Int> =
        context.usageDataStore.data.map { prefs ->
            val isToday = prefs[LAST_UPDATED_DAY] == todayKey()
            if (isToday) prefs[REELS_SCROLLED_COUNT] ?: 0 else 0
        }

}
