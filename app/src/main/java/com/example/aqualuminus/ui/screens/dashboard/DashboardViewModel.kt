package com.example.aqualuminus.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.repository.UVLightRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val uvLightRepository: UVLightRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Firebase user state
    var userName by mutableStateOf("User")
        private set

    var userPhotoUrl by mutableStateOf<String?>(null)
        private set

    // UV Light UI state
    var uvLightOn by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isConnected by mutableStateOf(false)
        private set

    init {
        loadUserData()
        observeConnectionStatus()
        startUVLightMonitoring()
        refreshUVStatus()
    }

    // Firebase user methods
    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            userName = user.displayName
                ?: user.email?.substringBefore("@")
                        ?: "User"
            userPhotoUrl = user.photoUrl?.toString()
        }
    }

    fun refreshUserData() {
        auth.currentUser?.reload()?.addOnCompleteListener {
            loadUserData()
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // UV Light monitoring and control methods
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            uvLightRepository.isConnected.collectLatest { connected ->
                isConnected = connected
            }
        }
    }

    private fun startUVLightMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    if (!isLoading) { // Don't poll during user actions
                        val result = uvLightRepository.getUVLightStatus()
                        result.fold(
                            onSuccess = { status ->
                                uvLightOn = status
                                // Clear error only if there was a connection issue
                                if (error?.contains("Connection") == true) {
                                    error = null
                                }
                            },
                            onFailure = { exception ->
                                // Don't override error if user action is in progress
                                if (!isLoading) {
                                    error = "Connection lost: ${exception.message}"
                                }
                            }
                        )
                    }
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    delay(10000) // Wait longer on error
                }
            }
        }
    }

    fun refreshUVStatus() {
        viewModelScope.launch {
            isLoading = true
            error = null

            val result = uvLightRepository.getUVLightStatus()
            result.fold(
                onSuccess = { status ->
                    uvLightOn = status
                },
                onFailure = { exception ->
                    error = "Failed to refresh status: ${exception.message}"
                }
            )

            isLoading = false
        }
    }

    fun toggleUVLight() {
        viewModelScope.launch {
            executeUVCommand("toggle") {
                uvLightRepository.toggleUVLight()
            }
        }
    }

    fun turnOnUVLight() {
        viewModelScope.launch {
            executeUVCommand("turn on") {
                uvLightRepository.turnOnUVLight()
            }
        }
    }

    fun turnOffUVLight() {
        viewModelScope.launch {
            executeUVCommand("turn off") {
                uvLightRepository.turnOffUVLight()
            }
        }
    }

    private suspend fun executeUVCommand(
        actionName: String,
        command: suspend () -> Result<Boolean>
    ) {
        isLoading = true
        error = null

        val result = command()
        result.fold(
            onSuccess = { newState ->
                uvLightOn = newState
            },
            onFailure = { exception ->
                error = "Failed to $actionName UV light: ${exception.message}"
            }
        )

        isLoading = false
    }

    fun clearError() {
        error = null
    }

    // Method to update ESP32 IP address if needed
    fun updateESP32IP(newIP: String) {
        uvLightRepository.updateESP32IP(newIP)
    }
}

// ViewModelFactory for manual dependency injection
class DashboardViewModelFactory(
    private val uvLightRepository: UVLightRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(uvLightRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}