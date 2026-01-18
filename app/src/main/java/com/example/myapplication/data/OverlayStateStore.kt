package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.overlayStore by preferencesDataStore("overlay_state")

object OverlayStateStore {

    private val SHOW_OVERLAY_KEY = booleanPreferencesKey("show_overlay")

    fun overlayVisibleFlow(context: Context): Flow<Boolean> =
        context.overlayStore.data.map { prefs ->
            prefs[SHOW_OVERLAY_KEY] ?: false
        }

    suspend fun setOverlayVisible(context: Context, visible: Boolean) {
        context.overlayStore.edit { prefs ->
            prefs[SHOW_OVERLAY_KEY] = visible
        }
    }
}
