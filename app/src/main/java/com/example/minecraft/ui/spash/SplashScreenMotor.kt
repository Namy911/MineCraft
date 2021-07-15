package com.example.minecraft.ui.spash

import androidx.lifecycle.*
import com.example.minecraft.repository.SplashScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
class SplashScreenMotor
@Inject constructor(
    private val repository: SplashScreenRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stage1:Int = Random.nextInt(20, 65)
    private val stage2:Int = Random.nextInt(66, 85)

    companion object{
        const val LOADING_STATE = "ui.splash.loading"
        const val START_CONTENT_STATE = "ui.splash.content.start"
    }

    private val _fulList = MutableStateFlow<SplashScreenState>(SplashScreenState.Loading)
    val fulList: StateFlow<SplashScreenState> = _fulList.asStateFlow()

    init {
        getFullData()
    }

    fun setLoadingNumber(){ savedStateHandle.set(LOADING_STATE, stage1) }
    fun getLoadingNumber() = savedStateHandle.get<Int>(LOADING_STATE)

    fun setStartContentNumber(){ savedStateHandle.set(START_CONTENT_STATE, stage2) }
    fun getStartContentNumber() = savedStateHandle.get<Int>(START_CONTENT_STATE)

    // Get all data from server
    private fun getFullData(){
        viewModelScope.launch {
            repository.getDataFromServer()
                .distinctUntilChanged()
                .collectLatest { _fulList.value = it }
        }
    }
}