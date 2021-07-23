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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "MainActivity"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object{
        const val FLAG_DEST_BILLING_FRAGMENT = 2
        const val FLAG_DEST_SPLASH_TO_MAIN = 1
        const val FLAG_DEST_MAIN_FRAGMENT = 3
        const val EXTRA_FLAG_DIR_NAME = "activity.main.flag"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    var appOpenAd: AppOpenAd? = null
    private var flagAppOpenAd = false

    private lateinit var appSharedPrefManager: AppSharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Setup Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowHomeEnabled(false)
        }

        appSharedPrefManager = AppSharedPreferencesManager(this)

        val flagDest = intent.getIntExtra(EXTRA_FLAG_DIR_NAME, -1)
        /** Setup navigation
         *  @param [flagDest] flag to hide settings ico
         *  @param [flagAppOpenAd] flag enable AD
         *  @param [homeIndicator], [toolBarSettings], [toolbar] setup visibility on fragments(custom toolbar)
         */
        binding.apply {
            homeIndicator.setOnClickListener { super.onBackPressed() }
            toolBarSettings.setOnClickListener { setActionBarSettings() }
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.splashScreenFragment -> { toolbar.visibility = View.GONE }
                    R.id.mainFragment -> {
                        toolbar.visibility = View.VISIBLE
                        if (homeIndicator.isVisible) {
                            homeIndicator.visibility = View.INVISIBLE
                        }
                        toolBarSettings.visibility = View.VISIBLE
                        flagAppOpenAd = true
                    }
                    R.id.detailFragment -> {
                        homeIndicator.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.VISIBLE
                        flagAppOpenAd = true
                    }
                    R.id.settingsFragment -> {
                        homeIndicator.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.GONE
                        flagAppOpenAd = true
                    }
                    R.id.settingsDetailFragment -> {
                        homeIndicator.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.GONE
                        flagAppOpenAd = true
                    }
                    R.id.subscriptionFragment -> {
                        toolbar.visibility = View.VISIBLE
                        homeIndicator.visibility = View.GONE
                        toolBarSettings.visibility = View.GONE
                        flagAppOpenAd = false
                    }
                }
            }
        }
    }

    // Config Ad: Ap open, load ad
//    override fun onPause() {
//        super.onPause()
//        lifecycleScope.launchWhenResumed {
//            appSharedPrefManager.billingAdsSate.collectLatest { prefState ->
//                if (!prefState) {
//                    AppOpenAd.load(
//                        this@MainActivity, AppUtil.APP_OPEN_UNIT_ID, AdRequest.Builder().build(),
//                        1, object : AppOpenAd.AppOpenAdLoadCallback() {
//                            override fun onAdLoaded(appOpenAd: AppOpenAd) {
//                                super.onAdLoaded(appOpenAd)
//                                this@MainActivity.appOpenAd = appOpenAd
//                            }
//
//                            override fun onAdFailedToLoad(error: LoadAdError) {
//                                super.onAdFailedToLoad(error)
//                                appOpenAd = null
//                            }
//                        })
//                }
//            }
//        }
//    }
//    /**
//     * check if have subscription
//     * if have disable ad
//     */
//    override fun onResume() {
//        super.onResume()
//        lifecycleScope.launchWhenResumed {
//            appSharedPrefManager.billingAdsSate.collectLatest { prefState ->
//                if (!prefState) {
//                    appOpenAd?.let { ad ->
//                        if (flagAppOpenAd) {
//                            val fullScreenContentCallback = object : FullScreenContentCallback() {
//                                override fun onAdDismissedFullScreenContent() {
//                                    this@MainActivity.appOpenAd = null
//                                }
//
//                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                    this@MainActivity.appOpenAd = null
//                                }
//                            }
//                            ad.fullScreenContentCallback = fullScreenContentCallback
//                            ad.show(this@MainActivity)
//                        }
//                    }
//                }
//            }
//        }
//    }

    private fun setActionBarSettings(){
            val action = NavGraphDirections.globalSettingsFragment()
            navController.navigate(action)
    }

    fun setupToolBartTitle(title: String = getString(R.string.app_name)){
        binding.txtBar.text = title
    }
}