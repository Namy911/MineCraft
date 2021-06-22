package com.example.minecraft.ui.spash

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.*
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.repository.SplashScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed class SplashScreenState {
    object Loading : SplashScreenState()
    object LoadComplete : SplashScreenState()
    data class Error(val error: String) : SplashScreenState()
}

@HiltViewModel
class SplashScreenMotor @Inject constructor(
        private val repository: SplashScreenRepository,
        private val savedStateHandle: SavedStateHandle
    ): ViewModel() {

    private val stage1:Int = Random.nextInt(20, 65)
    private val stage2:Int = Random.nextInt(66, 85)

    companion object{
        const val LOADING_STATE = "ui.splash.loading"
        const val START_CONTENT_STATE = "ui.splash.content.start"
    }
    private val _fulList: MutableLiveData<SplashScreenState> by lazy { MutableLiveData<SplashScreenState>() }
    val fulList: LiveData<SplashScreenState> get() = _fulList


//    private val _offlineList: MutableLiveData<List<AddonModel>> by lazy { MutableLiveData<List<AddonModel>>() }
//    val offlineList: LiveData<List<AddonModel>> get() = _offlineList

//    private val _offlineList = MutableSharedFlow<List<AddonModel>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//    val offlineList: SharedFlow<List<AddonModel>> get() = _offlineList.asSharedFlow()

    init {
        getFulData()
//        getOfflineList()
    }

    fun setLoadingNumber(){
        savedStateHandle.set(LOADING_STATE, stage1)
    }
    fun getLoadingNumber() = savedStateHandle.get<Int>(LOADING_STATE)

    fun setStartContentNumber(){
        savedStateHandle.set(START_CONTENT_STATE, stage2)
    }
    fun getStartContentNumber() = savedStateHandle.get<Int>(START_CONTENT_STATE)
    // Get all data from server
    private fun getFulData(){
        viewModelScope.launch {
            _fulList.value = SplashScreenState.Loading
            val response = repository.getDataFromServer()
            if (response.isSuccessful){
                response.body()?.let { entity ->
                    // Insert, from initialization screen
                    val model = entity.addon.map { it.convertToAddonModel(it) }
                    repository.insertFulData(model)
                    _fulList.value = SplashScreenState.LoadComplete
                }
            } else {
                _fulList.value = SplashScreenState.Error(response.message())
            }
        }
    }
    //
//    private fun getOfflineList(){
//        viewModelScope.launch(Dispatchers.Main) {
////            _offlineList.emitAll(repository.getOfflineData())
//             repository.getOfflineData().collect {
//                 _offlineList.value = it
//            }
//        }
//    }
}