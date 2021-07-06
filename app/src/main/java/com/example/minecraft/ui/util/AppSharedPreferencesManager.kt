package com.example.minecraft.ui.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.minecraft.PremiumActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = AppSharedPreferencesManager.BILLING_ADS)

class AppSharedPreferencesManager @Inject constructor(val context: Context) {
    companion object {
        const val TRIAL_EXIST = "ui.util.trial.exist"
        const val TRIAL_ACKNOWLEDGE= "ui.util.trial.not.exist"

        const val BILLING_ADS = "ui.util.billing.Acknowledge"
    }
    val ACKNOWLEDGE = booleanPreferencesKey("Acknowledge")

    val billingAdsSate: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ACKNOWLEDGE] ?: false
        }

    suspend fun setBillingAdsSate(state: Boolean) {
        context.dataStore.edit {
                preferences -> preferences[ACKNOWLEDGE] = state
        }
    }
}