package com.example.minecraft.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.minecraft.R
import com.example.minecraft.databinding.FragmentSettingsDetailBinding
import com.example.minecraft.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsDetailFragment : Fragment() {

    private var _binding: FragmentSettingsDetailBinding? = null
    private val binding get() = checkNotNull(_binding) {"binding isn't initialized"}

    private val args: SettingsDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.text = args.text
        setupToolBartTitle(args.title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(title: String){
        (activity as MainActivity).setupToolBartTitle(title)
    }
}