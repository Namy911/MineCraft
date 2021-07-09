package com.example.minecraft

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.databinding.ActivityPremiumBinding
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PremiumActivity"
@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
        // Setup navigation with colliders
        binding.apply {
            colliderBackArrow.setOnClickListener { super.onBackPressed() }
            colliderSettings.setOnClickListener { setActionBarSettings() }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.premiumFragment -> {
                    binding.apply {
                        toolbar.visibility = View.GONE
                        homeIndicator.visibility = View.INVISIBLE
                        colliderBackArrow.visibility = View.INVISIBLE
                    }
                }
                R.id.settingsPremiumDetailFragment -> {
                    binding.apply {
                        toolbar.visibility = View.VISIBLE
                        homeIndicator.visibility = View.VISIBLE
                        colliderBackArrow.visibility = View.VISIBLE
                        toolBarSettings.visibility = View.INVISIBLE
                    }
                }
                else -> {
                }
            }
        }
    }

    fun setupToolBartTitle(title: String = getString(R.string.app_name)) {
        binding.txtBar.text = title
    }

    private fun setActionBarSettings() {
        val action = NavGraphDirections.globalSettingsFragment()
        navController.navigate(action)
    }
}