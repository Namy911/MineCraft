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
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.minecraft.BuildConfig
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentDetailBinding
import com.example.minecraft.MainActivity
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_BILLING_FRAGMENT
import com.example.minecraft.data.model.AddonModel
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
    private val binding get() = checkNotNull(_binding) { "binding isn't initialized" }
//    private val binding get() = _binding!!

    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    var mRewardedAd: RewardedAd? = null

    lateinit var appSharedPrefManager: AppSharedPreferencesManager

    private var prefState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setFlagRewardDownload(false)
        viewModel.setFlagRewardShare(false)
        // See state of subscription
        appSharedPrefManager = AppSharedPreferencesManager(requireContext())
        lifecycleScope.launch {
            appSharedPrefManager.billingAdsSate.collectLatest { state -> prefState = state }
        }
        // If have internet connection and don't have subscription load AD reward
        if (checkInternetConnection(requireContext()) && !prefState) {
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

        val tempBehavior = args.model.behavior
        val tempResource = args.model.resource
        // check if file exist before download
        when {
            tempBehavior.isNotEmpty() && tempResource.isNotEmpty() -> { checkFilesExists(args.model);Log.d(TAG, "onViewCreated: 1") }
            tempBehavior.isNotEmpty() -> { checkFileExists(args.model.behavior, TAG_BEHAVIOR);Log.d(TAG, "onViewCreated: 2") }
            tempResource.isNotEmpty() -> { checkFileExists(args.model.resource, TAG_RESOURCE);Log.d(TAG, "onViewCreated: 3") }
        }
        Log.d(TAG, "checkFilesExists: ${viewModel.getCachePathBehavior()}")
        Log.d(TAG, "checkFilesExists: ${viewModel.getCachePathResource()}")
        binding.apply {
            val list: List<String> = args.model.preview
            imgContainer.adapter = DetailPageAdapter(list, args.title, requireActivity())
            // Setup indicators(dots) on slider
            pageIndicator.apply {
                count = list.size
                selection = 0
            }
            //
//            lifecycleScope.launchWhenStarted {
//                viewModel.progress.collectLatest { value ->
//                    progressDownload.progress = value
//                }
//            }

            txtDesc.text = args.model.description
            if (!prefState) {
                buttonInitTitle()
            } else {
                readyAdsButtonsConf()
            }
            /**
             * check if file exist before download
             * Setup Buttons, check subscription or if AD seen
             */
            btnShare.setOnClickListener {
                val temp = viewModel.getFlagRewardShare()

//                if (temp == false && checkInternetConnection(requireContext()) && !prefState) {
//                    adSeen(DownloadAddon.DIR_CACHE)
//                } else {
                    when {
                        tempBehavior.isNotEmpty() && tempResource.isNotEmpty() -> {
                            when {
                                viewModel.getCachePathBehavior() == null && viewModel.getCachePathResource() == null -> {
                                    checkFilesExists(args.model)
                                    shareFilesCheck()
                                }
                                viewModel.getCachePathResource() == null -> {
                                    shareFileCheck(tempResource, TAG_RESOURCE)
                                    checkFileExist(args.model.resource, TAG_RESOURCE)
                                }
                                viewModel.getCachePathBehavior() == null -> {
                                    shareFileCheck(tempBehavior, TAG_BEHAVIOR)
                                    checkFileExist(args.model.behavior, TAG_BEHAVIOR)
                                } else -> {
                                    checkFilesExists(args.model)
                                    shareFilesCheck()
                                }
                            }
                        }
                        tempBehavior.isNotEmpty() -> {
                            checkFileExists(args.model.behavior, TAG_BEHAVIOR)
                            shareFileCheck(tempBehavior, TAG_BEHAVIOR)
                        }
                        tempResource.isNotEmpty() -> {
                            checkFileExists(args.model.resource, TAG_RESOURCE)
                            shareFileCheck(tempResource, TAG_RESOURCE)
                        }
                    }
                }
//            }

            btnDownload.setOnClickListener {
//                if (!prefState) {
//                    val temp = viewModel.getFlagRewardDownload()
//                    if (temp == false && checkInternetConnection(requireContext())) {
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
//            }
            // [FLAG_DEST_BILLING_FRAGMENT] from right redirection, close button
            btnInstall.setOnClickListener {
                if (prefState) {
                    if (checkPermission()) {
                        checkFilesExists(args.model)
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
        if (checkInternetConnection(requireContext())) {
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
    // Init Ad reward
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
    /**
     * Show reward once
     * @param [flag] to have difference between
     * button share and button download
     */
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
    /**
     * Behavior from share button, one file
     * check if file exist create and chooser if exist otherwise
     * download file and create chooser
     */
    private fun shareFileCheck(model: String, tag: String) {
        if (checkInternetConnection(requireContext())) {
            val temp = if (tag == TAG_RESOURCE) {
                viewModel.getCachePathResource()
            } else {
                viewModel.getCachePathBehavior()
            }

            val cacheLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model, tag)

            if (temp.isNullOrEmpty()) {
                if (checkPermission()) {
                    workDownloadSingle(
                        model, getPackFileName(model, tag),
                        DownloadAddon.DIR_CACHE, args.model, true
                    )
                    if (File(cacheLink).exists()) {
                        viewModel.setCachePathBehavior(cacheLink)
                    }
                }
            } else {
                val sendIntent: Intent = Intent().apply {
                    putExtra(Intent.EXTRA_TEXT, "Share Addon")
                    type = "file/*"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_STREAM, getPath(File(cacheLink)))
                    action = Intent.ACTION_SEND
                }

                val shareIntent = Intent.createChooser(sendIntent, "")
                requireActivity().startActivity(shareIntent)
            }
        } else {
            Toast.makeText(
                requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT
            ).show()
        }
    }
    /**
     * Behavior from share button, multiple file
     * download files and create chooser
     */
    private fun shareFilesCheck() {
        val temp1 = viewModel.getCachePathResource()
        val temp2 = viewModel.getCachePathBehavior()
        val list = arrayListOf<Uri?>(null, null)

        if (checkPermission()) {
            if (checkInternetConnection(requireContext())) {
                val model = args.model
                if (temp1 == null && temp2 == null) {
                    workDownloadMultiple(
                        listOf(
                            getPackFileName(model.resource, TAG_RESOURCE),
                            getPackFileName(model.behavior, TAG_BEHAVIOR)
                        ),
                        model
                    )
                } else if (temp1 == null && temp2 != null){
                    workDownloadMultiple(
                        listOf(getPackFileName(model.resource, TAG_RESOURCE), null), model
                    )
                }
                else if (temp1 != null && temp2 == null){
                    workDownloadMultiple(
                        listOf(null, getPackFileName(model.behavior, TAG_BEHAVIOR)), model
                    )
                }else{
                    temp1?.let { list[1] = getPath(File(it)) }
                    temp2?.let { list[0] = getPath(File(it)) }

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
            } else {
                Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    //
    fun getPath(file: File): Uri? = try {
        FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileProvider", file)
    } catch (e: IllegalArgumentException) {
        Log.d("File Selector", "The selected file not funded: $file")
        null
    }

    inner class DetailPageAdapter(
        private val listOfLinks: List<String>,
        private val title: String,
        private val ctx: Context
    ): PagerAdapter() {

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