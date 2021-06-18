package com.example.minecraft.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.ItemFooterBinding
import com.example.minecraft.databinding.ItemRecyclerAdnativeBinding
import com.example.minecraft.databinding.ItemRecyclerBinding
import com.example.minecraft.databinding.MainFragmentBinding
import com.example.minecraft.ui.util.DownloadDialogUtil
import com.example.minecraft.ui.util.EventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainFragment : DownloadDialogUtil(){
    companion object{
        const val TAG = "MainFragment"
    }

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private val PAGE_SIZE: Int = 10
    lateinit var adapter: PagingAdapter

    lateinit var adLoader: AdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PagingAdapter()

        // first in
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)

        viewModel.getLimit(adapter.getItems(), PAGE_SIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle()
        // Setup recycler
        binding.apply {
            container.adapter = adapter
            val manager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            container.layoutManager = manager
            container.addOnScrollListener(
                RecyclerScroller(manager, object : RecyclerScroller.ScrollListener{
                    override fun onLoadMore() {
                        // setup list
                       viewModel.getLimit(adapter.getItems(), PAGE_SIZE)
                        adapter.footerFlag = true
                    }
                })
            )
        }
        viewModel.list.observe(viewLifecycleOwner, EventObserver{
            val list = it.toMutableList()
            if (it.isNotEmpty()) {
                val temp: ArrayList<Any> = arrayListOf()
//                adapter.addItems(temp.addAll(list))
                temp.addAll(list)
                adapter.addItems(temp)
            } else {
                binding.message.visibility = View.VISIBLE
            }
        })
        setupAdMob()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun setupAdMob() {
//        val adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-3940256099942544/2247696110")
        adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad : NativeAd ->
                val adView = layoutInflater.inflate(R.layout.item_recycler_adnative, null) as NativeAdView
//                    val adView = binding as NativeAdView
                populateNativeAdView(ad, adView)
                if (adLoader.isLoading) {
                    val list: MutableList<Any> = mutableListOf(ad)
                    Log.d(TAG, "setupAdMob: ${list.size}")
                    adapter.addItems(list)
                }
                if (requireActivity().isDestroyed) {
                    ad.destroy()
                    return@forNativeAd
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the failure by logging, altering the UI, and so on.
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder().build()
            ).build()
//        adLoader.loadAd(AdRequest.Builder().build())
        adLoader.loadAds(AdRequest.Builder().build(), 5)

    }

    private fun populateNativeAdView(ad: NativeAd, adView: NativeAdView, ){
        adView.setNativeAd(ad)
//        val item = adView.findViewById<LinearLayout>(R.id.ad_native_container)
//        item.removeAllViews()
//        item.addView(adView)
    }

    fun setupToolBartTitle(){ (activity as MainActivity?)!!.setupToolBartTitle() }
    // Helpers
    inner class PagingAdapter(private val list: MutableList<Any> = arrayListOf()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val REGULAR_ITEM = 0
        val FOOTER_ITEM = 1
        val AD_NATIVE_ITEM = 2
        // hide footer item
        var footerFlag = false

        override fun getItemViewType(position: Int): Int {
            if (position == list.size) { return FOOTER_ITEM }
            val item = list[position]
            return if (item is AddonModel) {
                REGULAR_ITEM
            }
//            else if (position > 1 && (position + 1) % 9 == 0) {
            else if (item is NativeAd) {
                AD_NATIVE_ITEM
            }
            else {
                FOOTER_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  RecyclerView.ViewHolder {
            return when (viewType) {
                REGULAR_ITEM -> {
                    TaskViewHolder(ItemRecyclerBinding.inflate(layoutInflater, parent, false))
                }
                FOOTER_ITEM -> {
                    FooterViewHolder(ItemFooterBinding.inflate(layoutInflater, parent, false))
                }
                AD_NATIVE_ITEM -> {
//                    AdNativeViewHolder(ItemRecyclerAdnativeBinding.inflate(layoutInflater, parent, false))
                    AdNativeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_adnative, parent, false))
                }
                else -> {
                    throw RuntimeException("ItemArrayAdapter, The type has to be ONE or ZERO")
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder.itemViewType){
                REGULAR_ITEM -> {
                    holder as TaskViewHolder
                    holder.bind(list[position] as AddonModel)
                }
                AD_NATIVE_ITEM -> {
                    holder as AdNativeViewHolder
//                    holder.bind()
                }
                // no data need to be assigned
                FOOTER_ITEM -> {
                    holder as FooterViewHolder
                    holder.bind(footerFlag)
                }
                else -> { }
            }
        }

        override fun getItemCount() = list.size + 1

//        fun addItems(items : MutableList<AddonModel>) {
        fun addItems(items : MutableList<Any>) {
            list.addAll(items)
            notifyDataSetChanged()
        }

        fun getItems(): Int{
            var count = 0
            for (item in list){
                if (item is AddonModel){
                    count++
                }
            }
            return count
        }
    }

    inner class FooterViewHolder(private val binding: ItemFooterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(state: Boolean) {
            if (state) {
                binding.progressBar2.visibility = View.VISIBLE
            } else {
                binding.progressBar2.visibility = View.GONE
            }
        }
    }

    inner class AdNativeViewHolder(private val binding: ItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: NativeAd){
            val title = model.

            binding.apply {
                Glide.with(requireActivity()).load(model.image).centerCrop().into(imageView)
                txtInstall.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title, false))
                }
                txtInstall.text = title
                btnDownload.setOnClickListener {
                    if (checkPermission()) { dialogDownload(model, DownloadAddon.DIR_CACHE) }
                }
                itemView.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title, false))
                }
            }
        }
    }
    }

    inner class TaskViewHolder(private val binding: ItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: AddonModel){
            val title = model.title

            binding.apply {
                Glide.with(requireActivity()).load(model.image).centerCrop().into(imageView)
                txtInstall.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title, false))
                }
                txtInstall.text = title
                btnDownload.setOnClickListener {
                    if (checkPermission()) { dialogDownload(model, DownloadAddon.DIR_CACHE) }
                }
                itemView.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title, false))
                }
            }
        }
    }
}
class RecyclerScroller(private val layout: LinearLayoutManager, private val listener: ScrollListener) : RecyclerView.OnScrollListener() {
    private var previousTotal = 0
    private val visibleThreshold = 1
    private var loading = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val visibleItemCount = recyclerView.childCount
        val totalItemCount: Int = layout.itemCount
        val firstVisibleItem: Int = layout.findFirstVisibleItemPosition()
        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
            if (totalItemCount > 1) listener.onLoadMore()
                loading = true
            }
        }
        interface ScrollListener {
            fun onLoadMore()
        }
    }
