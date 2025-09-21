package com.example.aqualuminus.data.model

data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val hostname: String,
    val device_id: String? = null,
    val device_name: String? = null
)
