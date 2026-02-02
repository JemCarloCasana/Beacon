package com.example.beacon.viewmodel // NOTE: Make sure package matches your file structure

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class SosViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var sosJob: Job? = null
    private var sessionId: String? = null

    // === EXISTING STATE ===
    private val _isSosActive = MutableLiveData(false)
    val isSosActive: LiveData<Boolean> = _isSosActive

    private val _currentLocation = MutableLiveData<Location?>()

    // === NEW STATE FOR SOS FLOW ===
    // Stores the type of emergency (General, Fire, Medical, etc.)
    private val _sosContext = MutableLiveData<String>("GENERAL")
    val sosContext: LiveData<String> = _sosContext

    // Stores the status of the help request (Searching, Connected, Arrived)
    private val _trackingStatus = MutableLiveData<String>("SEARCHING")
    val trackingStatus: LiveData<String> = _trackingStatus

    fun startSos() {
        if (_isSosActive.value == true) {
            Log.w("SosViewModel", "SOS start requested, but already active.")
            return
        }
        if (auth.currentUser == null) {
            Log.e("SosViewModel", "Cannot start SOS: User not logged in.")
            return
        }
        if (_currentLocation.value == null) {
            Log.e("SosViewModel", "Cannot start SOS: Location is not available yet.")
            return
        }

        _isSosActive.value = true
        // Create a new Session ID immediately
        sessionId = db.collection("sos_sessions").document().id
        Log.d("SosViewModel", "SOS Started. Session ID: $sessionId")

        sosJob = viewModelScope.launch {
            while (isActive) {
                updateFirestore(_currentLocation.value)
                delay(10000) // Wait 10 seconds between updates
            }
        }
    }

    /**
     * 🚀 NEW: Updates the emergency type in Firestore
     * Called when user clicks a button in SosContextFragment
     */
    fun setSosContext(contextType: String) {
        // 1. Update Local State
        _sosContext.value = contextType

        // 2. Update Firestore Document
        val currentSessionId = sessionId
        if (currentSessionId != null) {
            viewModelScope.launch {
                try {
                    db.collection("sos_sessions").document(currentSessionId)
                        .update("emergencyType", contextType)
                        .await()
                    Log.d("SosViewModel", "Emergency Context updated to: $contextType")
                } catch (e: Exception) {
                    Log.e("SosViewModel", "Failed to update emergency context", e)
                }
            }
        } else {
            Log.w("SosViewModel", "Cannot update context: Session ID is null")
        }
    }

    /**
     * 🚀 NEW: Resets everything when the user says "I AM SAFE"
     */
    fun cancelSos() {
        // 1. Stop the Firestore Loop
        stopSos()

        // 2. Reset UI States for next time
        _sosContext.value = "GENERAL"
        _trackingStatus.value = "SEARCHING"
    }

    // Renamed slightly to be private, as cancelSos() is the public entry point now
    // logic remains the same as your original stopSos
    private fun stopSos() {
        if (_isSosActive.value == false) return

        _isSosActive.value = false
        sosJob?.cancel()
        sosJob = null

        sessionId?.let { id ->
            viewModelScope.launch {
                try {
                    db.collection("sos_sessions").document(id)
                        .update("active", false, "endedAt", FieldValue.serverTimestamp())
                        .await()
                    Log.d("SosViewModel", "SOS Stopped. Session $id marked as inactive.")
                } catch (e: Exception) {
                    Log.e("SosViewModel", "Error marking session as inactive.", e)
                }
            }
        }
        sessionId = null
    }

    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    private suspend fun updateFirestore(location: Location?) {
        val uid = auth.currentUser?.uid ?: return
        val currentSessionId = sessionId ?: return

        if (location == null) return

        val docRef = db.collection("sos_sessions").document(currentSessionId)
        val documentExists = try {
            docRef.get().await().exists()
        } catch (e: Exception) {
            false
        }

        val sessionData = mutableMapOf<String, Any>(
            "userId" to uid,
            "active" to true,
            // Ensure emergency type is set, default to current value
            "emergencyType" to (_sosContext.value ?: "GENERAL"),
            "locations" to FieldValue.arrayUnion(
                mapOf(
                    "location" to GeoPoint(location.latitude, location.longitude),
                    "timestamp" to Date()
                )
            )
        )

        if (!documentExists) {
            sessionData["startedAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(sessionData, SetOptions.merge()).await()
            Log.d("SosViewModel", "Firestore updated for session: $currentSessionId")
        } catch (e: Exception) {
            Log.e("SosViewModel", "Error writing to Firestore", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSos()
    }
}