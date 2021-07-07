package com.example.minecraft

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.MainActivity
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val TAG = "PremiumActivity"
@AndroidEntryPoint
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: LayoutPremiumBinding
//    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val animBtn = AnimationUtils.loadAnimation(this, R.anim.btn_premium)
        val animTxt = AnimationUtils.loadAnimation(this, R.anim.txt_premium)

        billingManager = BillingManager(this){
            finish()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.apply {
            btnPremium.animation = animBtn
            txtBtnTrial.animation = animTxt

            imgClose.setOnClickListener {
                val intent = Intent(this@PremiumActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            btnPremium.setOnClickListener {
                initBilling()
            }
        }

        Glide.with(this).load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif")
            .into(binding.gifStub)



    }

    private fun initBilling(){
        billingManager.startConnection()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}