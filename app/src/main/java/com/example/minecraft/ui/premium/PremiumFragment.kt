package com.example.minecraft.ui.premium

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.settings.SettingsDetailFragmentDirections
import com.example.minecraft.ui.settings.SettingsFragment
import com.example.minecraft.ui.settings.SettingsFragmentDirections
import com.example.minecraft.ui.util.AppUtil
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PremiumFragment : Fragment() {
    private var _binding: LayoutPremiumBinding? = null
    val binding get() = _binding!!
    //    @Inject
    lateinit var billingManager: BillingManager
    lateinit var appUtil: AppUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)

        appUtil = AppUtil()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val animBtn = AnimationUtils.loadAnimation(requireActivity(), R.anim.btn_premium)
        val animTxt = AnimationUtils.loadAnimation(requireActivity(), R.anim.txt_premium)

        billingManager = BillingManager(requireActivity()){
            requireActivity().finish()
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }

        binding.apply {
            btnPremium.animation = animBtn
            txtBtnTrial.animation = animTxt

            imgClose.setOnClickListener {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }

            btnPremium.setOnClickListener {
                initBilling()
            }

            txtSubscription.setOnClickListener {
                findNavController().navigate(PremiumFragmentDirections.settingsPremiumDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.policy), getString(R.string.subscription_info)))
            }

            txtTerms.setOnClickListener {
                findNavController().navigate(PremiumFragmentDirections.settingsPremiumDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.help), getString(R.string.terms_of_usage)))
            }

            txtPrivacy.setOnClickListener {
                findNavController().navigate(PremiumFragmentDirections.settingsPremiumDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.policy), getString(R.string.txt_privacy_policy)))
            }
        }

        Glide.with(this).load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif")
            .into(binding.gifStub)
    }

    private fun initBilling(){
        billingManager.startConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}