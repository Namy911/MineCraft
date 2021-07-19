package com.example.minecraft.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.example.minecraft.BuildConfig
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentDetailBinding
import com.example.minecraft.MainActivity
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_BILLING_FRAGMENT
import com.example.minecraft.ui.util.AppSharedPreferencesManager
import com.example.minecraft.ui.util.AppUtil.Companion.REVARD_AD_UNIT_ID
import com.example.minecraft.ui.util.DownloadDialogUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File


@AndroidEntryPoint
class DetailFragment : DownloadDialogUtil() {
    companion object {
        const val TAG = "DetailFragment"
    }

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = checkNotNull(_binding) {"binding isn't initialized"}

    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    var mRewardedAd: RewardedAd? = null

    lateinit var appSharedPrefManager: AppSharedPreferencesManager
    private var prefState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setFlagRewardDownload(false)
        viewModel.setFlagRewardShare(false)
        //
        appSharedPrefManager = AppSharedPreferencesManager(requireContext())

        lifecycleScope.launch {
            appSharedPrefManager.billingAdsSate.collectLatest { state ->
                prefState = state
            }
        }

        if (checkInternetConnection() && !prefState) {
            MobileAds.initialize(requireContext()) {
                loadAddReward()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkFileExists(args.model)

        binding.apply {
            val list: List<String> = args.model.preview
            imgContainer.adapter = DetailPageAdapter(list, args.title, requireActivity())
            pageIndicator.apply {
                count = list.size
                selection = 0
            }

            txtDesc.text = args.model.description
            if (!prefState) {
                buttonInitTitle()
            } else {
                readyAdsButtonsConf()
            }

            btnShare.setOnClickListener {
                val temp = viewModel.getFlagRewardShare()

                val temBehavior = args.model.behavior
                val temResource = args.model.resource



//                if (temp == false && checkInternetConnection() && !prefState) {
//                    adSeen(DownloadAddon.DIR_CACHE)
//                }else{
                if (temBehavior.isNotEmpty() && temResource.isNotEmpty()){
                    checkFileExists(args.model)
                    shareFilesCheck()
                }else if (temBehavior.isNotEmpty()){
                    checkFileExists(args.model)
                    shareFileCheck(temBehavior, TAG_BEHAVIOR)
                } else if (temResource.isNotEmpty()){
                    checkFileExists(args.model)
                    shareFileCheck(temResource, TAG_RESOURCE)
                }
//                }
            }

            btnDownload.setOnClickListener {
                if (!prefState) {
//                    val temp = viewModel.getFlagRewardDownload()
//                    if (temp == false && checkInternetConnection()) {
//                        adSeen(DownloadAddon.DIR_EXT_STORAGE)
//                    } else {
//                        if (checkPermission()) {
//                            dialogDownload(args.model, DownloadAddon.DIR_EXT_STORAGE)
//                        }
//                    }
//                } else {
                    if (checkPermission()) {
                        dialogDownload(args.model, DownloadAddon.DIR_EXT_STORAGE)
                    }
                }
            }

            btnInstall.setOnClickListener {
                if (prefState) {
                    if (checkPermission()) {
                        checkFileExists(args.model)
                        dialogDownload(args.model, DownloadAddon.DIR_CACHE)
                    }
                } else {
                    findNavController().navigate(
                        DetailFragmentDirections.subscriptionFragment(
                            FLAG_DEST_BILLING_FRAGMENT
                        )
                    )
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolBartTitle(getString(R.string.title_fragment_details))
        lifecycleScope.launchWhenResumed {
            appSharedPrefManager.billingAdsSate.collectLatest { state ->
                if (!state) {
                    MobileAds.initialize(requireContext()) {}
                    binding.adView.loadAd(AdRequest.Builder().build())
                } else {
                    binding.adView.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun readyAdsButtonsConf() {
        binding.apply {
            btnDownload.text = getString(R.string.btn_download)
            btnShare.text = getString(R.string.btn_share)
        }
    }

    private fun buttonInitTitle() {
        if (checkInternetConnection()) {
            if (mRewardedAd != null) {
                readyAdsButtonsConf()
            } else {
                binding.apply {
                    btnDownload.text = getString(R.string.btn_prepay)
                    btnShare.text = getString(R.string.btn_prepay)
                }
            }
        } else {
            binding.apply {
                btnDownload.text = getString(R.string.btn_download)
                btnShare.text = getString(R.string.btn_share)
            }
        }
    }

    private fun loadAddReward() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(requireActivity(), REVARD_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    mRewardedAd = null
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    readyAdsButtonsConf()
                    mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadAddReward()
                        }
                    }
                }
            })
    }

    private fun adSeen(flag: String) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(requireActivity()) {
                fun onUserEarnedReward() {
                    if (flag == DownloadAddon.DIR_CACHE) {
                        viewModel.setFlagRewardShare(true)
                    } else {
                        viewModel.setFlagRewardDownload(true)
                    }
                    loadAddReward()
                }
                onUserEarnedReward()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
        }
    }

    fun setupToolBartTitle(title: String) {
        (activity as MainActivity).setupToolBartTitle(title)
    }

    private fun shareFileCheck(model: String, tag: String) {
        if (checkInternetConnection()) {
            if (checkPermission()) {
                workDownloadAddon(model, getPackFileName(model, tag),
                    DownloadAddon.DIR_CACHE, args.model, true)
            }
        } else {
            Toast.makeText(
                requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT
            ).show()
        }
    }
    // Button share logic
    private fun shareFilesCheck() {
        val callbackShare = object : BtnShareListener {
            val temp1 = viewModel.getCachePathBehavior()
            val temp2 = viewModel.getCachePathResource()
            val list = arrayListOf<Uri?>(null, null)

            override suspend fun configResource() {
                if (temp1 != null) {
                    list[1] = getPath(File(temp1))
                } else {
                    if (checkInternetConnection()) {
                        if (checkPermission()) {
                            workDownloadAddon(
                                args.model.behavior,
                                getPackFileName(args.model.behavior, TAG_BEHAVIOR),
                                DownloadAddon.DIR_CACHE, args.model,
                                true
                            )
                        }
                    } else {
                        Toast.makeText(
                            requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override suspend fun configBehavior() {
                if (temp2 != null) {
                    list[0] = getPath(File(temp2))
                } else {
                    if (checkInternetConnection()) {
                        if (checkPermission()) {
                            workDownloadAddon(
                                args.model.resource,
                                getPackFileName(args.model.resource, TAG_RESOURCE),
                                DownloadAddon.DIR_CACHE,
                                args.model,
                                true
                            )
                        }
                    } else {
                        Toast.makeText(
                            requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun sendIntent() {
                Log.d(TAG, "sendIntent: hhh")
                if (list[0] != null && list[1] != null) {
                    Log.d(TAG, "sendIntent: 1")
                    val sendIntent: Intent = Intent().apply {
                        putExtra(Intent.EXTRA_TEXT, "Share Addon")
                        type = "file/*"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
                        action = Intent.ACTION_SEND_MULTIPLE
                    }

                    val shareIntent = Intent.createChooser(sendIntent, "")
                    requireActivity().startActivity(shareIntent)
                }
            }
        }

        lifecycleScope.launch {
             val job1 = async { callbackShare.configResource() }
             val job2 = async { callbackShare.configBehavior() }

            job1.start()
            job2.start()
            job1.await()
            job2.await()

            callbackShare.sendIntent()
        }
    }
    //
    fun getPath(file: File): Uri? = try {
        FileProvider.getUriForFile(
            requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", file
        )
    } catch (e: IllegalArgumentException) {
        Log.d("File Selector", "The selected file not funded: $file")
        null
    }

    inner class DetailPageAdapter(
        private val listOfLinks: List<String>, val title: String,
        private val ctx: Context
    ) : PagerAdapter() {

        override fun getCount(): Int {
            return listOfLinks.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(ctx)
            val view = inflater.inflate(R.layout.item_pager, container, false)
            val image = view.findViewById<ImageView>(R.id.img)
            val txtTitle = view.findViewById<TextView>(R.id.txt_title)

            txtTitle.text = title
            Glide.with(requireActivity()).load(listOfLinks[position]).centerCrop().into(image)
            container.addView(view)

            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }
}