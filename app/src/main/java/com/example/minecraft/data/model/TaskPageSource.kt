package com.example.minecraft.data.model

import androidx.paging.*
import com.example.minecraft.data.TaskModel
import com.example.minecraft.data.db.TaskDataBase
import com.example.minecraft.data.services.TaskService
import java.lang.Exception


//@ExperimentalPagingApi
//class TaskPageSource(
//    private val query: String,
//    private val database: AddonModel.Store,
//    private val networkService: TaskService
//) : RemoteMediator<Int, AddonModel>() {
//    override suspend fun load(
//        loadType: LoadType,
//        state: PagingState<Int, AddonModel>
//    ): MediatorResult {
//        try {
//            val page = when (loadType) {
//                LoadType.PREPEND -> {
//                    return MediatorResult.Success(endOfPaginationReached = true)
//                }
//                LoadType.APPEND -> {
//            }
//                LoadType.REFRESH -> {
//                    null
//                }
//            }
//        } catch (e: Exception) {
//            MediatorResult.Error(e)
//        }
//    }

//    suspend fun getCloselyKey(state: PagingState<Int, AddonModel>): AddonModel? {
//        return state.anchorPosition?.let {
//            state.closestItemToPosition(it)?.let {
//                db.getOne(it.id)
//            }
//        }
//    }
//    suspend fun getLastKey(state: PagingState<Int, AddonModel>): AddonModel? {
//        return state.lastItemOrNull()?.let {
//                db.getOne(it.id)
//            }
//    }

//}