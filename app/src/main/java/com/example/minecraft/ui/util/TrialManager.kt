package com.example.minecraft.ui.util

import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.minecraft.ui.PremiumActivity
import com.example.minecraft.ui.spash.SplashScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class TrialManager(val context: Context) {
    companion object {
        const val TRIAL_EXIST = "ui.util.trial.exist"
        const val TRIAL_NOT_EXIST = "ui.util.trial.not.exist"
        const val TRIAL_NAME = "ui.util.trial.name"
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = TRIAL_NAME)
    }

    val trialFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PremiumActivity.TRIAL_BOUGHT] ?: TRIAL_NOT_EXIST
        }

    suspend fun setTrial() {
        context.dataStore.edit {
                trialSetting -> trialSetting[PremiumActivity.TRIAL_BOUGHT] = TRIAL_EXIST
        }
    }
}