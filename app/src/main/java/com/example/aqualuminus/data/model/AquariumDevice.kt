package com.example.aqualuminus.data.model

data class AquariumDevice(
    val deviceId: String,
    val deviceName: String,
    val ip: String,
    val mac: String,
    val version: String,
    val hostname: String,
    val isConnected: Boolean = false,
    val status: DeviceStatus = DeviceStatus.OFFLINE
)

enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    ERROR,
    CONNECTING
}