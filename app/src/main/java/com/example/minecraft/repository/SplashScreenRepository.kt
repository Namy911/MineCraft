package com.example.minecraft.repository

import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.services.TaskService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SplashScreenRepository @Inject constructor( private val service: TaskService, private val taskStore: AddonModel.Store) {

    suspend fun getData() = withContext(Dispatchers.IO) {
        service.getData()
    }

    suspend fun insertFulData(list: List<AddonModel>) = withContext(Dispatchers.IO) {
        taskStore.initialization(list)
    }
}