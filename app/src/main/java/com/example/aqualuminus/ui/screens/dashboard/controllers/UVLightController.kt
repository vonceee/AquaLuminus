package com.example.aqualuminus.ui.screens.dashboard.controllers

import android.util.Log
import com.example.aqualuminus.data.repository.UVLightRepository

class UVLightController(
    private val uvLightRepository: UVLightRepository
) {
    companion object {
        private const val TAG = "UVLightController"
    }

    suspend fun toggleUVLight(): Result<Boolean> {
        return executeCommand("toggle") { uvLightRepository.toggleUVLight() }
    }

    suspend fun turnOnUVLight(): Result<Boolean> {
        return executeCommand("turn on") { uvLightRepository.turnOnUVLight() }
    }

    suspend fun turnOffUVLight(): Result<Boolean> {
        return executeCommand("turn off") { uvLightRepository.turnOffUVLight() }
    }

    suspend fun getUVLightStatus(): Result<Boolean> {
        return uvLightRepository.getUVLightStatus()
    }

    private suspend fun executeCommand(
        actionName: String,
        command: suspend () -> Result<Boolean>
    ): Result<Boolean> {
        return try {
            val result = command()
            result.fold(
                onSuccess = { newState ->
                    Log.d(TAG, "UV light $actionName successful: $newState")
                    Result.success(newState)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to $actionName UV light", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during $actionName command", e)
            Result.failure(e)
        }
    }

    fun formatDuration(duration: Long): String {
        return uvLightRepository.formatDuration(duration)
    }

    fun getCurrentDuration(): Long {
        return uvLightRepository.getCurrentDuration()
    }
}