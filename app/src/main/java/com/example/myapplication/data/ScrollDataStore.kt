package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("scroll_data")

object ScrollDataStore {

    private val SCROLL_COUNT_KEY = intPreferencesKey("scroll_count")

    fun scrollCountFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[SCROLL_COUNT_KEY] ?: 0
        }

    suspend fun incrementScroll(context: Context) {
        context.dataStore.edit { prefs ->
            val current = prefs[SCROLL_COUNT_KEY] ?: 0
            prefs[SCROLL_COUNT_KEY] = current + 1
        }
    }

    suspend fun reset(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[SCROLL_COUNT_KEY] = 0
        }
    }
}
