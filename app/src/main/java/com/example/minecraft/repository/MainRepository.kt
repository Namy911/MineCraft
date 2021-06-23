package com.example.minecraft.repository

import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.services.TaskService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MainRepository @Inject constructor(private val taskStore: AddonModel.Store) {

//    suspend fun getLimit( offset: Int, limit: Int) =  withContext(Dispatchers.IO){ taskStore.getLimit(offset, limit) }
    fun getLimit( offset: Int, limit: Int) =   taskStore.getLimit(offset, limit)
    fun getAll() =  taskStore.getAllDistinct()
}