package com.example.minecraft.ui.splash

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_SPLASH_MAIN
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentSplashScreenBinding
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import com.example.minecraft.ui.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.Exception

private const val TAG = "SplashFragment"
@AndroidEntryPoint
class SplashScreenFragment : Fragment(), NetworkUtil {

    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = checkNotNull(_binding) {"binding isn't initialized"}

    private val motor: SplashScreenMotor by viewModels()

    private lateinit var appSharedPrefManager: AppSharedPreferencesManager

    private var billingManager: BillingManager? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { observeState() }
        override fun onLost(network: Network) { dialogNoInternet() }
    }
    var dialogNetwork: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(requireActivity()){ }
        // Set pref state from billing subscription
        billingManager?.setSubsState()
        appSharedPrefManager = AppSharedPreferencesManager(requireActivity())
        // Ful screen window
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        // Dialog chooser, if don't internet connection
        if (checkInternetConnection(requireContext())) { observeState() }
        else { dialogNoInternet() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplashScreenBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        if (binding.lottie.isAnimating){
            binding.lottie.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!binding.lottie.isAnimating){
            binding.lottie.playAnimation()
        }
    }
    // Progress bar config, main logic
    private fun observeState(){
        lifecycleScope.launch {
            motor.fulList.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collectLatest { viewState ->
                    binding.apply {
                        when (viewState) {
                            SplashScreenState.Loading -> {
                                motor.setLoadingNumber()
                                progressBar.progress = motor.getLoadingNumber()!!
                                textView3.text = "${motor.getLoadingNumber()!!} %"
                            }
                            SplashScreenState.LoadComplete -> {
                                motor.setStartContentNumber()
                                progressBar.progress = motor.getStartContentNumber()!!
                                textView3.text = "${motor.getStartContentNumber()!!} %"
                                navigateToMainScreen()
                                progressBar.progress = 100
                                textView3.text = "100 %"
                            }
                            is SplashScreenState.Error -> {}
                        }
                    }
                }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    /**
     * See dialog chooser
     * [PositiveButton] close activity an redirect to Settings
     * [NegativeButton] offline version of opp
     * [NeutralButton] close opp
     */
    private fun dialogNoInternet(){
        val builder = AlertDialog.Builder(requireContext()).apply {
            setTitle(getString(R.string.dialog_title_no_internet))
            setMessage(R.string.msg_no_internet)
            setPositiveButton(R.string.btn_snack_connect) { dialog, _ ->
                dialog.dismiss()
                requireActivity().finish()
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            setNegativeButton(getString(R.string.dialog_no_internet_go_next)) { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(SplashScreenFragmentDirections.mainFragment())
            }
            setNeutralButton(getString(R.string.dialog_no_internet_leave)) { dialog, _ -> dialog.dismiss(); requireActivity().finish() }
        }
        dialogNetwork = builder.create()
        dialogNetwork?.show()

        networkState()
    }
    /**
     * Listener to have internet state
     * [onAvailable] close dialog, see progress an redirect
     * [dialogNoInternet] see dialog to choose action
     */
    private fun networkState() {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else{
            val networkChangeFilter = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(networkChangeFilter, networkCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception){ }
        dialogNetwork?.dismiss()
    }
    /**
     * Redirect if don't have subscription
     * @param [FLAG_DEST_SPLASH_TO_MAIN], subscriptionFragment([FLAG_DEST_SPLASH_MAIN]), id to know which fragment was by previously
     * [FLAG_DEST_SPLASH_MAIN] redirect to [BillingFragment],
     * path: SplashScreenFragment to subscriptionFragment, don't have subscription
     * [mainFragment] redirect to [MainFragment], have subscription
     */
    private fun navigateToMainScreen() {
        if (BillingManager.BILLING_FLAG_STATE) {
            findNavController().navigate(SplashScreenFragmentDirections.subscriptionFragment(FLAG_DEST_SPLASH_MAIN))
        } else {
            findNavController().navigate(SplashScreenFragmentDirections.mainFragment())
        }
    }
}