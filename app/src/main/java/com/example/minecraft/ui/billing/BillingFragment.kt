package com.example.minecraft.ui.billing

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.settings.SettingsFragmentDirections
import com.example.minecraft.ui.spash.SplashScreenFragmentDirections
import com.example.minecraft.ui.util.AppUtil
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class BillingFragment : Fragment() {
    private val TAG = "BillingFragment"
    private var _binding: LayoutPremiumBinding? = null
    private val binding get() = _binding!!

    private val args: BillingFragmentArgs by navArgs()

    lateinit var appUtil: AppUtil

    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUtil = AppUtil()

        billingManager = BillingManager(requireActivity()) { closeNavigation() }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_trial))

        Glide.with(requireActivity()).load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif").into(binding.gifStub)

        val animBtn = AnimationUtils.loadAnimation(requireActivity(), R.anim.btn_premium)
        val animTxt = AnimationUtils.loadAnimation(requireActivity(), R.anim.txt_premium)

        binding.apply {
            btnPremium.setOnClickListener {
                if (checkInternetConnection()) {
                    billingManager.startConnection()
                }else{
                    Toast.makeText(requireActivity(), resources.getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                }
            }
            btnPremium.animation = animBtn
            txtBtnTrial.animation = animTxt

            imgClose.setOnClickListener {
                closeNavigation()
            }

            txtTerms.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.help), getString(R.string.terms_of_usage)))
            }

            txtPrivacy.setOnClickListener {
                findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.policy), getString(R.string.txt_privacy_policy)))
            }
        }
    }
    private fun closeNavigation(){
        val destId = args.flagDest
        if (destId == 1){
            findNavController().navigate(BillingFragmentDirections.mainFragment())
        }else {
            findNavController().popBackStack()
        }
    }

    private fun checkInternetConnection(): Boolean{
        val connectivityManager = requireActivity().applicationContext
            .getSystemService(ConnectivityManager::class.java)
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        billingManager.endConnection()
    }

    fun setupToolBartTitle(title: String){ (activity as MainActivity).setupToolBartTitle(title) }
}