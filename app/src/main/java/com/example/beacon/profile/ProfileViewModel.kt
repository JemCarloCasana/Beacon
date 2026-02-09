package com.example.beacon.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beacon.util.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Profile data
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    // Loading states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Events
    private val _updateSuccess = MutableLiveData<Event<Unit>>()
    val updateSuccess: LiveData<Event<Unit>> = _updateSuccess

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            _userProfile.value = null
            return
        }

        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // Get additional profile data from Firestore
                val docRef = firestore.collection("users").document(user.uid)
                val document = docRef.get().await()
                
                android.util.Log.d("ProfileViewModel", "Loading profile for user: ${user.uid}")
                
                if (document.exists()) {
                    android.util.Log.d("ProfileViewModel", "Firestore document exists, data: ${document.data}")
                } else {
                    android.util.Log.w("ProfileViewModel", "Firestore document does not exist for user: ${user.uid}")
                }
                
                // Migration logic: try new field first, fallback to old field
                val address = document.getString("address") 
                    ?: document.getString("affiliation") 
                    ?: ""
                
                val profile = UserProfile(
                    fullName = user.displayName ?: "",
                    email = user.email ?: "",
                    address = address,
                    phoneNumber = document.getString("phoneNumber"),
                    profileImageUrl = document.getString("profileImageUrl")
                )
                
                android.util.Log.d("ProfileViewModel", "Created UserProfile: $profile")
                _userProfile.value = profile
                
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("permission-denied") == true -> 
                        "Permission denied while loading profile."
                    e.message?.contains("unavailable") == true -> 
                        "Network error while loading profile."
                    else -> "Failed to load profile: ${e.message}"
                }
                
                // Log detailed error for debugging
                android.util.Log.e("ProfileViewModel", "Profile load failed", e)
                
                _errorMessage.value = Event(errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        fullName: String,
        address: String,
        phoneNumber: String?
    ) {
        val user = auth.currentUser ?: return
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "Updating profile for user: ${user.uid}")
                android.util.Log.d("ProfileViewModel", "Update data - fullName: $fullName, address: $address, phone: $phoneNumber")
                
                // Update Firebase Auth profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()
                
                user.updateProfile(profileUpdates).await()
                android.util.Log.d("ProfileViewModel", "Firebase Auth profile updated successfully")
                
                // Update Firestore document
                val updates = mutableMapOf<String, Any>(
                    "address" to address,
                    "phoneNumber" to (phoneNumber ?: "")
                )
                
                firestore.collection("users").document(user.uid)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                android.util.Log.d("ProfileViewModel", "Firestore document updated successfully")
                
                // Reload profile data
                loadUserProfile()
                
                _updateSuccess.value = Event(Unit)
                
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("no document to update") == true -> 
                        "Profile not found. Please complete your profile setup."
                    e.message?.contains("permission-denied") == true -> 
                        "Permission denied. Please try again."
                    e.message?.contains("unavailable") == true -> 
                        "Network error. Please check your connection."
                    e.message?.contains("deadline-exceeded") == true -> 
                        "Request timeout. Please try again."
                    else -> "Failed to update profile: ${e.message}"
                }
                
                // Log detailed error for debugging
                android.util.Log.e("ProfileViewModel", "Profile update failed", e)
                
                _errorMessage.value = Event(errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}