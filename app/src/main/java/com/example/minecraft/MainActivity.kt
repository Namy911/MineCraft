package com.example.minecraft

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.minecraft.databinding.ActivityMainBinding
import com.example.minecraft.ui.main.MainFragmentDirections
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MainActivity"
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object{
        const val FLAG_DEST_BILLING_FRAGMENT = 2
        const val FLAG_DEST_BILLING_MAIN = 1
        const val EXTRA_FLAG_DIR_NAME = "activity.main.flag"
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    lateinit var appSharedPrefManager: AppSharedPreferencesManager

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
                }
                R.id.detailFragment -> {
                    binding.apply {
                        colliderSettings.visibility = View.VISIBLE
                        homeIndicator.visibility = View.VISIBLE
                        colliderBackArrow.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.VISIBLE
                    }
                }
                R.id.settingsFragment -> {
                    binding.apply {
                        colliderSettings.visibility = View.VISIBLE
                        homeIndicator.visibility = View.VISIBLE
                        colliderBackArrow.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.GONE
                    }
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
                }
                R.id.trialFragment -> {
                    if (flagDest != FLAG_DEST_BILLING_FRAGMENT) {
                        binding.apply {
                            colliderSettings.visibility = View.VISIBLE
                            colliderSettings.visibility = View.VISIBLE
                            homeIndicator.visibility = View.VISIBLE
                            colliderBackArrow.visibility = View.VISIBLE
                            toolBarSettings.visibility = View.GONE
                        }
                    } else {
                        binding.apply {
                            homeIndicator.visibility = View.VISIBLE
                            colliderBackArrow.visibility = View.VISIBLE
                            toolBarSettings.visibility = View.GONE
                            colliderSettings.visibility = View.GONE
                        }
                    }
                }
                else -> {
                }
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