package com.example.myapplication.data

import android.content.Context
import android.util.Log
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
    private val DEEP_SCROLL_COUNT = intPreferencesKey("deep_scroll_count")
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
                Log.d("UsageDataStore", "Day changed from $savedDay to $today, resetting counters")
                prefs[TOTAL_SCROLL_MINUTES] = 0L
                prefs[DEEP_SCROLL_COUNT] = 0
                prefs[LAST_UPDATED_DAY] = today
            }
        }
    }

    /**
     * Add session time in **minutes** with improved precision
     * Now ensures at least 1 minute for any non-zero duration
     */
    suspend fun addSessionTime(context: Context, durationMs: Long) {
        // Don't add if duration is too small (less than 1 second)
        if (durationMs < 1000) return

        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val currentMinutes = prefs[TOTAL_SCROLL_MINUTES] ?: 0L

            // Convert ms → minutes, ensuring at least 1 minute for any meaningful duration
            val sessionMinutes = if (durationMs >= 60_000) {
                // If it's 1 minute or more, round to nearest minute
                (durationMs + 30_000) / 60_000
            } else {
                // If it's less than 1 minute but more than 0, count as 1 minute
                1L
            }

            val newTotal = currentMinutes + sessionMinutes
            prefs[TOTAL_SCROLL_MINUTES] = newTotal

            Log.d("UsageDataStore", "Added $sessionMinutes min (from ${durationMs}ms), total: $newTotal")
        }
    }

    suspend fun incrementDeepScroll(context: Context) {
        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val current = prefs[DEEP_SCROLL_COUNT] ?: 0
            val newTotal = current + 1
            prefs[DEEP_SCROLL_COUNT] = newTotal
            Log.d("UsageDataStore", "Deep scroll incremented to $newTotal")
        }
    }

    /**
     * Flow of total scrolling time **in minutes** for today
     */
    fun totalTimeMinutesFlow(context: Context): Flow<Long> =
        context.usageDataStore.data.map {
            val value = it[TOTAL_SCROLL_MINUTES] ?: 0L
            Log.d("UsageDataStore", "totalTimeMinutesFlow emitting: $value")
            value
        }

    fun deepScrollCountFlow(context: Context): Flow<Int> =
        context.usageDataStore.data.map { it[DEEP_SCROLL_COUNT] ?: 0 }
}