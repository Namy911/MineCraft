package com.example.minecraft.ui.splash

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_SPLASH_TO_MAIN
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentSplashScreenBinding
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.BillingManager
import com.example.minecraft.ui.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Exception

private const val TAG = "SplashFragment"
@AndroidEntryPoint
class SplashScreenFragment : Fragment(), NetworkUtil {

    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = checkNotNull(_binding) {"binding isn't initialized"}

    private val motor: SplashScreenMotor by viewModels()

    private lateinit var appSharedPrefManager: AppSharedPreferencesManager

    lateinit var billingManager: BillingManager

    var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(requireActivity()){ }
        // Set pref state from billing subscription
        billingManager.setSubsState()
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
        job = lifecycleScope.launch {
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

    override fun onDetach() {
        super.onDetach()
        job?.cancel()
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
     * (try block to avoid bug with dialog)
     */
    private fun dialogNoInternet(){
        val builder = AlertDialog.Builder(requireContext()).apply {
            setTitle(getString(R.string.dialog_title_no_internet))
            setMessage(R.string.msg_no_internet)
            setPositiveButton(R.string.btn_snack_connect) { _, _ ->
                requireActivity().finish()
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            setNegativeButton(getString(R.string.dialog_no_internet_go_next)) { _, _ ->
                findNavController().navigate(SplashScreenFragmentDirections.mainFragment())
            }
            setNeutralButton(getString(R.string.dialog_no_internet_leave)) { _, _ -> requireActivity().finish() }
        }

        val dialog = builder.create()

        try { builder.create().show() }
        catch (e : Exception){ Log.d(TAG, "dialogNoInternet: ") }
        networkState(dialog)
    }
    /**
     * Listener to have internet state
     * [onAvailable] close dialog, see progress an redirect
     * [dialogNoInternet] see dialog to choose action
     */
    private fun networkState(dialog: AlertDialog) {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (this@SplashScreenFragment.isDetached) {
                        observeState()
                        dialog.dismiss()
                    }
                }
                override fun onLost(network: Network) {
                    if (this@SplashScreenFragment.isDetached) {
                        dialogNoInternet()
                    }
                }
            })
        } else{
            val networkChangeFilter = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(
                networkChangeFilter,
                object : ConnectivityManager.NetworkCallback(){
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    if (this@SplashScreenFragment.isDetached) {
                        observeState()
                        dialog.dismiss()
                    }
                }
                override fun onLost(network: Network) {
                    if (this@SplashScreenFragment.isDetached) {
                        dialogNoInternet()
                    }
                }
            })
        }
    }
    /**
     * Redirect if don't have subscription
     * @param [FLAG_DEST_SPLASH_TO_MAIN], subscriptionFragment([FLAG_DEST_SPLASH_TO_MAIN]), id to know which fragment was by previously
     * [FLAG_DEST_SPLASH_TO_MAIN] redirect to [BillingFragment],
     * path: SplashScreenFragment to subscriptionFragment, don't have subscription
     * [mainFragment] redirect to [MainFragment], have subscription
     */
    private fun navigateToMainScreen() {
        if (BillingManager.BILLING_FLAG_STATE) {
            findNavController().navigate(SplashScreenFragmentDirections.subscriptionFragment(FLAG_DEST_SPLASH_TO_MAIN))
        } else {
            findNavController().navigate(SplashScreenFragmentDirections.mainFragment())
        }

    }
}