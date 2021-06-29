package com.example.minecraft.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.ActivityPremiumBinding
import com.example.minecraft.ui.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var trialManager: TrialManager
//    private lateinit var navController: NavController

    companion object{
        val TRIAL_BOUGHT = stringPreferencesKey("trial_bought")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trialManager = TrialManager(this)

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPremium.setOnClickListener {
            lifecycleScope.launch { trialManager.setTrial() }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

       val animBtn = AnimationUtils.loadAnimation(this, R.anim.btn_premium)
       val animTxt = AnimationUtils.loadAnimation(this, R.anim.txt_premium)
       binding.btnPremium.animation = animBtn
       binding.txtBtnTrial.animation = animTxt

        binding.imgClose.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        Glide.with(this).load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif").into(binding.gifStub)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}