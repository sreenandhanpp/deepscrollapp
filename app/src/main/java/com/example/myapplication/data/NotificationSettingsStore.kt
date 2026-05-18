package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationSettings by preferencesDataStore("notification_settings")

object NotificationSettingsStore {

    private val NOTIFY_AFTER_REELS =
        intPreferencesKey("notify_after_reels")

    fun notifyAfterReelsFlow(context: Context): Flow<Int> =
        context.notificationSettings.data.map {
            it[NOTIFY_AFTER_REELS] ?: 50   // 🌿 default = 50 reels
        }

    suspend fun setNotifyAfterReels(context: Context, reels: Int) {
        context.notificationSettings.edit {
            it[NOTIFY_AFTER_REELS] = reels.coerceAtLeast(1)
        }
    }
}
