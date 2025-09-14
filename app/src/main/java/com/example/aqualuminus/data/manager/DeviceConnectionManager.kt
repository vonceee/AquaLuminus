package com.example.aqualuminus.data.manager

import android.util.Log
import com.example.aqualuminus.data.repository.UVLightRepository
import kotlinx.coroutines.delay

class DeviceConnectionManager(
    private val uvLightRepository: UVLightRepository
) {
    companion object {
        private const val TAG = "DeviceConnectionManager"
        private const val DISCOVERY_TIMEOUT = 8000L
    }

    suspend fun connectToDevice(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting device connection...")
            val result = uvLightRepository.findAndConnectAfterSetup()

            if (result.isSuccess) {
                Log.d(TAG, "Successfully connected to device")
                Result.success(Unit)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Connection failed"
                Log.e(TAG, "Connection failed: $errorMsg")
                Result.failure(Exception("No Device Detected."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            Result.failure(e)
        }
    }

    suspend fun forceDiscovery(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting device discovery...")
            uvLightRepository.startDeviceDiscovery()
            delay(DISCOVERY_TIMEOUT)

            val result = uvLightRepository.autoConnect()
            if (result.isSuccess) {
                Log.d(TAG, "Discovery and connection successful")
                Result.success(Unit)
            } else {
                Result.failure(Exception("No devices found. Check WiFi connection and device status."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        uvLightRepository.disconnect()
    }

    fun isOnSameNetwork(): Boolean = uvLightRepository.isOnSameNetwork()
    fun getCurrentWiFiName(): String? = uvLightRepository.getCurrentWiFiName()
}