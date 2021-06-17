package com.example.minecraft

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.minecraft.databinding.MainActivityBinding
import com.example.minecraft.ui.main.MainFragment
import com.example.minecraft.ui.main.MainViewModel
import com.example.minecraft.ui.settings.SettingsFragmentDirections
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MainActivity"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private lateinit var navController: NavController

    lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()

        mAdView.loadAd(adRequest)
        // Setup Navigation
        setupToolBartTitle()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowHomeEnabled(false)
        }

        // Setup navigation with colliders
        binding.colliderBackArrow.setOnClickListener { super.onBackPressed() }
        binding.colliderSettings.setOnClickListener { setActionBarSettings() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> {
                    if (binding.homeIndicator.isVisible){
                        binding.homeIndicator.visibility = View.INVISIBLE
                        binding.colliderBackArrow.visibility = View.GONE
                    }
                    binding.toolBarSettings.visibility = View.VISIBLE
                }
                R.id.detailFragment -> {
                    binding.homeIndicator.visibility = View.VISIBLE
                    binding.colliderBackArrow.visibility = View.VISIBLE
                    binding.toolBarSettings.visibility = View.VISIBLE
                }
                R.id.settingsFragment -> {
                    binding.homeIndicator.visibility = View.VISIBLE
                    binding.colliderBackArrow.visibility = View.VISIBLE
                    binding.toolBarSettings.visibility = View.INVISIBLE
                }
                R.id.settingsDetailFragment -> {
                    binding.homeIndicator.visibility = View.VISIBLE
                    binding.colliderBackArrow.visibility = View.VISIBLE
                }
                else -> { }
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