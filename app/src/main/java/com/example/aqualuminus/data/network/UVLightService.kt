// data/network/UVLightService.kt
package com.example.aqualuminus.data.network

import retrofit2.Response
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