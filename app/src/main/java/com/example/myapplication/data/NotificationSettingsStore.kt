package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationSettings by preferencesDataStore("notification_settings")

object NotificationSettingsStore {

    private val NOTIFY_AFTER_MINUTES =
        intPreferencesKey("notify_after_minutes")

    fun notifyAfterMinutesFlow(context: Context): Flow<Int> =
        context.notificationSettings.data.map {
            it[NOTIFY_AFTER_MINUTES] ?: 10   // 🌿 default = 10 min
        }

    suspend fun setNotifyAfterMinutes(context: Context, minutes: Int) {
        context.notificationSettings.edit {
            it[NOTIFY_AFTER_MINUTES] = minutes.coerceAtLeast(1)
        }
    }
}
