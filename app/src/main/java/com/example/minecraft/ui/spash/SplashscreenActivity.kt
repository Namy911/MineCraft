package com.example.minecraft.ui.spash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.navigation.NavController
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.ActivitySplashscreenBinding
import com.example.minecraft.databinding.MainActivityBinding
import com.example.minecraft.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "SplashscreenActivity"
@AndroidEntryPoint
class SplashscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashscreenBinding

    private val motor: SplashScreenMotor by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivitySplashscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        motor.fulList.observe(this){ viewState ->
            when(viewState){
                 SplashScreenState.Loading -> { Log.d(TAG, "Loading Data: ") }
                 SplashScreenState.LoadComplete -> { navigateToMainScreen() }
                 is SplashScreenState.Error -> { Log.d(TAG, "Error:  ${viewState.error}") }
            }
        }
    }

    private fun navigateToMainScreen(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}