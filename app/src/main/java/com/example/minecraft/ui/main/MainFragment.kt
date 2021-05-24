package com.example.minecraft.ui.main

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.ItemFooterBinding
import com.example.minecraft.databinding.ItemRecyclerBinding
import com.example.minecraft.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect


private const val TAG = "MainFragment"
@AndroidEntryPoint
class MainFragment : Fragment(){

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private val PAGE_SIZE: Int = 6

    private val recyclerScrollKey: String = "ui.main.scroll.key"

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
//        loadListPosition()
        // Setup recycler
        val adapter = PagingAdapter()
        binding.apply {
            container.adapter = adapter
            val manager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            container.layoutManager = manager
            container.addOnScrollListener(
                RecyclerScroller(manager, object : RecyclerScroller.ScrollListener{
                    override fun onLoadMore() {
                        // setup list
                        viewModel.setPageSize(adapter.itemCount - 1)
                        Log.d(TAG, "onLoadMore: ${viewModel.getPageSize()} -> ${adapter.itemCount - 1}")
                        viewModel.getPageSize()?.let { size -> viewModel.getLimit(size, PAGE_SIZE) }
                        adapter.footerFlag = true
                    }
                })
            )
        }
        // first in
        lifecycleScope.launchWhenStarted {
        if (savedInstanceState == null) {
                viewModel.getLimit(adapter.itemCount - 1, PAGE_SIZE)
            }
        }
        // Setup data limit
        lifecycleScope.launchWhenStarted {
            viewModel.list.collect { list ->
                adapter.addItems(list.toMutableList())
                adapter.footerFlag = false
            }
        }
    }

    fun saveListPosition() {
        val position = (binding.container.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        PreferenceManager.getDefaultSharedPreferences(requireActivity())
            .edit()
            .putInt(recyclerScrollKey, position)
            .apply()
    }

    fun loadListPosition() {
        val scrollPosition = PreferenceManager.getDefaultSharedPreferences(requireActivity()).getInt(recyclerScrollKey, 0)
        binding.container.scrollToPosition(scrollPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(title: String = getString(com.example.minecraft.R.string.app_name)){ (activity as MainActivity?)!!.setupToolBartTitle(title) }
    // Helpers
    inner class PagingAdapter(private val list: MutableList<Any> = mutableListOf()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
//            val lastPos = list.size - 1
            list.addAll(items)
            notifyDataSetChanged()
//            notifyItemRangeInserted(lastPos, items.size)
        }
    }

    inner class FooterViewHolder(private val binding: ItemFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
                btnInstall.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.dialogInstallFragment())
                }
                itemView.setOnClickListener {
                    findNavController().navigate(MainFragmentDirections.detailFragment(model,title))
//                    saveListPosition()
                }
            }
        }
    }
}
class RecyclerScroller(
    private val layout: LinearLayoutManager,
    private val listener: ScrollListener
) : RecyclerView.OnScrollListener() {
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
