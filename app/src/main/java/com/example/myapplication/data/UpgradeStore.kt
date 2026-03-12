package com.example.myapplication.data


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object UpgradeStore {

    private val Context.dataStore by preferencesDataStore("upgrade_store")

    private val UPGRADE_SEEN = booleanPreferencesKey("upgrade_seen")

    fun upgradeSeenFlow(context: Context) =
        context.dataStore.data.map {
            it[UPGRADE_SEEN] ?: false
        }

    suspend fun markUpgradeSeen(context: Context) {
        context.dataStore.edit {
            it[UPGRADE_SEEN] = true
        }
    }
}

