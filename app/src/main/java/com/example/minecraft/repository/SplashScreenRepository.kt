package com.example.minecraft.repository

import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.services.TaskService
import com.example.minecraft.ui.spash.SplashScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SplashScreenRepository @Inject constructor( private val service: TaskService, private val taskStore: AddonModel.Store) {

//    suspend fun getDataFromServer() = withContext(Dispatchers.IO) {
//        service.getData()
//    }

//
    fun getDataFromServer() = flow {
        val response = service.getData()
        if (response.isSuccessful) {
            response.body()?.let { entity ->
                // Insert, from initialization screen
                val model = entity.addon.map { it.convertToAddonModel(it) }
                insertFulData(model)

                emit(SplashScreenState.LoadComplete)
            }
        } else {
            emit(SplashScreenState.Error(response.message()))
        }
    }

    suspend fun insertFulData(list: List<AddonModel>) = withContext(Dispatchers.IO) {
        taskStore.initialization(list)
    }

//    fun getOfflineData() = taskStore.getAllDistinct()

}