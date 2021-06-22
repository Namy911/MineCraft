package com.example.minecraft.ui.spash

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.ActivitySplashscreenBinding
import com.example.minecraft.ui.PremiumActivity
import com.example.minecraft.ui.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import java.lang.Exception
import java.sql.ResultSetMetaData
import kotlin.random.Random


private const val TAG = "SplashscreenActivity"
@AndroidEntryPoint
class SplashscreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashscreenBinding

    private val motor: SplashScreenMotor by viewModels()

    private lateinit var trialManager: TrialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trialManager = TrialManager(this)
        // Ful screen window
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        binding = ActivitySplashscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Dialog chooser, if don't internet connection
        if (getConnection()) {
            observeState()
        }
        else {
            dialogNoInternet()
        }

        binding.lottie.apply {
            imageAssetsFolder = "images/"
            repeatCount = ValueAnimator.INFINITE
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.lottie.isAnimating){
            binding.lottie.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.lottie.isAnimating){
            binding.lottie.playAnimation()
        }
    }
    // Progress bar config, main logic
    private fun observeState(){
        motor.fulList.observe(this) { viewState ->
            when (viewState) {
                SplashScreenState.Loading -> {
                    motor.setLoadingNumber()
                    binding.progressBar.progress = motor.getLoadingNumber()!!
                    binding.textView3.text = "${motor.getLoadingNumber()!!} %"
                    Log.d(TAG, "Loading Data... ")
                }
                SplashScreenState.LoadComplete -> {
                    motor.setStartContentNumber()
                    binding.progressBar.progress = motor.getStartContentNumber()!!
                    binding.textView3.text = "${motor.getStartContentNumber()!!} %"
                    navigateToMainScreen()
                    binding.progressBar.progress = 100
                    binding.textView3.text = "100 %"
                }
                is SplashScreenState.Error -> { }
            }
        }
    }
    // Dialog chooser
    private fun dialogNoInternet(){
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.dialog_title_no_internet))
            setMessage(R.string.msg_no_internet)
            setPositiveButton(R.string.btn_snack_connect) { _, _ ->
                finish()
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            setNegativeButton(getString(R.string.dialog_no_internet_go_next)) { _, _ -> navigateToMainScreen() }
            setNeutralButton(getString(R.string.dialog_no_internet_leave)) { _, _ -> finish() }
            show()
        }
        checkInternetConnection()
        Log.d(TAG, "checkInternetConnection: no connect")
    }
    // Check if user connect internet self
    private fun checkInternetConnection(){
        lifecycleScope.launchWhenStarted {
            repeat(15) {
                delay(2000)
                val connected2 = getConnection()
                if (connected2) {
                    observeState()
                    return@launchWhenStarted
                }
            }
        }
    }

    private fun getConnection(): Boolean{
        val connectivityManager = this@SplashscreenActivity.getSystemService(ConnectivityManager::class.java)
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    // Check if user have trial and redirect
    private fun navigateToMainScreen(){
        lifecycleScope.launchWhenCreated {
            trialManager.trialFlow.collect { trial ->
                val intent = if (trial == TrialManager.TRIAL_NOT_EXIST){
                    Intent(this@SplashscreenActivity, PremiumActivity::class.java)
                } else{
                    Intent(this@SplashscreenActivity, MainActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
        }
    }
}