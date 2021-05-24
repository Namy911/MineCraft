package com.example.minecraft.ui.spash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.data.network.AddonEntity
import com.example.minecraft.data.network.util.Resource
import com.example.minecraft.repository.SplashScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed class SplashScreenState {
    object Loading : SplashScreenState()
    object LoadComplete : SplashScreenState()
    data class Error(val error: String) : SplashScreenState()
}

@HiltViewModel
class SplashScreenMotor @Inject constructor(private val repository: SplashScreenRepository): ViewModel() {

    private val _fulList: MutableLiveData<SplashScreenState> by lazy { MutableLiveData<SplashScreenState>() }
    val fulList: LiveData<SplashScreenState> get() = _fulList

    init {
        getFulData()
    }

    // Get all data from server
    private fun getFulData(){
        viewModelScope.launch {
            _fulList.value = SplashScreenState.Loading
//            delay(3000)
            val response = repository.getData()
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
}