package com.example.minecraft.ui.trial

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentTrialBinding
import com.example.minecraft.ui.main.DetailFragment
import com.example.minecraft.ui.main.DetailFragmentDirections
import com.example.minecraft.ui.main.MainFragment
import com.example.minecraft.ui.main.MainViewModel
import com.example.minecraft.ui.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.internal.wait


@AndroidEntryPoint
class TrialFragment : Fragment() {
    private val TAG = "TrialFragment"
    private var _binding: FragmentTrialBinding? = null
    private val binding get() = _binding!!

    private val args: TrialFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    private lateinit var trialManager: TrialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trialManager = TrialManager(requireActivity())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_trial))

        binding.btnPremium.setOnClickListener { button ->
            requireActivity().lifecycleScope.launch{
                trialManager.setTrial()
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