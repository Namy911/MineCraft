package com.example.minecraft.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.ItemFooterBinding
import com.example.minecraft.databinding.ItemRecyclerBinding
import com.example.minecraft.databinding.MainFragmentBinding
import com.example.minecraft.ui.util.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MainFragment : DownloadDialogUtil(){
    companion object{
        const val TAG = "MainFragment"
        const val COUNT_ADS = 5
    }

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private val PAGE_SIZE: Int = 4
    lateinit var adapter: PagingAdapter

    var addCount: Int = 0

    private lateinit var trialManager: TrialManager
    private var flagTrial = false

    var isLoading = false

    var fulList: MutableSet<RosterItem> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trialManager = TrialManager(requireActivity())
        // check if trial exist
        requireActivity().lifecycleScope.launchWhenCreated {
            trialManager.trialFlow.collectLatest { trial ->
                flagTrial = trial != TrialManager.TRIAL_NOT_EXIST
            }
        }
        adapter = PagingAdapter()
        viewModel.getLimit(0, PAGE_SIZE)
        setupListItem()
        // first in
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)
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
            progressBar.visibility = View.VISIBLE
            container.adapter = adapter
            val manager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            container.layoutManager = manager
            container.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isLoading) {
                        if (manager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1) {
                            viewModel.getLimit(adapter.getItems(), PAGE_SIZE)
                            setupListItem()
                            isLoading = false
                        }
                    }
                }
            })
        }
        adapter.getFooter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun itemConfig(item : RosterItem){
        binding.progressBar.visibility = View.GONE
        lifecycleScope.launch {
            viewModel.list
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collectLatest {
                    if (it.isNotEmpty()) {
                        val temp = mutableSetOf<RosterItem>()
                        temp.addAll(it)
                        temp.add(item)
                        Log.d(TAG, "itemConfig: ${temp.size}")
                        fulList.addAll(temp)
//                        fulList.add(FooterItem(true))
                    }
                        adapter.submitList(fulList.toMutableList())
                        isLoading = true
                }
        }
    }

    private fun setupListItem(): AdLoader {
        var adList: RosterItem? =  null
        val adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad: NativeAd ->
                adList = AdsItem(ad)
                if (requireActivity().isDestroyed) {
                    ad.destroy()
                    return@forNativeAd
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    itemConfig(adList!!)
//                    adapter.getFooter()
                }
            }).build()
        adLoader.loadAd(AdRequest.Builder().build())
        return adLoader
    }

    fun setupToolBartTitle(){ (activity as MainActivity?)!!.setupToolBartTitle() }
    // Helpers
    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    inner class PagingAdapter : ListAdapter<RosterItem, BaseViewHolder<*>>(diff) {
        val REGULAR_ITEM = 0
        val FOOTER_ITEM = 1
        val AD_NATIVE_ITEM = 2
        // hide footer item
        var footerFlag = false

        override fun getItemViewType(position: Int): Int {
            if (position == adapter.itemCount) { return FOOTER_ITEM }
            return when (adapter.getItem(position).rosterType) {
                RosterItem.TYPE.ADDON -> { REGULAR_ITEM }
                RosterItem.TYPE.ADS -> { AD_NATIVE_ITEM }
                else -> { FOOTER_ITEM }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  BaseViewHolder<*> {
            return when (viewType) {
                REGULAR_ITEM -> {
                    TaskViewHolder(ItemRecyclerBinding.inflate(layoutInflater, parent, false))
                }
                FOOTER_ITEM -> {
                    FooterViewHolder(ItemFooterBinding.inflate(layoutInflater, parent, false))
                }
                AD_NATIVE_ITEM -> {
                    AdNativeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_adnative, parent, false))
                }
                else -> {
                    throw RuntimeException("ItemArrayAdapter, The type has to be ONE or ZERO")
                }
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
            val element = adapter.getItem(position)
            when(holder.itemViewType){
                REGULAR_ITEM -> {
                    holder as TaskViewHolder
                    holder.bind(element as AddonModel)
                }
                AD_NATIVE_ITEM -> {
//                    if (!flagTrial) {
                        holder as AdNativeViewHolder
                        holder.bind(element as AdsItem)
//                    }
                }
                // no data need to be assigned
                FOOTER_ITEM -> {
                    holder as FooterViewHolder
//                    holder.bind(footerFlag)
                    holder.bind(element as FooterItem)
                }
                else -> { }
            }
        }


        fun getItems(): Int{
            var count = 0
            for (item in adapter.currentList){
                if (item.rosterType == RosterItem.TYPE.ADDON){
                    count++
                }
            }
            return count
        }

        fun getFooter(){
            for (item in adapter.currentList){
                 if (item.rosterType == RosterItem.TYPE.FOOTER){
                    fulList.remove(item)
                }
            }
            adapter.submitList(fulList.toMutableList())
        }
    }

    inner class FooterViewHolder(private val binding: ItemFooterBinding) : BaseViewHolder<FooterItem>(binding.root) {
       override fun bind(item: FooterItem) {
            if (item.status) {
                binding.progressBar2.visibility = View.VISIBLE
            } else {
                binding.progressBar2.visibility = View.GONE
            }
        }
    }

    inner class AdNativeViewHolder(private val view: View) : BaseViewHolder<AdsItem>(view) {
         override fun bind(item: AdsItem){
            val ad = item.ads
            val adView = view as NativeAdView

            val headlineView = adView.findViewById<TextView>(R.id.txt_install)
            headlineView.text = ad.headline
            adView.headlineView = headlineView

            val list = ad.images
            val img = adView.findViewById<ImageView>(R.id.imageView)
            img.setOnClickListener {
                ad.enableCustomClickGesture()
            }

            Glide.with(requireActivity()).load(list[0].uri).into(img)
            adView.imageView =  img

            val buttonView = adView.findViewById<Button>(R.id.btn_download)
            buttonView.text = ad.callToAction
            adView.callToActionView  = buttonView
//            adView.setClickConfirmingView(buttonView)

            adView.setNativeAd(ad)
        }
    }

    inner class TaskViewHolder(private val binding: ItemRecyclerBinding) : BaseViewHolder<AddonModel>(binding.root) {
       override fun bind(item: AddonModel){
            val title = item.title

            binding.apply {
                Glide.with(requireActivity()).load(item.image).centerCrop().into(imageView)
                txtInstall.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(item,title))
                }
                txtInstall.text = title
                btnDownload.setOnClickListener {
                    if (checkPermission()) { dialogDownload(item, DownloadAddon.DIR_CACHE) }
                }
                itemView.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(item,title))
                }
            }
        }
    }

    val diff = object: DiffUtil.ItemCallback<RosterItem>() {
        override fun areItemsTheSame(oldItem: RosterItem , newItem: RosterItem): Boolean {
            return (oldItem as? AddonModel)?.id == (newItem as? AddonModel)?.id
        }

        override fun areContentsTheSame(oldItem: RosterItem, newItem: RosterItem): Boolean {
            return (oldItem as? AddonModel) == (newItem as? AddonModel)
        }
    }
}

//class RecyclerScroller(
//    private val layout: LinearLayoutManager,
//    private val listener: ScrollListener
//) : RecyclerView.OnScrollListener() {
//    private var previousTotal = 0
//    private val visibleThreshold = 1
//    private var loading = true
//
//    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//        super.onScrolled(recyclerView, dx, dy)
//        val visibleItemCount = recyclerView.childCount
//        val totalItemCount: Int = layout.itemCount
//        val firstVisibleItem: Int = layout.findFirstVisibleItemPosition()
//
//        if (loading) {
//            if (totalItemCount > previousTotal) {
//                loading = false
//                previousTotal = totalItemCount
//            }
//        }
//
//        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
//            if (totalItemCount > 1) listener.onLoadMore()
//            loading = true
//        }
//    }
//
//    interface ScrollListener { fun onLoadMore() }
//}
