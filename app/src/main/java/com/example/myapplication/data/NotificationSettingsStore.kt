package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore delegate (top-level)
val Context.notificationSettings: DataStore<Preferences> by preferencesDataStore(
    name = "notification_settings"
)

object NotificationSettingsStore {

    private val NOTIFY_AFTER_REELS = intPreferencesKey("notify_after_reels")

    private const val DEFAULT_REELS = 10

    /**
     * Flow that emits the current "notify after X reels" value
     */
    fun notifyAfterReelsFlow(context: Context): Flow<Int> =
        context.notificationSettings.data.map {
            it[NOTIFY_AFTER_REELS] ?: DEFAULT_REELS
        }

    /**
     * One-time suspend read – useful for service initial value
     */
    suspend fun getNotifyAfterReels(context: Context): Int =
        context.notificationSettings.data
            .map { it[NOTIFY_AFTER_REELS] ?: DEFAULT_REELS }
            .first()

    /**
     * Update the value and persist it
     */
    suspend fun setNotifyAfterReels(context: Context, reels: Int) {
        context.notificationSettings.edit {
            it[NOTIFY_AFTER_REELS] = reels.coerceAtLeast(1)
        }
    }
}