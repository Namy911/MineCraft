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
import com.example.minecraft.databinding.PremiumActivityBinding
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PremiumActivity"
@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: PremiumActivityBinding
    private lateinit var navController: NavController

    //    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PremiumActivityBinding.inflate(layoutInflater)
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
        binding.colliderBackArrow.setOnClickListener { super.onBackPressed() }
        binding.colliderSettings.setOnClickListener { setActionBarSettings() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.premiumFragment -> {
                    binding.homeIndicator.visibility = View.INVISIBLE
                    binding.colliderBackArrow.visibility = View.INVISIBLE
                }
                R.id.settingsPremiumDetailFragment -> {
                    binding.homeIndicator.visibility = View.VISIBLE
                    binding.colliderBackArrow.visibility = View.VISIBLE
                    binding.toolBarSettings.visibility = View.INVISIBLE
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