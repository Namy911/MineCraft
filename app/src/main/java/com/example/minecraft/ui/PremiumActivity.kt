package com.example.minecraft.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PremiumActivity"
@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: LayoutPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPremium.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
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

    @Inject
    fun initBilling(trialManager: TrialManager){
        trialManager.startConnection()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}