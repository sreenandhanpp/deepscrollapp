package com.example.myapplication.data.repository

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.remote.ApiService
import com.example.myapplication.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.registrationDataStore by preferencesDataStore("registration")

class RegistrationRepository(
    private val context: Context,
    private val apiService: ApiService
) {
    private val DEVICE_ID = stringPreferencesKey("device_id")
    private val CHILD_ID = stringPreferencesKey("child_id")
    private val REGISTRATION_STATUS = stringPreferencesKey("registration_status") // "PENDING", "REGISTERED"

    val deviceIdFlow: Flow<String?> = context.registrationDataStore.data.map { it[DEVICE_ID] }
    val childIdFlow: Flow<String?> = context.registrationDataStore.data.map { it[CHILD_ID] }
    val isRegisteredFlow: Flow<Boolean> = context.registrationDataStore.data.map { 
        it[REGISTRATION_STATUS] == "REGISTERED" 
    }

    suspend fun registerDeviceIfNeeded() {
        val prefs = context.registrationDataStore.data.first()
        var deviceId = prefs[DEVICE_ID]
        var childId = prefs[CHILD_ID]
        val status = prefs[REGISTRATION_STATUS]

        if (status == "REGISTERED") return

        if (deviceId == null) {
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) 
                ?: UUID.randomUUID().toString()
            context.registrationDataStore.edit { it[DEVICE_ID] = deviceId }
        }

        if (childId == null) {
            childId = generateChildId()
            context.registrationDataStore.edit { it[CHILD_ID] = childId }
        }

        try {
            val response = apiService.register(
                RegisterRequest(
                    childId = childId,
                    deviceId = deviceId,
                    label = "My Kid"
                )
            )
            if (response.isSuccessful) {
                context.registrationDataStore.edit { it[REGISTRATION_STATUS] = "REGISTERED" }
            }
        } catch (e: Exception) {
            // Will retry on next launch or via WorkManager
        }
    }

    private fun generateChildId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..7).map { chars.random() }.joinToString("")
    }
}
