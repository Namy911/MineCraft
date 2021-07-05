package com.example.minecraft.ui.main

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
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
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentDetailBinding
import com.example.minecraft.ui.util.DownloadDialogUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class DetailFragment : DownloadDialogUtil() {
    companion object{
        const val TAG = "DetailFragment"
//        const val FLAG_BUTTON_SHARE = "ui.main.flag.button_share"
    }

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    var mRewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //
        viewModel.setFlagTrial(false)
        viewModel.setFlagRewardDownload(false)
        viewModel.setFlagRewardShare(false)
        //
        if (checkInternetConnection()) {
            MobileAds.initialize(requireContext()){
                loadAddReward()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_details))
        checkFileExists(args.model)

        binding.apply {
            val list: List<String> = args.model.preview
            imgContainer.adapter = DetailPageAdapter(list, args.title, requireActivity())
            pageIndicator.apply {
                count = list.size
                selection = 0
            }

            buttonInitTitle()

            txtDesc.text = args.model.description

            btnShare.setOnClickListener {
                if (checkInternetConnection()) {
                    val temp = viewModel.getFlagRewardShare()
                    if (temp == false) {
                        adSeen(DownloadAddon.DIR_CACHE)
                    } else{
                        checkFileExists(args.model)
                        shareFileCheck()
                    }
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                }
            }

            btnDownload.setOnClickListener {
                val temp = viewModel.getFlagRewardDownload()
                if (temp == false && checkInternetConnection()) {
                    adSeen(DownloadAddon.DIR_EXT_STORAGE)
                } else {
                    if (checkPermission()) {
                        dialogDownload(args.model, DownloadAddon.DIR_EXT_STORAGE)
                    }
                }
            }

            btnInstall.setOnClickListener { button ->
//                val temp = viewModel.getFlagTrial()
//                if (checkPermission()) {
//                    if (temp == true) {
//                        if (checkPermission()) {
//                            checkFileExists(args.model)
//                            dialogDownload(args.model, DownloadAddon.DIR_CACHE)
//                        }
//                    } else {
                        findNavController().navigate(DetailFragmentDirections.trialFragment(args.model))
                        viewModel.setFlagTrial(true) // trial fragment has been open
//                    }
//                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun readyAdsButtonsConf(){
        binding.apply {
            btnDownload.text = getString(R.string.btn_download)
            btnShare.text = getString(R.string.btn_share)
        }
    }

    fun buttonInitTitle(){
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
    fun networkSate(){
        val connectivityManager = requireActivity().applicationContext
            .getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    binding.apply {
                        btnDownload.text = getString(R.string.btn_download)
                        btnShare.text = getString(R.string.btn_share)
                    }
                }

                override fun onLost(network: Network) {
                    binding.apply {
                        btnDownload.text = getString(R.string.btn_download)
                        btnShare.text = getString(R.string.btn_share)
                    }
                }
            })
        }
    }
    private fun loadAddReward() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            requireActivity(), "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    mRewardedAd = null
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    readyAdsButtonsConf()
                    mRewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                           loadAddReward()
                        }
                    }
                }
            })
    }

    private fun adSeen(flag: String){
        if (mRewardedAd != null) {
            mRewardedAd?.show(requireActivity()) {
                fun onUserEarnedReward() {
                    if (flag == DownloadAddon.DIR_CACHE){
                        viewModel.setFlagRewardShare(true)
                    }else {
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

    fun setupToolBartTitle(title: String){ (activity as MainActivity?)!!.setupToolBartTitle(title) }
    // Button share logic
    fun shareFileCheck() {
        val temp1 = viewModel.getCachePathBehavior()
        val temp2 = viewModel.getCachePathResource()

        val sendIntent: Intent = Intent().apply {
            putExtra(Intent.EXTRA_TEXT, "Share Addon")
            type = "file/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val list = arrayListOf<Uri?>(null, null)
        // Behavior cell config
        if (temp1 != null) {
            list[1] = getPath(File(temp1))
        } else {
            if (checkInternetConnection() && checkPermission()) {
                workDownloadAddon(
                    args.model.behavior,
                    getPackFileName(args.model.behavior, TAG_BEHAVIOR),
                    DownloadAddon.DIR_CACHE,
                    args.model,
                    true
                )
            } else {
                Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
            }
        }
        // Resource cell config
        if (temp2 != null) {
            list[0] = getPath(File(temp2))
        } else {
            if (checkInternetConnection() && checkPermission()) {
                workDownloadAddon(
                    args.model.resource,
                    getPackFileName(args.model.resource, TAG_RESOURCE),
                    DownloadAddon.DIR_CACHE,
                    args.model,
                    true
                )
            } else {
                Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
            }
        }
        // Intent share config, multiple or single
        if (list[0] != null && list[1] != null) {
            sendIntent.action = Intent.ACTION_SEND_MULTIPLE
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            val shareIntent = Intent.createChooser(sendIntent, "null")
            requireActivity().startActivity(shareIntent)
        }
    }
    //
    fun getPath(file: File) = try {
        FileProvider.getUriForFile(
            requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", file)
    } catch (e: IllegalArgumentException) {
        Log.d("File Selector", "The selected file not funded: $file")
        null
    }

    inner class DetailPageAdapter(private val listOfLinks: List<String>, val title: String, private val ctx: Context) : PagerAdapter(){
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

            return  view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

    }
}