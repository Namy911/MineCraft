package com.example.minecraft.repository

import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.services.TaskService
import com.example.minecraft.ui.main.MainFragment
import com.example.minecraft.ui.main.RosterItemLoadState
import com.example.minecraft.ui.main.RosterItemOffLineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class MainRepository @Inject constructor(private val taskStore: AddonModel.Store) {

    suspend fun getLimit(offset: Int, limit: Int) = withContext(Dispatchers.IO) {
        val result = taskStore.getLimit(offset, limit)

        when {
            result.isNotEmpty() && result.size == MainFragment.PAGE_SIZE -> {
                RosterItemLoadState.LoadComplete(result)
            }
            result.isNotEmpty() && result.size < MainFragment.PAGE_SIZE -> {
                RosterItemLoadState.LoadLast(result)
            }
            else -> { RosterItemLoadState.Error("Empty list RosterItemLoadState") }
        }
    }

    suspend fun getAll() =  withContext(Dispatchers.IO){
        val result = taskStore.getAll()

        if (result.isNotEmpty()){
            RosterItemOffLineState.LoadComplete(result)
        }else {
            RosterItemOffLineState.Error("Empty list RosterItemOffLineState")
        }
    }
}