package com.example.minecraft.repository

import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.network.toAddonModel
import com.example.minecraft.data.services.TaskService
import com.example.minecraft.ui.spash.SplashScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SplashScreenRepository @Inject constructor( private val service: TaskService, private val taskStore: AddonModel.Store) {

    fun getDataFromServer() = flow {
        val response = service.getData()
        if (response.isSuccessful) {
            response.body()?.let { entity ->
                // Insert, from initialization screen
                val model = entity.addon.map { it.toAddonModel() }
                insertFulData(model)

                emit(SplashScreenState.LoadComplete)
            }
        } else {
            emit(SplashScreenState.Error(response.message()))
        }
    }

    private suspend fun insertFulData(list: List<AddonModel>) = withContext(Dispatchers.IO) {
        taskStore.initialization(list)
    }
}