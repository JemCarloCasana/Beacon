package com.example.beacon.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.beacon.ActivityViewModel
import com.example.beacon.sos.SosViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationViewModel(
    application: Application,
    private val activityViewModel: ActivityViewModel,
    private val sosViewModel: SosViewModel,
    private val locationStatusViewModel: LocationStatusViewModel
) : AndroidViewModel(application) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L // every 5 seconds
    ).build()

    // 1. Refactored Logic: Central place to handle ALL location updates
    private fun processLocation(location: Location) {
        try {
            locationStatusViewModel.onLocationAcquired()
            activityViewModel.updateUserLocation(location.latitude, location.longitude)
            sosViewModel.updateCurrentLocation(location)

            Log.d("LocationViewModel", "GPS Processed: ${location.latitude}, ${location.longitude}")
        } catch (e: Exception) {
            Log.e("LocationViewModel", "Error processing GPS", e)
            onLocationError(e.message ?: "Unknown error")
        }
    }

    // 2. The Internal Callback (FusedLocation)
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                processLocation(location)
            }
        }
    }

    // 3. THE MISSING FUNCTION (Fixes your error)
    // This allows MapFragment to pass location updates to this ViewModel manually
    fun onLocationUpdate(location: Location) {
        processLocation(location)
    }

    // 🚀 START REAL GPS
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
        Log.d("LocationViewModel", "Started GPS updates")
    }

    fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(callback)
        Log.d("LocationViewModel", "Stopped GPS updates")
    }

    fun onLocationError(error: String) {
        locationStatusViewModel.onLocationError(error)
    }

    fun onPermissionDenied() {
        locationStatusViewModel.setLocationDisabled()
    }

    fun onPermissionGranted() {
        locationStatusViewModel.setLocationSearching()
        startLocationUpdates()
    }
}