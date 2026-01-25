package com.example.beacon.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.beacon.auth.LoginActivity
import com.example.beacon.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var sosTimer: CountDownTimer? = null
    private val SOS_DURATION = 3000L // 3 seconds in milliseconds

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
    }

    /** ---------------- SOS Button Logic ---------------- */
    private fun setupSosButton() {
        binding.sosContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startSosTimer()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelSosTimer()
                    true
                }
                else -> false
            }
        }
    }

    private fun startSosTimer() {
        binding.tvInstruction.text = "Hold to trigger SOS..."
        sosTimer = object : CountDownTimer(SOS_DURATION, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // Optional: update progress or animation
            }

            override fun onFinish() {
                triggerSos()
            }
        }.start()
    }

    private fun cancelSosTimer() {
        sosTimer?.cancel()
        binding.tvInstruction.text = "Press in case of emergency"
    }

    private fun triggerSos() {
        Toast.makeText(requireContext(), "SOS Triggered!", Toast.LENGTH_SHORT).show()
        binding.tvInstruction.text = "SOS sent!"
        // TODO: Add actual SOS logic: send location, notify contacts, etc.
    }

    /** ---------------- Check-in Button ---------------- */
    private fun setupCheckInButton() {
        binding.btnCheckIn.setOnClickListener {
            Toast.makeText(requireContext(), "Check-in successful", Toast.LENGTH_SHORT).show()
            // TODO: Update Firebase / mark user as safe
        }
    }

    /** ---------------- Emergency Contacts ---------------- */
    private fun setupContactsButton() {
        binding.btnContacts.setOnClickListener {
            // Example: open dialer with first emergency contact
            val contactNumber = "09171234567" // Replace with real contact
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
            startActivity(intent)
        }
    }

    /** ---------------- Logout Button ---------------- */
    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    /** ---------------- Location Card ---------------- */
    private fun setupLocationCard() {
        binding.tvLocationTitle.text = "Location Active"
        binding.tvLocationSubtitle.text = "Sharing with 3 contacts"
        // TODO: Integrate MapLibre or GPS later
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sosTimer?.cancel()
        _binding = null
    }
}
