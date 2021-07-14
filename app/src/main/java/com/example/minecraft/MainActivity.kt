package com.example.minecraft

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.minecraft.databinding.ActivityMainBinding
import com.example.minecraft.ui.main.MainFragmentDirections
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.AppUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object{
        const val FLAG_DEST_BILLING_FRAGMENT = 2
        const val FLAG_DEST_MAIN_FRAGMENT = 1
        const val EXTRA_FLAG_DIR_NAME = "activity.main.flag"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    var appOpenAd: AppOpenAd? = null
    private var flagAppOpenAd = false
    private var prefState = false

    private lateinit var appSharedPrefManager: AppSharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Setup Navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowHomeEnabled(false)
        }

        appSharedPrefManager = AppSharedPreferencesManager(this)
        lifecycleScope.launch {
            appSharedPrefManager.billingAdsSate.collectLatest { state ->
                prefState = state
            }
        }

        val flagDest = intent.getIntExtra(EXTRA_FLAG_DIR_NAME, -1)

        if (flagDest == FLAG_DEST_BILLING_FRAGMENT) {
            navController.navigate(MainFragmentDirections.trialFragment(FLAG_DEST_BILLING_FRAGMENT))
        } else {
            setupToolBartTitle()
        }
        // Setup navigation with colliders
        binding.apply {
            homeIndicator.setOnClickListener { super.onBackPressed() }
            toolBarSettings.setOnClickListener { setActionBarSettings() }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> {
                    binding.apply {
                        if (homeIndicator.isVisible) {
                            homeIndicator.visibility = View.INVISIBLE
                        }
                        toolBarSettings.visibility = View.VISIBLE
                    }
                    flagAppOpenAd = true
                }
                R.id.detailFragment -> {
                    binding.apply {
                        homeIndicator.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.VISIBLE
                    }
                    flagAppOpenAd = true
                }
                R.id.settingsFragment -> {
                    binding.apply {
                        homeIndicator.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.GONE
                    }
                    flagAppOpenAd = true
                }
                R.id.settingsDetailFragment -> {
                    if (flagDest != FLAG_DEST_BILLING_FRAGMENT) {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                        }
                    } else {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                            toolBarSettings.visibility = View.GONE
                        }
                    }
                    flagAppOpenAd = true
                }
                R.id.trialFragment -> {
                    if (flagDest != FLAG_DEST_BILLING_FRAGMENT) {
                        binding.apply {
                            homeIndicator.visibility = View.GONE
                            toolBarSettings.visibility = View.GONE
                        }
                    } else {
                        binding.apply {
                            homeIndicator.visibility = View.GONE
                            toolBarSettings.visibility = View.GONE
                        }
                    }
                    flagAppOpenAd = false
                }
                else -> { }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!prefState) {
            AppOpenAd.load(this, AppUtil.APP_OPEN_UNIT_ID, AdRequest.Builder().build(), 1, object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(appOpenAd: AppOpenAd) {
                        super.onAdLoaded(appOpenAd)
                        this@MainActivity.appOpenAd = appOpenAd
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        appOpenAd = null
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        appOpenAd?.let { ad ->
        if (flagAppOpenAd) {
            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    this@MainActivity.appOpenAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    this@MainActivity.appOpenAd = null
                }
            }
                ad.fullScreenContentCallback = fullScreenContentCallback
                ad.show(this)
            }
        }
    }

    private fun setActionBarSettings(){
            val action = NavGraphDirections.globalSettingsFragment()
            navController.navigate(action)
    }

    fun setupToolBartTitle(title: String = getString(R.string.app_name)){
        binding.txtBar.text = title
    }
}