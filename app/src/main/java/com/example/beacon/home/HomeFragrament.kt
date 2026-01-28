package com.example.beacon.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.beacon.ActivityViewModel
import com.example.beacon.R
import com.example.beacon.databinding.FragmentHomeBinding
import com.example.beacon.sos.SosViewModel
import com.example.beacon.viewmodel.LocationStatusViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel: ActivityViewModel by activityViewModels()
    private val sosViewModel: SosViewModel by activityViewModels()
    private val locationStatusViewModel: LocationStatusViewModel by activityViewModels()



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
        observeViewModels()
        observeLocationStatus()
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

private fun observeLocationStatus() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationStatusViewModel.setLocationDisabled()
            return
        }

        // Observe state changes for better UX
        locationStatusViewModel.locationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LocationStatusViewModel.LocationState.ACQUIRED -> {
                    val lastUpdate = locationStatusViewModel.lastUpdateTime.value ?: System.currentTimeMillis()
                    updateLocationCard(true, lastUpdate, false, Color.WHITE)
                }
                LocationStatusViewModel.LocationState.SEARCHING -> {
                    updateLocationCard(false, 0L, false, Color.parseColor("#FFF3E0"))  // Orange
                }
                LocationStatusViewModel.LocationState.STALE -> {
                    val lastUpdate = locationStatusViewModel.lastUpdateTime.value ?: 0L
                    updateLocationCard(false, lastUpdate, false, Color.parseColor("#FFEBEE"))  // Light Red
                }
                LocationStatusViewModel.LocationState.ERROR -> {
                    updateLocationCard(false, 0L, true, Color.parseColor("#FFEBEE"))  // Light Red
                }
                LocationStatusViewModel.LocationState.DISABLED -> {
                    updateLocationCard(false, 0L, true, Color.WHITE)
                }
            }
        }
        
        // Observe error messages
        locationStatusViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Log.w("HomeFragment", "Location error: $errorMsg")
            }
        }
    }

    private fun updateLocationCard(isActive: Boolean, lastUpdateTime: Long, isDisabled: Boolean, backgroundColor: Int = Color.WHITE) {
        // Set background color
        binding.locationCard.setBackgroundColor(backgroundColor)
        
        when {
            isDisabled -> {
                binding.tvLocationTitle.text = "Location Disabled"
                binding.tvLocationSubtitle.text = "Enable GPS to continue"
                binding.vIndicatorLarge.setBackgroundResource(R.drawable.circle_background_red)
                binding.statusIndicatorSmall.setBackgroundResource(R.drawable.status_indicator_red)
            }
            isActive -> {
                binding.tvLocationTitle.text = "Location Active"
                binding.tvLocationSubtitle.text = formatLastUpdateTime(lastUpdateTime)
                binding.vIndicatorLarge.setBackgroundResource(R.drawable.circle_background_green)
                binding.statusIndicatorSmall.setBackgroundResource(R.drawable.status_indicator_green)
            }
            else -> {
                binding.tvLocationTitle.text = "Searching for GPS..."
                binding.tvLocationSubtitle.text = "Acquiring signal..."
                binding.vIndicatorLarge.setBackgroundResource(R.drawable.circle_background_yellow)
                binding.statusIndicatorSmall.setBackgroundResource(R.drawable.status_indicator_yellow)
            }
        }
    }

    private fun formatLastUpdateTime(timestamp: Long): String {
        if (timestamp == 0L) return "No updates yet"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Last update: Just now"
            diff < 3600_000 -> "Last update: ${diff / 60_000} min ago"
            diff < 86_400_000 -> "Last update: ${diff / 3600_000} hour(s) ago"
            else -> {
                val date = java.util.Date(timestamp)
                val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                "Last update: ${format.format(date)}"
            }
        }
    }



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

    private fun setupCheckInButton() {
        binding.btnCheckIn.setOnClickListener {
            Toast.makeText(requireContext(), "Check-in successful", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupContactsButton() {
        binding.btnContacts.setOnClickListener {
            val contactNumber = "09171234567" // Example number
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
            startActivity(intent)
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            activityViewModel.logout()
        }
    }

override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
