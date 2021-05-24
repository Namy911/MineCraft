package com.example.minecraft.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.example.minecraft.databinding.FragmentSettingsBinding
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_setinga))
        binding.apply {
            btnHelp.setOnClickListener {  findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(getString(R.string.txt_help), getString(R.string.help)))}
            btnPolicy.setOnClickListener {  findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(getString(R.string.txt_privacy_policy), getString(R.string.privacy_policy))) }
            btnTerms.setOnClickListener { findNavController().navigate(SettingsFragmentDirections.settingsDetailFragment(getString(R.string.txt_terms_of_usage), getString(R.string.terms_of_usage))) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(title: String){
        (activity as MainActivity?)!!.setupToolBartTitle(title)
    }
}