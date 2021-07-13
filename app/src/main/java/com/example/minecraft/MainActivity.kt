package com.example.minecraft

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.minecraft.databinding.ActivityMainBinding
import com.example.minecraft.ui.main.DetailFragment
import com.example.minecraft.ui.main.MainFragmentDirections
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.AppUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint

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

    private var interstitialAd: InterstitialAd? = null
    private var flagInterstitialAd = false

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

        val flagDest = intent.getIntExtra(EXTRA_FLAG_DIR_NAME, -1)

        if (flagDest == FLAG_DEST_BILLING_FRAGMENT) {
            navController.navigate(MainFragmentDirections.trialFragment(FLAG_DEST_BILLING_FRAGMENT))
        } else {
            setupToolBartTitle()
        }
        // Setup navigation with colliders
        binding.apply {
            colliderBackArrow.setOnClickListener { super.onBackPressed() }
            colliderSettings.setOnClickListener { setActionBarSettings() }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> {
                    binding.apply {
                        if (homeIndicator.isVisible) {
                            homeIndicator.visibility = View.INVISIBLE
                            colliderBackArrow.visibility = View.GONE
                        }
                        toolBarSettings.visibility = View.VISIBLE
                        colliderSettings.visibility = View.VISIBLE
                    }
                    flagInterstitialAd = true
                }
                R.id.detailFragment -> {
                    binding.apply {
                        colliderSettings.visibility = View.VISIBLE
                        homeIndicator.visibility = View.VISIBLE
                        colliderBackArrow.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.VISIBLE
                    }
                    flagInterstitialAd = true
                }
                R.id.settingsFragment -> {
                    binding.apply {
                        colliderSettings.visibility = View.VISIBLE
                        homeIndicator.visibility = View.VISIBLE
                        colliderBackArrow.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.GONE
                    }
                    flagInterstitialAd = true
                }
                R.id.settingsDetailFragment -> {
                    if (flagDest != FLAG_DEST_BILLING_FRAGMENT) {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                            colliderBackArrow.visibility = View.VISIBLE
                        }
                    } else {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                            colliderBackArrow.visibility = View.VISIBLE
                            toolBarSettings.visibility = View.GONE
                            colliderSettings.visibility = View.GONE
                        }
                    }
                    flagInterstitialAd = true
                }
                R.id.trialFragment -> {
                    if (flagDest != FLAG_DEST_BILLING_FRAGMENT) {
                        binding.apply {
                            colliderSettings.visibility = View.VISIBLE
                            colliderSettings.visibility = View.VISIBLE
                            homeIndicator.visibility = View.GONE
                            colliderBackArrow.visibility = View.GONE
                            toolBarSettings.visibility = View.GONE
                        }
                    } else {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                            colliderBackArrow.visibility = View.VISIBLE
                            homeIndicator.visibility = View.GONE
                            colliderBackArrow.visibility = View.GONE
                            toolBarSettings.visibility = View.GONE
                            colliderSettings.visibility = View.GONE
                        }
                    }
                    flagInterstitialAd = false
                }
                else -> { }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (interstitialAd != null && flagInterstitialAd) {
            interstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            interstitialAd?.show(this)
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
        }
    }


    override fun onPause() {
        super.onPause()
        InterstitialAd.load(this,
            AppUtil.INTERSTIAL_AD_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(DetailFragment.TAG, adError.message)
                interstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@MainActivity.interstitialAd = interstitialAd
            }
        })
    }

    private fun setActionBarSettings(){
            val action = NavGraphDirections.globalSettingsFragment()
            navController.navigate(action)
    }

    fun setupToolBartTitle(title: String = getString(R.string.app_name)){
        binding.txtBar.text = title
    }
}