package com.example.beacon.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beacon.util.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {

    // TRIGGERED is removed as it's an event, not a state.
    enum class SosState { IDLE, COUNTDOWN }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _navigateToLogin = MutableLiveData<Event<Unit>>()
    val navigateToLogin: LiveData<Event<Unit>> = _navigateToLogin

    // This represents the UI state of the SOS button's countdown.
    private val _sosState = MutableLiveData<SosState>(SosState.IDLE)
    val sosState: LiveData<SosState> = _sosState

    // This is a one-shot event to signal that the SOS should be triggered.
    private val _sosTriggered = MutableLiveData<Event<Unit>>()
    val sosTriggered: LiveData<Event<Unit>> = _sosTriggered

    private val _userLocation = MutableLiveData<Pair<Double, Double>?>()
    val userLocation: LiveData<Pair<Double, Double>?> = _userLocation

    private var countdownJob: Job? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user == null) {
            _navigateToLogin.value = Event(Unit)
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
    }

    fun startSosCountdown() {
        if (_sosState.value == SosState.IDLE) {
            _sosState.value = SosState.COUNTDOWN
            countdownJob = viewModelScope.launch {
                delay(3000) // 3-second countdown
                // The countdown completed, fire the one-shot event.
                _sosTriggered.value = Event(Unit)
                // Reset the UI state back to idle.
                _sosState.value = SosState.IDLE
            }
        }
    }

    fun cancelSosCountdown() {
        countdownJob?.cancel()
        if (_sosState.value == SosState.COUNTDOWN) {
            _sosState.value = SosState.IDLE
        }
    }

    fun logout() {
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        countdownJob?.cancel()
    }
}