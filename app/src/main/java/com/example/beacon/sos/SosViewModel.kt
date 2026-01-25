package com.example.beacon.sos

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
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

    private val _isSosActive = MutableLiveData(false)
    val isSosActive: LiveData<Boolean> = _isSosActive

    // LiveData to hold the user's location, which will be updated from MapFragment
    private val _currentLocation = MutableLiveData<Location?>()

    fun startSos() {
        if (_isSosActive.value == true) {
            Log.w("SosViewModel", "SOS start requested, but already active.")
            return
        }
        if (auth.currentUser == null) {
            Log.e("SosViewModel", "Cannot start SOS: User not logged in.")
            return
        }
        // *** KEY CHANGE: Check for location *before* starting the job ***
        if (_currentLocation.value == null) {
            Log.e("SosViewModel", "Cannot start SOS: Location is not available yet.")
            // Optionally, you can add a Toast message from the Fragment to inform the user
            return
        }

        _isSosActive.value = true
        sessionId = db.collection("sos_sessions").document().id
        Log.d("SosViewModel", "SOS Started. Session ID: $sessionId")

        sosJob = viewModelScope.launch {
            // Loop will continue as long as the coroutine is active
            while (isActive) {
                // Get the latest location and update Firestore
                // Now we are sure that _currentLocation is not null for the first run
                updateFirestore(_currentLocation.value)
                delay(10000) // Wait 10 seconds
            }
        }
    }

    fun stopSos() {
        if (_isSosActive.value == false) return

        _isSosActive.value = false
        sosJob?.cancel() // Stop the loop
        sosJob = null

        // Mark the session as inactive in Firestore
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
        sessionId = null // Clear the session ID
    }

    /**
     * This method is called from the outside (e.g., your MapFragment)
     * to provide the ViewModel with the user's current location.
     */
    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    /**
     * Writes the session data to Firestore.
     */
    private suspend fun updateFirestore(location: Location?) {
        val uid = auth.currentUser?.uid ?: return
        val currentSessionId = sessionId ?: return

        if (location == null) {
            Log.w("SosViewModel", "Skipping Firestore update: location is null.")
            return
        }

        // Check if the document already exists to decide whether to set 'startedAt'
        val docRef = db.collection("sos_sessions").document(currentSessionId)
        val documentExists = try {
            docRef.get().await().exists()
        } catch (e: Exception) {
            false
        }

        // Prepare the data to be saved
        val sessionData = mutableMapOf<String, Any>(
            "userId" to uid,
            "active" to true,
            "locations" to FieldValue.arrayUnion(
                mapOf(
                    "location" to GeoPoint(location.latitude, location.longitude),
                    "timestamp" to Date()
                )
            )
        )

        // Only add 'startedAt' if the document is being created for the first time
        if (!documentExists) {
            sessionData["startedAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(sessionData, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d("SosViewModel", "Successfully updated Firestore for session: $currentSessionId")
        } catch (e: Exception) {
            Log.e("SosViewModel", "Error writing to Firestore", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure the session is stopped if the ViewModel is destroyed
        stopSos()
    }
}
