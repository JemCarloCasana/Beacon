package com.example.beacon.home

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.beacon.ActivityViewModel
import com.example.beacon.databinding.FragmentHomeBinding
import com.example.beacon.sos.SosViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: ActivityViewModel by activityViewModels()
    // --- THIS IS THE CRITICAL CHANGE ---
    // Get the SOS ViewModel that is shared with the Activity and other fragments.
    private val sosViewModel: SosViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSosButton()
        setupCheckInButton()
        setupContactsButton()
        setupLogoutButton()
        setupLocationCard()
        observeViewModels()
    }

    private fun observeViewModels() {
        activityViewModel.sosState.observe(viewLifecycleOwner) { state ->
            if (state == ActivityViewModel.SosState.COUNTDOWN) {
                binding.tvInstruction.text = "Hold to trigger SOS..."
            }
        }

        activityViewModel.sosTriggered.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                sosViewModel.startSos()
            }
        }

        sosViewModel.isSosActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                binding.tvInstruction.text = "SOS ACTIVE\n(Long press to cancel)"
            } else {
                binding.tvInstruction.text = "Press in case of emergency"
            }
        }
    }

    /** ---------------- SOS Button Logic ---------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSosButton() {
        binding.sosContainer.setOnTouchListener { _, event ->
            if (sosViewModel.isSosActive.value == true) {
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activityViewModel.startSosCountdown()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activityViewModel.cancelSosCountdown()
                    true
                }
                else -> false
            }
        }

        binding.sosContainer.setOnLongClickListener {
            if (sosViewModel.isSosActive.value == true) {
                sosViewModel.stopSos()
                Toast.makeText(requireContext(), "SOS STOPPED", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }

    /** ---------------- Check-in Button ---------------- */

    private fun setupCheckInButton() {
        binding.btnCheckIn.setOnClickListener {
            Toast.makeText(requireContext(), "Check-in successful", Toast.LENGTH_SHORT).show()
        }
    }

    /** ---------------- Emergency Contacts ---------------- */

    private fun setupContactsButton() {
        binding.btnContacts.setOnClickListener {
            val contactNumber = "09171234567" // Example number
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
            startActivity(intent)
        }
    }

    /** ---------------- Logout Button ---------------- */

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            activityViewModel.logout()
        }
    }

    /** ---------------- Location Card ---------------- */

    private fun setupLocationCard() {
        binding.tvLocationTitle.text = "Location Active"
        binding.tvLocationSubtitle.text = "Sharing location when SOS is active"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
