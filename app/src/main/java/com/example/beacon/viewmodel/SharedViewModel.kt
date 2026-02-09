package com.example.beacon.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {

    // Check-in state
    private val _isCheckedIn = MutableLiveData<Boolean>(false)
    val isCheckedIn: LiveData<Boolean> = _isCheckedIn

    // Location data
    private val _lastUpdateLocation = MutableLiveData<Pair<Double, Double>?>()
    val lastUpdateLocation: LiveData<Pair<Double, Double>?> = _lastUpdateLocation

    // GPS alert countdown state
    private val _isGpsAlertCountDownActive = MutableLiveData<Boolean>(false)
    val isGpsAlertCountDownActive: LiveData<Boolean> = _isGpsAlertCountDownActive

    // Check-in dialog visibility
    private val _isCheckInDialogVisible = MutableLiveData<Boolean>(false)
    val isCheckInDialogVisible: LiveData<Boolean> = _isCheckInDialogVisible

    // Daily check-in count
    private val _dailyCheckInCount = MutableLiveData<Int>(0)
    val dailyCheckInCount: LiveData<Int> = _dailyCheckInCount

    /**
     * Increment daily check-in count
     */
    fun incrementDailyCheckIn() {
        viewModelScope.launch {
            val currentCount = _dailyCheckInCount.value ?: 0
            _dailyCheckInCount.postValue(currentCount + 1)
        }
    }

    /**
     * Check in the user
     */
    fun checkIn() {
        viewModelScope.launch {
            _isCheckedIn.postValue(true)
            _isCheckInDialogVisible.postValue(true)
            // Hide dialog after 3 seconds
            kotlinx.coroutines.delay(3000)
            _isCheckInDialogVisible.postValue(false)
        }
    }

    /**
     * Check out the user
     */
    fun checkOut() {
        viewModelScope.launch {
            _isCheckedIn.postValue(false)
        }
    }

    /**
     * Get current check-in status
     */
    fun isCheckedIn(): Boolean {
        return _isCheckedIn.value ?: false
    }

    /**
     * Update last known location
     */
    fun updateLastLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _lastUpdateLocation.postValue(Pair(latitude, longitude))
        }
    }

    /**
     * Start GPS alert countdown
     */
    fun startGpsAlertCountdown() {
        viewModelScope.launch {
            _isGpsAlertCountDownActive.postValue(true)
        }
    }

    /**
     * Stop GPS alert countdown
     */
    fun stopGpsAlertCountdown() {
        viewModelScope.launch {
            _isGpsAlertCountDownActive.postValue(false)
        }
    }

    /**
     * Called when location permission is granted
     */
    fun onLocationPermissionGranted() {
        // Start location-related operations
        startGpsAlertCountdown()
    }

    /**
     * Hide check-in dialog manually
     */
    fun hideCheckInDialog() {
        viewModelScope.launch {
            _isCheckInDialogVisible.postValue(false)
        }
    }
}