package com.example.beacon.sos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentSosContextBinding
import com.example.beacon.viewmodel.SosViewModel

class SosContextFragment : Fragment(R.layout.fragment_sos_context) {

    private val viewModel: SosViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSosContextBinding.bind(view)

        // Helper function to handle clicks
        fun setContextAndNavigate(context: String) {
            viewModel.setSosContext(context)
            findNavController().navigate(R.id.action_context_to_tracking)
        }

        binding.btnMedical.setOnClickListener { setContextAndNavigate("MEDICAL") }
        binding.btnFire.setOnClickListener { setContextAndNavigate("FIRE") }
        binding.btnViolence.setOnClickListener { setContextAndNavigate("CRIME") }
        binding.btnUnsure.setOnClickListener { setContextAndNavigate("GENERAL") }
    }
}