package com.example.myapplication.data.sync

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.deviceStore by preferencesDataStore("device_store")

object DeviceIdManager {

    private val DEVICE_ID = stringPreferencesKey("device_id")

    suspend fun getDeviceId(context: Context): String {

        val prefs = context.deviceStore.data.first()

        val existing = prefs[DEVICE_ID]

        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()

        context.deviceStore.edit {
            it[DEVICE_ID] = newId
        }

        return newId
    }
}