package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reelSettings by preferencesDataStore("reel_settings")

object ReelSettingsStore {

    private val INTERVAL_KEY = intPreferencesKey("reel_interval")

    fun intervalFlow(context: Context): Flow<Int> =
        context.reelSettings.data.map {
            it[INTERVAL_KEY] ?: 10
        }

    suspend fun setInterval(context: Context, value: Int) {
        context.reelSettings.edit {
            it[INTERVAL_KEY] = value
        }
    }
}
