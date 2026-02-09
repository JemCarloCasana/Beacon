package com.example.beacon.profile

data class UserProfile(
    val fullName: String,
    val email: String,
    val address: String,
    val phoneNumber: String?,
    val profileImageUrl: String?
)