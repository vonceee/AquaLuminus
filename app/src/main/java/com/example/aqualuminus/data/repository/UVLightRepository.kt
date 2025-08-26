package com.example.aqualuminus.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST

data class UVLightResponse(
    val success: Boolean = true,
    val uvLightOn: Boolean,
    val status: String? = null,
    val message: String? = null,
    val timestamp: Long,
    val device: String? = null
)

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

    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    /**
     * Get the current UV light status
     * @return Result<Boolean> - true if UV light is on, false if off
     */
    suspend fun getUVLightStatus(): Result<Boolean> {
        return try {
            val response = uvLightService.getStatus()
            if (response.isSuccessful && response.body() != null) {
                _isConnected.value = true
                Result.success(response.body()!!.uvLightOn)
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
                Result.success(response.body()!!.uvLightOn)
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
}