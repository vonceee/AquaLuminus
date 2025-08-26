package com.example.aqualuminus.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
// Data classes for API responses
data class UVLightResponse(
    val success: Boolean = true,
    val uvLightOn: Boolean,
    val status: String? = null,
    val message: String? = null,
    val timestamp: Long,
    val device: String? = null
)

// Retrofit service interface
interface UVLightService {
    @GET("api/status")
    suspend fun getStatus(): Response<UVLightResponse>

    @POST("api/on")
    suspend fun turnOn(): Response<UVLightResponse>

    @POST("api/off")
    suspend fun turnOff(): Response<UVLightResponse>

    @POST("api/toggle")
    suspend fun toggle(): Response<UVLightResponse>
}

class UVLightRepository {

    // Network configuration - you might want to move this to a config file or inject it
    private val ESP32_IP = "192.168.254.199" // Change this to your ESP32's IP

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://$ESP32_IP/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val uvLightService = retrofit.create(UVLightService::class.java)

    // Connection status flow
    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    // UV Light timing
    private val _uvLightStartTime = MutableStateFlow<Long?>(null)
    val uvLightStartTime: Flow<Long?> = _uvLightStartTime.asStateFlow()

    private val _uvLightDuration = MutableStateFlow(0L)
    val uvLightDuration: Flow<Long> = _uvLightDuration.asStateFlow()

    /**
     * Get the current UV light status
     * @return Result<Boolean> - true if UV light is on, false if off
     */
    suspend fun getUVLightStatus(): Result<Boolean> {
        return try {
            val response = uvLightService.getStatus()
            if (response.isSuccessful && response.body() != null) {
                _isConnected.value = true
                val isOn = response.body()!!.uvLightOn
                updateUVLightTiming(isOn)
                Result.success(isOn)
            } else {
                _isConnected.value = false
                Result.failure(Exception("Failed to get status: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
    }

    /**
     * Turn on the UV light
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun turnOnUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService.turnOn() }
    }

    /**
     * Turn off the UV light
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun turnOffUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService.turnOff() }
    }

    /**
     * Toggle the UV light state
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun toggleUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService.toggle() }
    }

    /**
     * Private helper method to execute UV light commands
     */
    private suspend fun executeUVLightCommand(
        command: suspend () -> Response<UVLightResponse>
    ): Result<Boolean> {
        return try {
            val response = command()
            if (response.isSuccessful && response.body() != null) {
                _isConnected.value = true
                val isOn = response.body()!!.uvLightOn
                updateUVLightTiming(isOn)
                Result.success(isOn)
            } else {
                _isConnected.value = false
                Result.failure(Exception("Command failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
    }

    /**
     * Update UV light timing based on current state
     */
    private fun updateUVLightTiming(isOn: Boolean) {
        val currentTime = System.currentTimeMillis()

        if (isOn) {
            // If light is on and we don't have a start time, set it
            if (_uvLightStartTime.value == null) {
                _uvLightStartTime.value = currentTime
            }
            // Update duration if we have a start time
            _uvLightStartTime.value?.let { startTime ->
                _uvLightDuration.value = currentTime - startTime
            }
        } else {
            // If light is off, reset timing
            _uvLightStartTime.value = null
            _uvLightDuration.value = 0L
        }
    }

    /**
     * Update the ESP32 IP address and recreate the retrofit instance
     * Note: You might want to make this more sophisticated by using a configuration class
     */
    fun updateESP32IP(newIP: String) {
        // For now, this would require recreating the retrofit instance
        // In a production app, you might want to handle this more elegantly
        // by making the IP configurable through dependency injection
    }

    /**
     * Get current connection status synchronously
     */
    fun getCurrentConnectionStatus(): Boolean {
        return _isConnected.value
    }

    /**
     * Get current UV light duration in milliseconds
     */
    fun getCurrentDuration(): Long {
        return _uvLightDuration.value
    }

    /**
     * Reset the timer (useful when manually turning off)
     */
    fun resetTimer() {
        _uvLightStartTime.value = null
        _uvLightDuration.value = 0L
    }
}