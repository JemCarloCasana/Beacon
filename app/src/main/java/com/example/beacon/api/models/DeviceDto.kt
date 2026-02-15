package com.example.beacon.api.models

data class DeviceDto(
    val id: Long,
    val user_id: Long,
    val fcm_token: String,
    val platform: String,
    val created_at: String,
    val updated_at: String
)
