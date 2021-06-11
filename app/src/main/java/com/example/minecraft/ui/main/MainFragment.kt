package com.example.minecraft.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.ItemFooterBinding
import com.example.minecraft.databinding.ItemRecyclerBinding
import com.example.minecraft.databinding.MainFragmentBinding
import com.example.minecraft.ui.util.DownloadDialogUtil
import com.example.minecraft.ui.util.EventObserver
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
                adapter.addItems(list)
            }else{
                binding.message.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(){ (activity as MainActivity?)!!.setupToolBartTitle() }
    // Helpers
    inner class PagingAdapter(private val list: MutableList<Any> = arrayListOf()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val REGULAR_ITEM = 0
        val FOOTER_ITEM = 1
        // hide footer item
        var footerFlag = false

        override fun getItemViewType(position: Int): Int {
            if (position == list.size) { return FOOTER_ITEM }
            val item = list[position]
            return if (item is AddonModel) {
                REGULAR_ITEM
            } else {
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
                // no data need to be assigned
                FOOTER_ITEM -> {
                    holder as FooterViewHolder
                    holder.bind(footerFlag)
                }
                else -> { }
            }
        }

        override fun getItemCount() = list.size + 1

        fun addItems(items : MutableList<AddonModel>) {
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

    inner class TaskViewHolder(private val binding: ItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: AddonModel){
            val title = model.title

            binding.apply {
                Glide.with(requireActivity()).load(model.image).centerCrop().into(imageView)
                txtInstall.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title))
                }
                txtInstall.text = title
                btnDownload.setOnClickListener {
                    if (checkPermission()) { dialogDownload(model, DownloadAddon.DIR_CACHE) }
                }
                itemView.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title))
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
