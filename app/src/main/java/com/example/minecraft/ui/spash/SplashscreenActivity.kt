package com.example.minecraft.ui.spash

import android.animation.ValueAnimator
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.ActivitySplashscreenBinding
import com.example.minecraft.PremiumActivity
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Exception


private const val TAG = "SplashscreenActivity"
@AndroidEntryPoint
class SplashscreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashscreenBinding

    private val motor: SplashScreenMotor by viewModels()

    lateinit var appSharedPrefManager: AppSharedPreferencesManager
    lateinit var billingManager: BillingManager
    private var prefState = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(this){
//            finish()
//            startActivity(Intent(this@SplashscreenActivity, MainActivity::class.java))
        }
        billingManager.startConnection()

        appSharedPrefManager = AppSharedPreferencesManager(this)
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
        } else {
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
    lifecycleScope.launch {
        motor.fulList.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collectLatest { viewState ->
                when (viewState) {
                    SplashScreenState.Loading -> {
                        motor.setLoadingNumber()
                        binding.progressBar.progress = motor.getLoadingNumber()!!
                        binding.textView3.text = "${motor.getLoadingNumber()!!} %"
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
    }
    // Dialog chooser
    private fun dialogNoInternet(){
        val builder = AlertDialog.Builder(this).apply {
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
        }

        val dialog = builder.create()

        try {
            builder.create().show()
        } catch (e : Exception){
            Log.d(TAG, "dialogNoInternet: ")
        }
        networkState(dialog)
    }

    private fun networkState(dialog: AlertDialog) {
        val connectivityManager = this.getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    observeState()
                    dialog.dismiss()
                }
                override fun onLost(network: Network) {
                    dialogNoInternet()
                }
            })
        }
    }
    //
    private fun getConnection(): Boolean{
        val connectivityManager = this@SplashscreenActivity.getSystemService(ConnectivityManager::class.java)
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    // Check if user have trial and redirect
    private fun navigateToMainScreen() {

        val result = billingManager.checkItemAvailability()
        Log.d(TAG, "navigateToMainScreen: ${result} kk")
//        lifecycleScope.launchWhenCreated {
//            appSharedPrefManager.billingAdsSate.collect {
//                if (!it) {
//                    startActivity(Intent(this@SplashscreenActivity, PremiumActivity::class.java))
//                } else {
//                    startActivity(Intent(this@SplashscreenActivity, MainActivity::class.java))
//                }
//            }
//        }

        if (result) {
//        if (BillingManager.BILLING_FLAG_STATE) {
            finish()
            startActivity(Intent(this@SplashscreenActivity, PremiumActivity::class.java))
            CoroutineScope(SupervisorJob()).launch { appSharedPrefManager.setBillingAdsSate(false) }
        } else {
            finish()
            CoroutineScope(SupervisorJob()).launch { appSharedPrefManager.setBillingAdsSate(true) }
            startActivity(Intent(this@SplashscreenActivity, MainActivity::class.java))
        }
    }
}