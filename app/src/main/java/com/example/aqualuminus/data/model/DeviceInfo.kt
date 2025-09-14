package com.example.aqualuminus.data.model

data class DeviceInfo(
    val device: String,
    val version: String,
    val ip: String,
    val mac: String,
    val rssi: Int?,
    val hostname: String?
)
