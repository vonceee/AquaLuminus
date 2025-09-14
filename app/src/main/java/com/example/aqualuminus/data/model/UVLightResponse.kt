package com.example.aqualuminus.data.model

data class UVLightResponse(
    val success: Boolean = true,
    val uvLightOn: Boolean,
    val status: String? = null,
    val message: String? = null,
    val timestamp: Long,
    val device: String? = null
)
