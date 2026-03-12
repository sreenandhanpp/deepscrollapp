package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(
    name = "onboarding_prefs"
)

object OnboardingStore {

    private val ONBOARDING_COMPLETED_KEY =
        booleanPreferencesKey("onboarding_completed")

    fun onboardingCompletedFlow(context: Context): Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] ?: false
        }

    suspend fun setOnboardingCompleted(context: Context) {
        context.onboardingDataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}
