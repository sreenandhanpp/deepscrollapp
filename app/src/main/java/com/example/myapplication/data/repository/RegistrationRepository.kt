package com.example.myapplication.data.repository

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.remote.DeepScrollApi
import com.example.myapplication.data.remote.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.registrationStore by preferencesDataStore("registration")

class RegistrationRepository(
    private val api: DeepScrollApi,
    private val context: Context
) {
    private val childIdKey = stringPreferencesKey("child_id")
    private val deviceIdKey = stringPreferencesKey("device_id")
    private val registeredKey = booleanPreferencesKey("registered")

    val childIdFlow: Flow<String> = context.registrationStore.data.map { it[childIdKey] ?: "" }

    suspend fun ensureRegistered(label: String): Result<String> {
        val current = context.registrationStore.data.map { it }.firstSnapshot()
        if (current[registeredKey] == true) return Result.success(current[childIdKey].orEmpty())

        val deviceId = current[deviceIdKey] ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val childId = current[childIdKey] ?: generateChildId()
        context.registrationStore.edit {
            it[deviceIdKey] = deviceId
            it[childIdKey] = childId
        }

        return runCatching {
            val response = api.register(RegisterRequest(childId = childId, deviceId = deviceId, label = label))
            require(response.isSuccessful)
            context.registrationStore.edit { prefs -> prefs[registeredKey] = true }
            childId
        }
    }

    private fun generateChildId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString(7) { repeat(7) { append(chars[Random.nextInt(chars.length)]) } }
    }
}

private suspend fun <T> Flow<T>.firstSnapshot(): T = kotlinx.coroutines.flow.first(this)
