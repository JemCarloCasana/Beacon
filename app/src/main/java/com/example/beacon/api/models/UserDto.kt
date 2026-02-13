package com.example.beacon.api.models

data class UserDto(
    val id: Long,
    val firebase_uid: String,
    val email: String,
    val full_name: String,
    val phone_number: String?,
    val role: String
)
