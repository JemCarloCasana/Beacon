package com.example.beacon.api.models

data class DeviceRegisterRequest(
    val fcm_token: String,
    val platform: String = "android"
)
