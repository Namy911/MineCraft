package com.example.minecraft.ui.main

import android.util.Log
import androidx.lifecycle.*
import com.example.minecraft.repository.MainRepository
import com.example.minecraft.ui.util.RosterItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

sealed class RosterItemLoadState {
    object InitSate : RosterItemLoadState()
    data class LoadComplete(val content: List<RosterItem>) : RosterItemLoadState()
    data class LoadLast(val content: List<RosterItem>): RosterItemLoadState()
    data class Error(val error: String) : RosterItemLoadState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository,
    private val savedStateHandle: SavedStateHandle): ViewModel()
{
    companion object{
        val PATH_CACHE_RESOURCE: String = "ui.main.path.cache.resource"
        val PATH_CACHE_BEHAVIOR: String = "ui.main.path.cache.behavior"

        val FLAG_TRIAL: String = "ui.main.flag.trial"
        val FLAG_REWARD_DOWNLOAD: String = "ui.main.flag.reward"
        val FLAG_REWARD_SHARE: String = "ui.main.flag.share"

        val FLAG_INIT: String = "ui.main.flag.init"
    }
//    private val _list = MutableSharedFlow<List<RosterItem>>(replay = 1,  onBufferOverflow = BufferOverflow.DROP_LATEST)
//    val list: SharedFlow<List<RosterItem>> = _list.asSharedFlow()

    private val _list = MutableStateFlow<RosterItemLoadState>(RosterItemLoadState.InitSate)
    val list: StateFlow<RosterItemLoadState> = _list.asStateFlow()

//    private val _ad = MutableStateFlow<RosterItem?>(null)
//    val ad: StateFlow<RosterItem?> = _ad.asStateFlow()

    fun getItem(offset: Int, limit: Int) {
        Log.d(TAG, "getItem: ddddd")
        viewModelScope.launch {
            val result = repository.getLimit(offset, limit)
//            delay(1500)
            _list.value = if (result.isNotEmpty() && result.size == MainFragment.PAGE_SIZE) {
                RosterItemLoadState.LoadComplete(result)
            }else if (result.isNotEmpty() && result.size < MainFragment.PAGE_SIZE) {
                RosterItemLoadState.LoadLast(result)
            } else {
                RosterItemLoadState.Error("somme error")
            }
        }
    }

    fun setCachePathResource(path: String){ savedStateHandle.set(PATH_CACHE_RESOURCE, path) }
    fun getCachePathResource() = savedStateHandle.get<String>(PATH_CACHE_RESOURCE)

    fun setCachePathBehavior(path: String){ savedStateHandle.set(PATH_CACHE_BEHAVIOR, path) }
    fun getCachePathBehavior() = savedStateHandle.get<String>(PATH_CACHE_BEHAVIOR)

    fun setFlagTrial(flag: Boolean){ savedStateHandle.set(FLAG_TRIAL, flag) }
    fun getFlagTrial() = savedStateHandle.get<Boolean>(FLAG_TRIAL)

    fun setFlagRewardDownload(flag: Boolean){ savedStateHandle.set(FLAG_REWARD_DOWNLOAD, flag) }
    fun getFlagRewardDownload() = savedStateHandle.get<Boolean>(FLAG_REWARD_DOWNLOAD)

    fun setFlagRewardShare(flag: Boolean){ savedStateHandle.set(FLAG_REWARD_SHARE, flag) }
    fun getFlagRewardShare() = savedStateHandle.get<Boolean>(FLAG_REWARD_SHARE)
}