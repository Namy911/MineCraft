package com.example.minecraft.data.services

import com.example.minecraft.data.network.TaskEntity
import retrofit2.Response
import retrofit2.http.GET

interface TaskService {

    @GET("addons.json")
    suspend fun getData(): Response<TaskEntity>

    @GET("resource.mcpack")
    suspend fun getResource()

    @GET("behavior.mcpack")
    suspend fun getBehavior()
}