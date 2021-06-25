package com.example.minecraft.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.repository.MainRepository
import com.example.minecraft.ui.util.AdsItem
import com.example.minecraft.ui.util.Event
import com.example.minecraft.ui.util.RosterItem
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"


sealed class AdLoadState {
    object Loading : AdLoadState()
    data class LoadComplete(val content: List<RosterItem>) : AdLoadState()
    data class Error(val error: String) : AdLoadState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository,
    private val savedStateHandle: SavedStateHandle): ViewModel()
{
    companion object{
        val PATH_CACHE_RESOURCE: String = "ui.main.path.cache.resource"
        val PATH_CACHE_BEHAVIOR: String = "ui.main.path.cache.behavior"
        val PATH_PRIVATE_RESOURCE: String = "ui.main.path.private.resource"
        val PATH_PRIVATE_BEHAVIOR: String = "ui.main.path.private.behavior"

        val FLAG_ADMOB: String = "ui.main.flag.mob"
        val FLAG_TRIAL: String = "ui.main.flag.trial"

        val FLAG_INIT: String = "ui.main.flag.init"
//        val PATH_SD_BEHAVIOR: String = "ui.main.path.sd.behavior"
    }

//    private val _list: MutableLiveData<Event<List<AddonModel>>> by lazy { MutableLiveData<Event<List<AddonModel>>>() }
//    val list: LiveData<Event<List<AddonModel>>> = _list

//    private val _list: MutableLiveData<List<AddonModel>> by lazy { MutableLiveData<List<AddonModel>>() }
//    val list: LiveData<List<AddonModel>> = _list

    private val _list = MutableSharedFlow<List<AddonModel>>(replay = 1,  onBufferOverflow = BufferOverflow.DROP_LATEST)
    val list: SharedFlow<List<AddonModel>> = _list.asSharedFlow()

    val adList: MutableLiveData<List<NativeAd>> by lazy { MutableLiveData<List<NativeAd>>() }
//    val adLoad: LiveData<AdLoadState> = _adLoad

    private val _allList: MutableLiveData<List<AddonModel>> by lazy { MutableLiveData<List<AddonModel>>() }
    val allList: LiveData<List<AddonModel>> = _allList


//    fun getLimit(offset: Int, limit: Int){
//        viewModelScope.launch ( Dispatchers.Main ){
//            _list.value  = Event(repository.getLimit(offset, limit))
//        }
//    }

//    fun getLimit(offset: Int, limit: Int){
//        viewModelScope.launch ( Dispatchers.Main ){
//            _list.value  = repository.getLimit(offset, limit)
//        }
//    }
    fun getLimit(offset: Int, limit: Int){
        viewModelScope.launch {
                _list.emit(repository.getLimit(offset, limit))
        }
    }

    fun getAll(){
        viewModelScope.launch(Dispatchers.Main) {
            repository.getAll().collect {
                _allList.value = it
            }
        }
    }

    fun setCachePathResource(path: String){ savedStateHandle.set(PATH_CACHE_RESOURCE, path) }
    fun getCachePathResource() = savedStateHandle.get<String>(PATH_CACHE_RESOURCE)

    fun setPrivatePathResource(path: String){ savedStateHandle.set(PATH_PRIVATE_RESOURCE, path) }
    fun getPrivatePathResource() = savedStateHandle.get<String>(PATH_PRIVATE_RESOURCE)

    fun setInit(path: String){ savedStateHandle.set(PATH_PRIVATE_BEHAVIOR, path) }
    fun getPrivatePathBehavior() = savedStateHandle.get<String>(PATH_PRIVATE_BEHAVIOR)

    fun setCachePathBehavior(path: String){ savedStateHandle.set(PATH_CACHE_BEHAVIOR, path) }
    fun getCachePathBehavior() = savedStateHandle.get<String>(PATH_CACHE_BEHAVIOR)

    fun setFlagAdMob(value: String){ savedStateHandle.set(FLAG_ADMOB, value) }
    fun getFlagAdMob() = savedStateHandle.get<String>(FLAG_ADMOB)

    fun setFlagTrial(flag: Boolean){ savedStateHandle.set(FLAG_TRIAL, flag) }
    fun getFlagTrial() = savedStateHandle.get<Boolean>(FLAG_TRIAL)
}