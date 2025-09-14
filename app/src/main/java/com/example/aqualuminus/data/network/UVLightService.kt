package com.example.aqualuminus.data.network

import com.example.aqualuminus.data.model.DeviceInfo
import com.example.aqualuminus.data.model.UVLightResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface UVLightService {
    @GET("api/status")
    suspend fun getStatus(): Response<UVLightResponse>

    @POST("api/on")
    suspend fun turnOn(): Response<UVLightResponse>

    @POST("api/off")
    suspend fun turnOff(): Response<UVLightResponse>

    @POST("api/toggle")
    suspend fun toggle(): Response<UVLightResponse>

    @GET("api/info")
    suspend fun getDeviceInfo(): Response<DeviceInfo>
}