package com.example.minecraft.ui.main

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.MainActivity.Companion.FLAG_DEST_MAIN_FRAGMENT
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.ItemFooterBinding
import com.example.minecraft.databinding.ItemRecyclerAdnativeBinding
import com.example.minecraft.databinding.ItemRecyclerBinding
import com.example.minecraft.databinding.FragmentMainBinding
import com.example.minecraft.ui.spash.SplashscreenActivity
import com.example.minecraft.ui.util.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class MainFragment : DownloadDialogUtil(){
    companion object{
        const val TAG = "MainFragment"
        const val PAGE_SIZE: Int = 4
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    lateinit var adapter: PagingAdapter

    var addCount: Int = 0

    var isLoading = false
    var itemAd: RosterItem? = null

    lateinit var appSharedPrefManager: AppSharedPreferencesManager
    var prefState = false

    var fulList: MutableSet<RosterItem> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSharedPrefManager = AppSharedPreferencesManager(requireActivity())

        adapter = PagingAdapter()

        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        lifecycleScope.launch {
            appSharedPrefManager.billingAdsSate.collectLatest { prefState ->
                this@MainFragment.prefState = prefState
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle()

        if (!prefState) {
            MobileAds.initialize(requireActivity()) {}
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        } else {
            binding.adView.visibility = View.GONE
        }
        // Setup recycler
        binding.apply {
            container.adapter = adapter
            val manager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            container.layoutManager = manager
            lifecycleScope.launch {
                if (!prefState) {
                    if (checkInternetConnection()) {
                        // Online list
                        viewModel.list.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                            .collectLatest { state ->
                                when (state) {
                                    RosterItemLoadState.Loading -> {
                                        progressBar.visibility = View.VISIBLE
                                        viewModel.getItem(adapter.getItems(), PAGE_SIZE)
                                    }
                                    is RosterItemLoadState.LoadComplete -> {
                                        val job1 = async { getItemAd() }
                                        val job2 = async { state.content }
                                        job2.start()
                                        job1.start()

                                        val content = job2.await()
                                        val ad = job1.await()

                                        val prev = fulList.size
                                        fulList.addAll(content)
                                        val newList = fulList.size
                                        if (newList - prev == PAGE_SIZE){
                                            fulList.add(ad)
                                        }

                                        adapter.deleteFooter()
                                        fulList.add(FooterItem())
                                        adapter.submitList(fulList.toMutableList())

                                        progressBar.visibility = View.GONE
                                    }
                                    is RosterItemLoadState.LoadLast -> {
                                        insertLastItem(state.content)
                                        progressBar.visibility = View.GONE
                                    }
                                    is RosterItemLoadState.Error -> {
                                        progressBar.visibility = View.GONE
                                        Log.d(TAG, "flowWithLifecycle list: ${state.error}")
                                    }
                                }

                                container.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                        super.onScrolled(recyclerView, dx, dy)
                                        if (isLoading) {
                                            if (manager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 3) {
//                                networkState()
                                                Log.d(TAG, "onScrolled: ")
                                                if (!checkInternetConnection()) {
                                                    dialogNetwork(false)
                                                }
                                                viewModel.getItem(adapter.getItems(), PAGE_SIZE)
                                                isLoading = false
                                            }
                                        }
                                    }
                                })
                            }
                    } else {
                        networkState()
                        offlineList()
                    }
                }else{
                    offlineList()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //
    private fun offlineList(){
        viewModel.getAll()
        lifecycleScope.launch {
            viewModel.offLineList
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collectLatest { state ->
                    when(state){
                        is RosterItemOffLineState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Log.d(TAG, "flowWithLifecycle offLineList: ${state.error}")
                        }
                        RosterItemOffLineState.InitSate -> { binding.progressBar.visibility = View.VISIBLE }
                        is RosterItemOffLineState.LoadComplete -> {
                            binding.progressBar.visibility = View.GONE
                            adapter.submitList(state.content.toMutableList())
                        }
                    }
                }
        }
    }
    //
    private fun networkState(){
        val connectivityManager = requireActivity().applicationContext
            .getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (checkInternetConnection()){ dialogNetwork(true) }
                }

                override fun onLost(network: Network) {
                    if (!checkInternetConnection()) { dialogNetwork(false) }
                }
            })
        }
    }
    //
    fun dialogNetwork(state: Boolean){
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.alert_network_title))
            .setMessage(getString(R.string.alert_no_network_msg))

        builder.setPositiveButton(getString(R.string.alert_btn_continue)) { _, _ ->
                builder.create().dismiss()
            }
            .setNegativeButton(getString(R.string.alert_btn_reload)) { _, _ ->
                val intent = Intent(requireActivity(), SplashscreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent)
            }

        if (state) {
            builder.setMessage(getString(R.string.alert_network_msg))
                .setPositiveButton(getString(R.string.alert_btn_leave)) { _, _ ->
                    requireActivity().finish()
                }
                .setNegativeButton(getString(R.string.alert_btn_reload)) { _, _ ->
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK;
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent)
                }
            builder.create().dismiss()
        }

        val dialog = builder.create()
        try { dialog.show() }
        catch (e : Exception){ Log.d(TAG, "dialogNoInternet: ") }
    }

    private fun insertRosterIem(list: List<RosterItem>) {

    }

    private fun insertLastItem(list: List<RosterItem>){
        adapter.deleteFooter()
        fulList.addAll(list)
        adapter.submitList(fulList.toMutableList())
    }
    //Convert callback in to coroutine
    private suspend fun getItemAd()  =
        suspendCoroutine<RosterItem> {cont ->
            var item: RosterItem? = null
            val adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd { ad: NativeAd ->
                    item = AdsItem(ad)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        cont.resume((item!!))
                    }
                }).build()
            adLoader.loadAd(AdRequest.Builder().build())
        }

    fun setupToolBartTitle(){ (activity as MainActivity).setupToolBartTitle() }

    // ==========================   Helpers  ==============================================

    inner class PagingAdapter : ListAdapter<RosterItem, RecyclerView.ViewHolder>(diff) {
        val REGULAR_ITEM = 0
        val FOOTER_ITEM = 1
        val AD_NATIVE_ITEM = 2

        override fun submitList(list: MutableList<RosterItem>?) {
            super.submitList(list)
            isLoading = true
        }

        override fun getItemViewType(position: Int): Int {
            return when (adapter.getItem(position).rosterType) {
                RosterItem.TYPE.ADDON -> { REGULAR_ITEM }
                RosterItem.TYPE.ADS -> { AD_NATIVE_ITEM }
                else -> { FOOTER_ITEM }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return when (viewType) {
                REGULAR_ITEM -> { TaskViewHolder(ItemRecyclerBinding.inflate(layoutInflater, parent, false)      ) }
                FOOTER_ITEM -> { FooterViewHolder(ItemFooterBinding.inflate(layoutInflater, parent, false)) }
                AD_NATIVE_ITEM -> { AdNativeViewHolder(ItemRecyclerAdnativeBinding.inflate(layoutInflater, parent, false)) }
                else -> { throw RuntimeException("ItemArrayAdapter, The type has to be TWO, ONE or ZERO") }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val element = adapter.getItem(position)
            when (holder.itemViewType) {
                REGULAR_ITEM -> {
                    holder as TaskViewHolder
                    holder.bind(element as AddonModel)
                }
                AD_NATIVE_ITEM -> {
                    holder as AdNativeViewHolder
                    holder.bind(element as AdsItem)
                }
            }
        }

        fun getItems(): Int {
            var count = 0
            for (item in adapter.currentList) {
                if (item.rosterType == RosterItem.TYPE.ADDON) {
                    count++
                }
            }
            return count
        }

        fun deleteFooter() {
            val list = fulList.toList().takeLast(PAGE_SIZE + 2)
            for (item in list) {
                if (item.rosterType == RosterItem.TYPE.FOOTER) {
                    fulList.remove(item)
                }
            }
        }
    }
    inner class FooterViewHolder(private val binding: ItemFooterBinding) : RecyclerView.ViewHolder(binding.root)

    inner class AdNativeViewHolder(private val binding: ItemRecyclerAdnativeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AdsItem) {
            val ad = item.ads
            val adView = binding.container

            adView.iconView = adView.findViewById(R.id.ad_app_icon)

            val headlineView = adView.findViewById<TextView>(R.id.txt_install)
            headlineView.text = ad.headline
            adView.headlineView = headlineView

            val list = ad.images
            val img = adView.findViewById<ImageView>(R.id.imageView)

            Glide.with(requireActivity()).load(list[0].uri).into(img)
            adView.imageView = img

            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

            if (ad.callToAction == null) {
                adView.callToActionView?.visibility = View.INVISIBLE
            } else {
                adView.callToActionView?.visibility = View.VISIBLE
                (adView.callToActionView as Button).text = ad.callToAction
            }

            if (ad.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(ad.icon?.drawable)
                adView.iconView?.visibility = View.VISIBLE
            }
            adView.setNativeAd(ad)
        }
    }

    inner class TaskViewHolder(private val binding: ItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: AddonModel) {
                val title = item.title

                binding.apply {
                    Glide.with(requireActivity()).load(item.image).centerCrop().into(imageView)
                    txtInstall.setOnClickListener {
                        findNavController().navigate(
                            MainFragmentDirections.detailFragment(item, title)
                        )
                    }
                    txtInstall.text = title

                    btnDownload.setOnClickListener {
                        if (!prefState) {
                            findNavController().navigate(MainFragmentDirections.trialFragment(FLAG_DEST_MAIN_FRAGMENT))
                        } else {
                            if (checkPermission()) {
                                checkFileExists(item)
                                dialogDownload(item, DownloadAddon.DIR_CACHE)
                            }
                        }
                    }

                    itemView.setOnClickListener {
                        findNavController().navigate(MainFragmentDirections.detailFragment(item, title)
                        )
                    }
                }
            }
        }

    val diff = object : DiffUtil.ItemCallback<RosterItem>() {
            override fun areItemsTheSame(oldItem: RosterItem, newItem: RosterItem): Boolean {
                return (oldItem as? AddonModel)?.id == (newItem as? AddonModel)?.id
            }

            override fun areContentsTheSame(oldItem: RosterItem, newItem: RosterItem): Boolean {
                return (oldItem as? AddonModel) == (newItem as? AddonModel)
            }
        }
}