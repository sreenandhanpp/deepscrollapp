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

    private val TOTAL_SCROLL_TIME_MS = longPreferencesKey("total_scroll_time_ms")
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
                prefs[TOTAL_SCROLL_TIME_MS] = 0L
                prefs[DEEP_SCROLL_COUNT] = 0
                prefs[LAST_UPDATED_DAY] = today
            }
        }
    }

    suspend fun addSessionTime(context: Context, durationMs: Long) {
        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val current = prefs[TOTAL_SCROLL_TIME_MS] ?: 0L
            prefs[TOTAL_SCROLL_TIME_MS] = current + durationMs
        }
    }

    suspend fun incrementDeepScroll(context: Context) {
        resetIfNewDay(context)
        context.usageDataStore.edit { prefs ->
            val current = prefs[DEEP_SCROLL_COUNT] ?: 0
            prefs[DEEP_SCROLL_COUNT] = current + 1
        }
    }

    fun totalTimeFlow(context: Context): Flow<Long> =
        context.usageDataStore.data.map { it[TOTAL_SCROLL_TIME_MS] ?: 0L }

    fun deepScrollCountFlow(context: Context): Flow<Int> =
        context.usageDataStore.data.map { it[DEEP_SCROLL_COUNT] ?: 0 }
}
