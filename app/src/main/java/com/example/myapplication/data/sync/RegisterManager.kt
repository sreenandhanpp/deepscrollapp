package com.example.myapplication.data.sync

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.childStore by preferencesDataStore("child_store")

object RegisterManager {

    private val CHILD_ID = stringPreferencesKey("child_id")

    /* ==========================================================
       PUBLIC ENTRY
       ========================================================== */

    suspend fun registerIfNeeded(context: Context) {

        val deviceId = DeviceIdManager.getDeviceId(context)

        // ✅ Get or create childId
        val childId = getOrCreateChildId(context)

        Log.d("Register", "Using childId: $childId")

        val request = RegisterRequest(
            childId = childId,
            deviceId = deviceId,
            label = "My Device"
        )

        try {
            val response = ApiClient.authApi.register(request)

            if (response.isSuccessful) {
                Log.d("Register", "✅ Registered successfully: $childId")
            } else {

                // 🔥 409 = already exists → OK
                if (response.code() == 409) {
                    Log.d("Register", "⚠️ Already registered (OK): $childId")
                } else {
                    Log.e("Register", "❌ Failed: ${response.code()}")
                }
            }

        } catch (e: Exception) {
            Log.e("Register", "❌ Error", e)
        }
    }

    /* ==========================================================
       CHILD ID GENERATION + STORAGE
       ========================================================== */

    private suspend fun getOrCreateChildId(context: Context): String {

        val prefs = context.childStore.data.first()

        val existing = prefs[CHILD_ID]
        if (existing != null) return existing

        val newId = generateChildId()

        context.childStore.edit {
            it[CHILD_ID] = newId
        }

        Log.d("Register", "🆕 Generated new childId: $newId")

        return newId
    }

    /* ==========================================================
       GENERATOR
       ========================================================== */

    private fun generateChildId(): String {

        val raw = UUID.randomUUID()
            .toString()
            .replace("-", "")

        return raw.take(8).uppercase()  // Example: A1B2C3D4
    }

    suspend fun getChildId(context: Context): String {
        return getOrCreateChildId(context)
    }
}