package com.example.aqualuminus.data.model

data class WiFiSetupResponse(
    val success: Boolean,
    val ip: String?,
    val message: String,
    val device: String? = null,
    val hostname: String? = null,
    val setup_complete: Boolean = false
)
