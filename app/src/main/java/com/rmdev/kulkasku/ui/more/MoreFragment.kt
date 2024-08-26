package com.rmdev.kulkasku.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentMoreBinding

class MoreFragment : Fragment() {

    private lateinit var moreViewModel: MoreViewModel
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        moreViewModel = ViewModelProvider(this)[MoreViewModel::class.java]
        _binding = FragmentMoreBinding.inflate(inflater, container, false).apply {
            viewModel = moreViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load images with CircleCrop transformation using Glide
        Glide.with(this)
            .load(R.drawable.ic_academy)
            .transform(CircleCrop())
            .into(binding.icAcademy)

        Glide.with(this)
            .load(R.drawable.ic_tips)
            .transform(CircleCrop())
            .into(binding.icTips)

        binding.icAcademy.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_academyFragment)
        }

        binding.icTips.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_tipsFragment)
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
