package com.example.minecraft.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentDetailBinding
import com.example.minecraft.databinding.ItemPagerBinding
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "DetailFragment"
@AndroidEntryPoint
class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_detailt))
        binding.apply {
            val list: List<String> = args.model.preview
            imgContainer?.adapter = DetailPageAdapter(list, args.title, requireActivity())
            btnShare.setOnClickListener {
                Log.d(TAG, "onViewCreated: Click")
                val state = txtDesc.isVisible
                if (!state) {
                    txtDesc.visibility = View.VISIBLE
                }else{
                    txtDesc.visibility = View.GONE
                }
            }
            btnInstall.setOnClickListener {
                findNavController().navigate(DetailFragmentDirections.blankFragment())
            }
            btnAddon.setOnClickListener {
                findNavController().navigate(DetailFragmentDirections.blankFragment())
            }
//            btnAddon.text = args.model.title
            txtDesc.text = args.model.description
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun setupToolBartTitle(title: String){
        (activity as MainActivity?)!!.setupToolBartTitle(title)
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
            val txt_title = view.findViewById<TextView>(R.id.txt_title)
            txt_title.text = title
            Glide.with(requireActivity()).load(listOfLinks[position]).centerCrop().into(image)
            container.addView(view)

            return  view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

    }
}