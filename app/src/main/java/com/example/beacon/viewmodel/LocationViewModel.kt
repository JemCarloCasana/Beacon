package com.example.beacon.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    // === 1. LIVE DATA ===
    private val _locationData = MutableLiveData<Location>()
    val locationData: LiveData<Location> = _locationData

    // Events to notify HomeFragment to update LocationStatusViewModel
    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> = _errorEvent

    private val _permissionEvent = MutableLiveData<Boolean>() // true = granted, false = denied
    val permissionEvent: LiveData<Boolean> = _permissionEvent

    // === 2. GPS CONFIG ===
    private val fusedClient = LocationServices.getFusedLocationProviderClient(application)
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L
    ).build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                processLocation(location)
            }
        }
    }

    // === 3. METHODS CALLED BY MAP FRAGMENT ===
    // These were "Unresolved" before. Now they exist!

    fun onLocationUpdate(location: Location) {
        processLocation(location)
    }

    fun onLocationError(error: String) {
        Log.e("LocationViewModel", "Location Error: $error")
        _errorEvent.value = error
    }

    fun onPermissionDenied() {
        _permissionEvent.value = false
    }

    fun onPermissionGranted() {
        _permissionEvent.value = true
        startLocationUpdates()
    }

    // === 4. INTERNAL LOGIC ===
    private fun processLocation(location: Location) {
        _locationData.value = location
        Log.d("LocationViewModel", "GPS: ${location.latitude}, ${location.longitude}")
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            onPermissionDenied()
        } catch (e: Exception) {
            onLocationError(e.message ?: "Unknown GPS start error")
        }
    }

    fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(callback)
    }
}