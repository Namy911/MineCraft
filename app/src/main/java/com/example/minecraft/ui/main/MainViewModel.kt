package com.example.minecraft.ui.main

import androidx.lifecycle.*
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.repository.MainRepository
import com.example.minecraft.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"
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
//        val PATH_SD_BEHAVIOR: String = "ui.main.path.sd.behavior"
    }

    private val _list: MutableLiveData<Event<List<AddonModel>>> by lazy { MutableLiveData<Event<List<AddonModel>>>() }
    val list: LiveData<Event<List<AddonModel>>> = _list

    fun getLimit(offset: Int, limit: Int){
        viewModelScope.launch ( Dispatchers.Main ){
            _list.value  = Event(repository.getLimit(offset, limit))
        }
    }

    fun setCachePathResource(path: String){ savedStateHandle.set(PATH_CACHE_RESOURCE, path) }
    fun getCachePathResource() = savedStateHandle.get<String>(PATH_CACHE_RESOURCE)

    fun setPrivatePathResource(path: String){ savedStateHandle.set(PATH_PRIVATE_RESOURCE, path) }
    fun getPrivatePathResource() = savedStateHandle.get<String>(PATH_PRIVATE_RESOURCE)

    fun setPrivatePathBehavior(path: String){ savedStateHandle.set(PATH_PRIVATE_BEHAVIOR, path) }
    fun getPrivatePathBehavior() = savedStateHandle.get<String>(PATH_PRIVATE_BEHAVIOR)

    fun setCachePathBehavior(path: String){ savedStateHandle.set(PATH_CACHE_BEHAVIOR, path) }
    fun getCachePathBehavior() = savedStateHandle.get<String>(PATH_CACHE_BEHAVIOR)

    fun setFlagAdMob(value: String){ savedStateHandle.set(FLAG_ADMOB, value) }
    fun getFlagAdMob() = savedStateHandle.get<String>(FLAG_ADMOB)

    fun setFlagTrial(flag: Boolean){ savedStateHandle.set(FLAG_TRIAL, flag) }
    fun getFlagTrial() = savedStateHandle.get<Boolean>(FLAG_TRIAL)
}