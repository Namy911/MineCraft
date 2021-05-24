package com.example.minecraft.ui.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.minecraft.R
import com.example.minecraft.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "DialogInstallFragment"
@AndroidEntryPoint
class DialogInstallFragment : DialogFragment(){
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.dialog_view, container, false)
        val btnAddon: Button = view.findViewById(R.id.btn_addon)
        val btnInstall: Button = view.findViewById(R.id.btn_install)
        btnInstall.setOnClickListener { actionInstall() }
        btnAddon.setOnClickListener { actionAddon() }
        return  view
    }

    private fun actionAddon() {
        findNavController().navigate(DialogInstallFragmentDirections.blankFragment())
//        viewModel.getBehavior()
    }

    private fun actionInstall() {
        findNavController().navigate(DialogInstallFragmentDirections.blankFragment())
//        viewModel.getResource()
    }
}