package com.example.beacon.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentHomeBinding
import com.example.beacon.sos.SosStateMachine
import com.example.beacon.sos.SosEvent
import com.example.beacon.sos.SosState
import com.example.beacon.viewmodel.ActivityViewModel
import com.example.beacon.viewmodel.LocationStatusViewModel
import com.example.beacon.viewmodel.LocationViewModel
import com.example.beacon.viewmodel.SosViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private val activityViewModel: ActivityViewModel by activityViewModels()
    private val sosViewModel: SosViewModel by activityViewModels()

    // We use BOTH viewmodels now (Data + Status)
    private val locationViewModel: LocationViewModel by activityViewModels()
    private val locationStatusViewModel: LocationStatusViewModel by activityViewModels()

    // SOS State Machine - replaces old ObjectAnimator approach
    private lateinit var sosStateMachine: SosStateMachine

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

        setupSosStateMachine()
        setupSosButton()
        setupCheckInButton()
        setupContactsButton()

        observeViewModels()

        // Start GPS automatically
        checkPermissionsAndStartGps()
    }

    override fun onResume() {
        super.onResume()
        // Reset state machine when fragment resumes (user navigates back)
        // This ensures clean state regardless of previous navigation
        if (::sosStateMachine.isInitialized) {
            sosStateMachine.handleEvent(SosEvent.SosDeactivated)
        }
        
        // Re-enable SOS button touch after returning from navigation
        setupSosButton()
    }

    private fun checkPermissionsAndStartGps() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.onPermissionGranted() // Notify VM
        } else {
            locationViewModel.onPermissionDenied() // Notify VM
            locationStatusViewModel.setLocationDisabled()
        }
    }

    private fun observeViewModels() {
        // 1. SOS State Machine - replaces old state management
        observeSosStateMachine()
        
        // 2. SOS Active State from SosViewModel - still needed for background changes
        sosViewModel.isSosActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                binding.tvInstruction.text = "SOS ACTIVE"
                // Use visual indication for active state - change background color
                binding.sosContainer.setBackgroundResource(R.drawable.circle_sos_active_gradient)
                binding.sosProgressRing.visibility = View.INVISIBLE
            } else {
                // Reset to normal background when not active
                binding.sosContainer.setBackgroundResource(R.drawable.circle_sos_gradient)
            }
        }

        // 3. BRIDGE LOCATION DATA (Critical Logic)
        // We get raw data from LocationViewModel...
        locationViewModel.locationData.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                // ...and feed it to the other ViewModels
                locationStatusViewModel.onLocationAcquired() // Update Green Card
                sosViewModel.updateCurrentLocation(location) // Update SOS
                activityViewModel.updateUserLocation(location.latitude, location.longitude) // Update DB
            }
        }

        // Listen for errors from the GPS engine
        locationViewModel.errorEvent.observe(viewLifecycleOwner) { errorMsg ->
            locationStatusViewModel.onLocationError(errorMsg)
        }

        // 4. OBSERVE LOCATION STATUS (UI Updates)
        // We get the COLORS/TEXT from LocationStatusViewModel
        locationStatusViewModel.locationState.observe(viewLifecycleOwner) { state ->
            val lastTime = locationStatusViewModel.lastUpdateTime.value ?: 0L

            when (state) {
                LocationStatusViewModel.LocationState.ACQUIRED -> {
                    updateLocationCard(true, lastTime, false, Color.WHITE)
                }
                LocationStatusViewModel.LocationState.SEARCHING -> {
                    updateLocationCard(false, 0L, false, Color.parseColor("#FFF3E0"))
                }
                LocationStatusViewModel.LocationState.DISABLED -> {
                    updateLocationCard(false, 0L, true, Color.WHITE)
                }
                else -> { // ERROR or unknown
                    updateLocationCard(false, 0L, true, Color.parseColor("#FFEBEE"))
                }
            }
        }
    }

    /**
     * Observe SOS State Machine for reliable state management
     */
    private fun observeSosStateMachine() {
        // Observe state changes
        sosStateMachine.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SosState.Idle -> {
                    binding.sosProgressRing.visibility = View.INVISIBLE
                    binding.sosProgressRing.progress = 0
                    // Only update text if SOS is not active (to avoid conflict with SosViewModel observer)
                    if (sosViewModel.isSosActive.value != true) {
                        binding.tvInstruction.text = "Press in case of emergency"
                    }
                }
                is SosState.CountingDown -> {
                    binding.sosProgressRing.visibility = View.VISIBLE
                    binding.tvInstruction.text = "Hold to trigger SOS..."
                }
                is SosState.Activated -> {
                    binding.sosProgressRing.visibility = View.INVISIBLE
                    binding.sosProgressRing.progress = 0
                }
                is SosState.Cancelled -> {
                    binding.sosProgressRing.visibility = View.INVISIBLE
                    binding.sosProgressRing.progress = 0
                }
            }
        }
        
        // Observe progress updates
        sosStateMachine.progress.observe(viewLifecycleOwner) { progress ->
            binding.sosProgressRing.progress = progress
        }
    }

    /**
     * Setup SOS State Machine - replaces old ObjectAnimator approach
     */
    private fun setupSosStateMachine() {
        sosStateMachine = SosStateMachine()
        
        // Set callback for SOS activation
        sosStateMachine.setOnSosActivatedCallback {
            triggerSosActivation()
        }
        
        // Initialize progress bar for state machine (0-100 range instead of 0-3000)
        binding.sosProgressRing.max = 100
        binding.sosProgressRing.progress = 0
        binding.sosProgressRing.isIndeterminate = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSosButton() {
        binding.sosContainer.setOnTouchListener { _, event ->
            // If already active, just navigate
            if (sosViewModel.isSosActive.value == true) {
                findNavController().navigate(R.id.action_homeFragment_to_sosActivationFragment)
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start SOS countdown using state machine
                    sosStateMachine.handleEvent(SosEvent.PressStarted)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Stop SOS countdown using state machine
                    sosStateMachine.handleEvent(SosEvent.PressReleased)
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerSosActivation() {
        // Disable touch during activation to prevent multiple triggers
        binding.sosContainer.setOnTouchListener(null)
        
        // Reset state machine to clean state
        sosStateMachine.reset()

        sosViewModel.startSos()
        findNavController().navigate(R.id.action_homeFragment_to_sosActivationFragment)
    }

    private fun updateLocationCard(isActive: Boolean, lastUpdateTime: Long, isDisabled: Boolean, backgroundColor: Int = Color.WHITE) {
        binding.locationCard.setCardBackgroundColor(backgroundColor)

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

    private fun setupCheckInButton() {
        binding.btnCheckIn.setOnClickListener {
            Toast.makeText(requireContext(), "Check-in successful", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupContactsButton() {
        binding.btnContacts.setOnClickListener {
            val contactNumber = "09171234567"
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clean up state machine
        if (::sosStateMachine.isInitialized) {
            sosStateMachine.cleanup()
        }
        
        // Clean up location updates
        locationViewModel.stopLocationUpdates()
        _binding = null
    }
}