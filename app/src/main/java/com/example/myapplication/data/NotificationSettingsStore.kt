package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationSettings by preferencesDataStore("notification_settings")

object NotificationSettingsStore {

    private val NOTIFY_AFTER_REELS = intPreferencesKey("notify_after_reels")
    private val TIME_REMINDER = booleanPreferencesKey("time_reminder")
    private val RAPID_SWIPE = booleanPreferencesKey("rapid_swipe")
    private val ZONE_OUT = booleanPreferencesKey("zone_out")
    private val ROBOTIC = booleanPreferencesKey("robotic")
    private val DEEP_DIVE = booleanPreferencesKey("deep_dive")
    private val MINDLESS = booleanPreferencesKey("mindless")

    fun notifyAfterReelsFlow(context: Context): Flow<Int> =
        context.notificationSettings.data.map { it[NOTIFY_AFTER_REELS] ?: 1 } // Changed to 1 for immediate notification

    suspend fun setNotifyAfterReels(context: Context, reels: Int) {
        context.notificationSettings.edit { it[NOTIFY_AFTER_REELS] = reels.coerceAtLeast(1) }
    }

    fun timeReminderEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[TIME_REMINDER] ?: true }

    suspend fun setTimeReminderEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[TIME_REMINDER] = enabled }
    }

    fun rapidSwipeEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[RAPID_SWIPE] ?: true }

    suspend fun setRapidSwipeEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[RAPID_SWIPE] = enabled }
    }

    fun zoneOutEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[ZONE_OUT] ?: true }

    suspend fun setZoneOutEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[ZONE_OUT] = enabled }
    }

    fun roboticEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[ROBOTIC] ?: true }

    suspend fun setRoboticEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[ROBOTIC] = enabled }
    }

    fun deepDiveEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[DEEP_DIVE] ?: true }

    suspend fun setDeepDiveEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[DEEP_DIVE] = enabled }
    }

    fun mindlessEnabled(context: Context): Flow<Boolean> =
        context.notificationSettings.data.map { it[MINDLESS] ?: true }

    suspend fun setMindlessEnabled(context: Context, enabled: Boolean) {
        context.notificationSettings.edit { it[MINDLESS] = enabled }
    }
}