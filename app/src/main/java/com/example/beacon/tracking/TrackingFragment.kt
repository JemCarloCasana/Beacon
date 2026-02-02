package com.example.beacon.sos

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentTrackingBinding
import com.example.beacon.viewmodel.SosViewModel

class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: SosViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentTrackingBinding.bind(view)

        // Observe status (Later we will connect this to Realtime Postgres updates)
        viewModel.trackingStatus.observe(viewLifecycleOwner) { status ->
            binding.tvStatusDetail.text = when(status) {
                "CONNECTED" -> "Responder Connected! They are 2km away."
                "ARRIVED" -> "Responder has arrived."
                else -> "Broadcasting location to all nearby units..."
            }
        }

        binding.btnImSafe.setOnClickListener {
            viewModel.cancelSos()
            // Navigate back to Map (Home)
            findNavController().popBackStack(R.id.nav_home, false)
        }
    }
}