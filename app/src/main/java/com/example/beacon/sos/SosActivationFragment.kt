package com.example.beacon.sos

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentSosActivationBinding

class SosActivationFragment : Fragment() {
    private lateinit var binding: FragmentSosActivationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSosActivationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Trigger Shake Animation
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.screen_shake)
        binding.tvSosSent.startAnimation(shake)

        // 2. Auto-navigate after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) { // Check if fragment is still attached
                findNavController().navigate(R.id.action_activation_to_context)
            }
        }, 2000)
    }
}