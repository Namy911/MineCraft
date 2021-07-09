package com.example.minecraft.ui.billing

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.main.MainViewModel
import com.example.minecraft.ui.settings.SettingsFragment
import com.example.minecraft.ui.settings.SettingsFragmentDirections
import com.example.minecraft.ui.util.AppUtil
import com.example.minecraft.ui.util.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class BillingFragment : Fragment() {
    private val TAG = "BillingFragment"
    private var _binding: LayoutPremiumBinding? = null
    private val binding get() = _binding!!

    private val args: BillingFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

//    @Inject
    lateinit var appUtil: AppUtil

    lateinit var billingManager: BillingManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUtil = AppUtil()

        billingManager = BillingManager(requireActivity()) {
            val destId = args.flagDest
            if (destId == 1){
                requireActivity().finish()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }else {
                findNavController().popBackStack()
            }
        }
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
                billingManager.startConnection()
            }
            btnPremium.animation = animBtn
            txtBtnTrial.animation = animTxt
            imgClose.visibility = View.GONE

            txtSubscription.setOnClickListener {
                Log.d(TAG, "onViewCreated: ")
                findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.policy), getString(R.string.subscription_info)))
            }

            txtTerms.setOnClickListener {
                Log.d(TAG, "onViewCreated: ")
                findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.help), getString(R.string.terms_of_usage)))
            }

            txtPrivacy.setOnClickListener {
                Log.d(TAG, "onViewCreated: ")
                findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(appUtil.readTextFile(requireActivity(), R.raw.policy), getString(R.string.txt_privacy_policy)))
            }
        }

//        (activity as MainActivity).disableAd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        billingManager.endConnection()
    }

    fun setupToolBartTitle(title: String){ (activity as MainActivity).setupToolBartTitle(title) }
}