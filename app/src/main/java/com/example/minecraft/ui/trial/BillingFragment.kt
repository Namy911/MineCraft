package com.example.minecraft.ui.trial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.LayoutPremiumBinding
import com.example.minecraft.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class BillingFragment : Fragment() {
    private val TAG = "BillingFragment"
    private var _binding: LayoutPremiumBinding? = null
    private val binding get() = _binding!!

//    private val args: BillingFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_trial))

        binding.btnPremium.setOnClickListener { button ->
            requireActivity().lifecycleScope.launch{
                button.findNavController().popBackStack()
            }
        }
        Glide.with(requireActivity()).load("https://media.giphy.com/media/QGnhDpnrr7qhy/giphy.gif").into(binding.gifStub)

        val animBtn = AnimationUtils.loadAnimation(requireActivity(), R.anim.btn_premium)
        val animTxt = AnimationUtils.loadAnimation(requireActivity(), R.anim.txt_premium)
        binding.btnPremium.animation = animBtn
        binding.txtBtnTrial.animation = animTxt
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(title: String){ (activity as MainActivity?)!!.setupToolBartTitle(title) }
}