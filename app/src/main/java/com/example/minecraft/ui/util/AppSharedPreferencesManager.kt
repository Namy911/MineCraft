package com.example.minecraft.ui.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = AppSharedPreferencesManager.BILLING_MANAGER)

class AppSharedPreferencesManager @Inject constructor(val context: Context) {
    companion object {
        const val PREF_ACKNOWLEDGE= "ui.util.pref.name.acknowledge"
        const val BILLING_MANAGER = "ui.util.billing.Acknowledge"
    }

    private val ACKNOWLEDGE = booleanPreferencesKey(PREF_ACKNOWLEDGE)

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