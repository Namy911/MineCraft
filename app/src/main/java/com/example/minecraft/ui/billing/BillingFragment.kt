package com.example.minecraft.ui.billing

import android.os.Bundle
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
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_BILLING_FRAGMENT
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_SPLASH_TO_MAIN
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_MAIN_FRAGMENT
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.settings.SettingsFragmentDirections
import com.example.minecraft.ui.util.AppUtil
import com.example.minecraft.ui.util.BillingManager
import com.example.minecraft.ui.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BillingFragment : Fragment(), NetworkUtil {
    private val TAG = "BillingFragment"
    private var _binding: LayoutPremiumBinding? = null
    private val binding get() = checkNotNull(_binding) {"binding isn't initialized"}

    private val args: BillingFragmentArgs by navArgs()

    lateinit var appUtil: AppUtil

    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appUtil = AppUtil()

        billingManager = BillingManager(requireActivity()) { closeNavigation() }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_trial))
        // Background gif setup
        Glide.with(requireActivity())
            .load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif")
            .into(binding.gifStub)
        // Animation from button with text [Subscription]
        val animBtn = AnimationUtils.loadAnimation(requireActivity(), R.anim.btn_premium)
        val animTxt = AnimationUtils.loadAnimation(requireActivity(), R.anim.txt_premium)

        binding.apply {
            btnPremium.setOnClickListener {
                if (checkInternetConnection(requireContext())) {
                    billingManager.startConnection()
                }else{
                    Toast.makeText(requireActivity(), resources.getString(R.string.msg_no_internet), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            btnPremium.animation = animBtn
            txtBtnTrial.animation = animTxt

            imgClose.setOnClickListener { closeNavigation() }
            // Setup navigation from text[Terms of Usage], [Privacy Policy]
            txtTerms.setOnClickListener {
                findNavController().navigate(
                    SettingsFragmentDirections.settingsDetailFragment(
                        appUtil.readTextFile(requireActivity(), R.raw.help),
                        getString(R.string.terms_of_usage)
                    )
                )
            }
            // Navigate to detail with content [raw resource]
            txtPrivacy.setOnClickListener {
                findNavController().navigate(
                    SettingsFragmentDirections.settingsDetailFragment(
                        appUtil.readTextFile(requireActivity(), R.raw.policy),
                        getString(R.string.txt_privacy_policy)
                    )
                )
            }
        }
    }
    /**
     * Navigation [close button], navigate back
     * [FLAG_DEST_SPLASH_TO_MAIN] previously destination [SplashScreenFragment]
     * [FLAG_DEST_BILLING_FRAGMENT], [FLAG_DEST_MAIN_FRAGMENT]
     * previously destination [DetailFragment] or [MainFragment]
     */
    private fun closeNavigation(){
        when (args.flagDest) {
            FLAG_DEST_SPLASH_TO_MAIN -> { findNavController().navigate(BillingFragmentDirections.mainFragment()) }
            FLAG_DEST_BILLING_FRAGMENT, FLAG_DEST_MAIN_FRAGMENT -> { findNavController().popBackStack() }
            else -> { throw RuntimeException("Bad flag no destination ")}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingManager.endConnection()
        _binding = null
    }

    fun setupToolBartTitle(title: String){ (activity as MainActivity).setupToolBartTitle(title) }
}