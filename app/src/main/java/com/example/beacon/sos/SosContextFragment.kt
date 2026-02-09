package com.example.beacon.sos

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentSosContextBinding
import com.example.beacon.viewmodel.SosViewModel

class SosContextFragment : Fragment(R.layout.fragment_sos_context) {

    private val viewModel: SosViewModel by activityViewModels()
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val COUNTDOWN_DURATION = 5000L // 5 seconds
        private const val UPDATE_INTERVAL = 1000L // Update every second
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSosContextBinding.bind(view)

        // Helper function to handle clicks
        fun setContextAndNavigate(context: String) {
            countdownTimer?.cancel() // Cancel countdown when user makes a choice
            viewModel.setSosContext(context)
            findNavController().navigate(R.id.action_context_to_tracking)
        }

        // Setup countdown timer
        setupCountdownTimer(binding)

        binding.btnMedical.setOnClickListener { setContextAndNavigate("MEDICAL") }
        binding.btnFire.setOnClickListener { setContextAndNavigate("FIRE") }
        binding.btnViolence.setOnClickListener { setContextAndNavigate("CRIME") }
        binding.btnUnsure.setOnClickListener { setContextAndNavigate("GENERAL") }
    }

    private fun setupCountdownTimer(binding: FragmentSosContextBinding) {
        countdownTimer = object : CountDownTimer(COUNTDOWN_DURATION, UPDATE_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                binding.tvCountdown.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                // Auto-navigate with GENERAL context when no choice is made
                viewModel.setSosContext("GENERAL")
                findNavController().navigate(R.id.action_context_to_tracking)
            }
        }
        countdownTimer?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
    }
}