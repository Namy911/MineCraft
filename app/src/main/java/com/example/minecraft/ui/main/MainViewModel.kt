package com.example.minecraft.ui.main

import androidx.lifecycle.*
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.network.AddonEntity
import com.example.minecraft.data.network.util.Resource

import com.example.minecraft.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository,
    private val savedStateHandle: SavedStateHandle): ViewModel()
{
    sealed class MainViewState{
        object Loading: MainViewState()
        data class Content(val list : List<AddonModel>): MainViewState()
        data class Error (val error: Throwable): MainViewState()
    }
    companion object{
        const val PAGE_SIZE = "ui.main.page.size"
        const val PAGE_COUNT = "ui.main.page.count"
    }
    private val _list = MutableSharedFlow<List<AddonModel>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val list: SharedFlow<List<AddonModel>> = _list.asSharedFlow()

    fun setPageSize(size: Int = 1){ savedStateHandle.set(PAGE_SIZE, size) }
    fun getPageSize() = savedStateHandle.get<Int>(PAGE_SIZE)

//    fun setCount(value: Int){ savedStateHandle.set(PAGE_COUNT, value)}
//    fun getCount() = savedStateHandle.get<Int>(PAGE_COUNT)

    // Get data from DB
    fun getLimit(offset: Int, limit: Int){
        viewModelScope.launch ( Dispatchers.IO ){
            _list.emitAll(repository.getLimit(offset, limit))
        }
    }
}