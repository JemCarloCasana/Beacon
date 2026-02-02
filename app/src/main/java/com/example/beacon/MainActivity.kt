package com.example.beacon

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.beacon.auth.LoginActivity
import com.example.beacon.databinding.ActivityMainBinding
import com.example.beacon.viewmodel.ActivityViewModel
import com.example.beacon.viewmodel.LocationViewModel
import com.example.beacon.viewmodel.SosViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val activityViewModel: ActivityViewModel by viewModels()
    private val sosViewModel: SosViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()

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

        requestLocationPermissions()

        activityViewModel.navigateToLogin.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // 1. SETUP NAVIGATION COMPONENT
        // This finds the NavHostFragment in your XML
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 2. CONNECT BOTTOM NAV TO CONTROLLER
        // This handles fragment switching automatically based on IDs!
        binding.bottomNav.setupWithNavController(navController)

        // ⚠️ DELETED: The manual setOnItemSelectedListener block.
        // We removed the code that forced a logout here.
        // Now, clicking "Profile" will simply navigate to the ProfileFragment.
    }

    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}