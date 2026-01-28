package com.example.beacon

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.beacon.auth.LoginActivity
import com.example.beacon.databinding.ActivityMainBinding
import com.example.beacon.home.HomeFragment
import com.example.beacon.sos.SosViewModel
import com.example.beacon.viewmodel.LocationStatusViewModel
import com.example.beacon.viewmodel.LocationViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val activityViewModel: ActivityViewModel by viewModels()
    private val sosViewModel: SosViewModel by viewModels()
    private val locationStatusViewModel: LocationStatusViewModel by viewModels()

    // Initialize LocationViewModel with dependencies using a custom factory
    private val locationViewModel: LocationViewModel by viewModels {
        LocationViewModelFactory(
            application,
            activityViewModel,
            sosViewModel,
            locationStatusViewModel
        )
    }

    // Permission launcher for the Activity
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                locationViewModel.onPermissionGranted()
            } else {
                locationViewModel.onPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start location permission flow immediately after login/create
        requestLocationPermissions()

        // Observe navigation event
        activityViewModel.navigateToLogin.observe(this) { event ->
            event.getContentIfNotHandled()?.let { // Only proceed if the event has not been handled
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // Setup BottomNavigation actions
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_map -> {
                    replaceFragment(MapFragment())
                    true
                }
                R.id.nav_profile -> {
                    activityViewModel.logout() // Call logout from the ViewModel
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Note: LocationComponent now handles its own lifecycle internally
    // No need to manually start/stop location engine

    // Method to request permissions
    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

// Custom Factory for LocationViewModel
class LocationViewModelFactory(
    private val application: Application,
    private val activityViewModel: ActivityViewModel,
    private val sosViewModel: SosViewModel,
    private val locationStatusViewModel: LocationStatusViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(application, activityViewModel, sosViewModel, locationStatusViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
