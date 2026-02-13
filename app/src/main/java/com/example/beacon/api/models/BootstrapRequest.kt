package com.example.beacon.api.models

data class BootstrapRequest(
    val full_name: String,
    val phone_number: String? = null
)
