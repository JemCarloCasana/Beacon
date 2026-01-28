package com.example.beacon.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationStatusViewModel : ViewModel() {

    enum class LocationState {
        DISABLED, SEARCHING, ACQUIRED, STALE, ERROR
    }

    // Private mutable LiveData for internal updates
    private val _isLocationActive = MutableLiveData(false)
    // Public immutable LiveData for UI observation
    val isLocationActive: LiveData<Boolean> = _isLocationActive

    // Private mutable LiveData for last update time
    private val _lastUpdateTime = MutableLiveData(0L)
    // Public immutable LiveData for UI observation
    val lastUpdateTime: LiveData<Long> = _lastUpdateTime

    // Location state for better UX
    private val _locationState = MutableLiveData<LocationState>(LocationState.DISABLED)
    val locationState: LiveData<LocationState> = _locationState

    // Error message for debugging and user feedback
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Stores the timestamp of the last successful location update
    private var lastUpdateTimeInternal: Long = 0

    /**
     * Called when location is successfully acquired.
     * Updates the timestamp and sets the status to active.
     */
    fun onLocationAcquired() {
        lastUpdateTimeInternal = System.currentTimeMillis()
        _lastUpdateTime.postValue(lastUpdateTimeInternal)
        _isLocationActive.postValue(true)
        _locationState.postValue(LocationState.ACQUIRED)
        _errorMessage.postValue(null)  // Clear any previous errors
    }

    /**
     * Called when location acquisition is in progress.
     * Sets the status to searching.
     */
    fun setLocationSearching() {
        _isLocationActive.postValue(false)
        _locationState.postValue(LocationState.SEARCHING)
        _errorMessage.postValue(null)
    }

    /**
     * Called when location becomes stale.
     * Sets the status to stale.
     */
    fun onLocationStale() {
        _locationState.postValue(LocationState.STALE)
        _errorMessage.postValue(null)
    }

    /**
     * Called when location acquisition fails.
     * Sets the status to error and logs the error message.
     */
    fun onLocationFailed() {
        _isLocationActive.postValue(false)
        _locationState.postValue(LocationState.ERROR)
        _errorMessage.postValue("Location acquisition failed")
    }

    /**
     * Called when location permissions are denied to set disabled state.
     */
    fun setLocationDisabled() {
        _isLocationActive.postValue(false)
        _locationState.postValue(LocationState.DISABLED)
        _errorMessage.postValue(null)
    }

    /**
     * Called when location-related errors occur.
     * Sets the status to error and logs the specific error message.
     */
    fun onLocationError(error: String) {
        _isLocationActive.postValue(false)
        _locationState.postValue(LocationState.ERROR)
        _errorMessage.postValue(error)
    }
}