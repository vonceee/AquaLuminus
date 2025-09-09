package com.example.aqualuminus.ui.screens.dashboard

import android.util.Log
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
    private val _uvLightRepository: UVLightRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Expose the repository for SystemHealthCard
    val uvLightRepository: UVLightRepository get() = _uvLightRepository

    // Firebase User State
    var userName by mutableStateOf("User")
        private set

    var userPhotoUrl by mutableStateOf<String?>(null)
        private set

    // UV Light UI State
    var uvLightOn by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isConnected by mutableStateOf(false)
        private set

    var uvLightDuration by mutableStateOf(0L)
        private set

    // Connection state
    var connectionStatus by mutableStateOf("Disconnected")
        private set

    var isDiscovering by mutableStateOf(false)
        private set

    init {
        loadUserData()
        observeConnectionStatus()
        observeConnectionStatusText()
        observeUVLightDuration()
        observeDiscovering()

        // Try to connect to device first, then start monitoring
        connectToDevice()
    }

    // Firebase User Methods
    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            userName = user.displayName
                ?: user.email
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

    // Connection and monitoring observers
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            _uvLightRepository.isConnected.collectLatest { connected ->
                isConnected = connected
                if (connected) {
                    // Start monitoring only after connection is established
                    startUVLightMonitoring()
                    startDurationUpdater()
                    refreshUVStatus()
                }
            }
        }
    }

    private fun observeConnectionStatusText() {
        viewModelScope.launch {
            _uvLightRepository.connectionStatus.collectLatest { status ->
                connectionStatus = status
            }
        }
    }

    private fun observeUVLightDuration() {
        viewModelScope.launch {
            _uvLightRepository.uvLightDuration.collectLatest { duration ->
                uvLightDuration = duration
            }
        }
    }

    private fun observeDiscovering() {
        viewModelScope.launch {
            _uvLightRepository.isDiscovering.collectLatest { discovering ->
                isDiscovering = discovering
            }
        }
    }

    // Device connection methods
    fun connectToDevice() {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                Log.d("DashboardViewModel", "Starting device connection...")

                // Use the enhanced connection method from repository
                val result = _uvLightRepository.findAndConnectAfterSetup()

                if (result.isSuccess) {
                    Log.d("DashboardViewModel", "Successfully connected to device")
                    // Connection observers will handle starting monitoring
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Connection failed"
                    Log.e("DashboardViewModel", "Connection failed: $errorMsg")
                    error = "Device not found. Make sure device is configured and both are on same WiFi."
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Connection error", e)
                error = "Connection error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun retryConnection() {
        connectToDevice()
    }

    fun forceDiscovery() {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                Log.d("DashboardViewModel", "Starting device discovery...")
                _uvLightRepository.startDeviceDiscovery()

                // Wait for discovery to complete
                delay(8000)

                // Try to connect to discovered devices
                val result = _uvLightRepository.autoConnect()

                if (result.isSuccess) {
                    Log.d("DashboardViewModel", "Discovery and connection successful")
                } else {
                    error = "No devices found. Check WiFi connection and device status."
                }

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Discovery error", e)
                error = "Discovery failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private var monitoringJob: kotlinx.coroutines.Job? = null

    private fun startUVLightMonitoring() {
        // Cancel existing monitoring job
        monitoringJob?.cancel()

        monitoringJob = viewModelScope.launch {
            while (true) {
                try {
                    if (!isLoading && isConnected) { // Only monitor when connected and not during user actions
                        val result = _uvLightRepository.getUVLightStatus()
                        result.fold(
                            onSuccess = { status ->
                                uvLightOn = status
                                // Clear connection errors only
                                if (error?.contains("Connection") == true || error?.contains("not found") == true) {
                                    error = null
                                }
                            },
                            onFailure = { exception ->
                                // Don't override error if user action is in progress
                                if (!isLoading) {
                                    isConnected = false
                                    error = "Connection lost to device"
                                    Log.e("DashboardViewModel", "UV status check failed", exception)
                                }
                            }
                        )
                    }
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Monitoring error", e)
                    delay(10000) // Wait longer on error
                }
            }
        }
    }

    private var durationJob: kotlinx.coroutines.Job? = null

    private fun startDurationUpdater() {
        // Cancel existing duration job
        durationJob?.cancel()

        durationJob = viewModelScope.launch {
            while (true) {
                if (uvLightOn && isConnected) {
                    // Update duration from repository every second when UV light is on
                    uvLightDuration = _uvLightRepository.getCurrentDuration()
                }
                delay(1000)
            }
        }
    }

    fun refreshUVStatus() {
        viewModelScope.launch {
            if (!isConnected) {
                error = "Not connected to device"
                return@launch
            }

            isLoading = true
            error = null

            val result = _uvLightRepository.getUVLightStatus()
            result.fold(
                onSuccess = { status ->
                    uvLightOn = status
                    Log.d("DashboardViewModel", "UV Status refreshed: $status")
                },
                onFailure = { exception ->
                    error = "Failed to get UV status"
                    Log.e("DashboardViewModel", "Failed to refresh UV status", exception)
                }
            )

            isLoading = false
        }
    }

    fun toggleUVLight() {
        if (!isConnected) {
            error = "Not connected to device"
            return
        }

        viewModelScope.launch {
            executeUVCommand("toggle") {
                _uvLightRepository.toggleUVLight()
            }
        }
    }

    fun turnOnUVLight() {
        if (!isConnected) {
            error = "Not connected to device"
            return
        }

        viewModelScope.launch {
            executeUVCommand("turn on") {
                _uvLightRepository.turnOnUVLight()
            }
        }
    }

    fun turnOffUVLight() {
        if (!isConnected) {
            error = "Not connected to device"
            return
        }

        viewModelScope.launch {
            executeUVCommand("turn off") {
                _uvLightRepository.turnOffUVLight()
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
                Log.d("DashboardViewModel", "UV light $actionName successful: $newState")
            },
            onFailure = { exception ->
                error = "Failed to $actionName UV light: ${exception.message}"
                Log.e("DashboardViewModel", "Failed to $actionName UV light", exception)

                // If command fails, might be connection issue
                if (exception.message?.contains("HTTP") == true) {
                    isConnected = false
                }
            }
        )

        isLoading = false
    }

    fun clearError() {
        error = null
    }

    // Disconnect method
    fun disconnect() {
        viewModelScope.launch {
            monitoringJob?.cancel()
            durationJob?.cancel()
            _uvLightRepository.disconnect()

            isConnected = false
            uvLightOn = false
            uvLightDuration = 0L
            connectionStatus = "Disconnected"
        }
    }

    // Get formatted duration string
    fun getFormattedDuration(): String {
        return _uvLightRepository.formatDuration(uvLightDuration)
    }

    // Check if on same WiFi network
    fun isOnSameNetwork(): Boolean {
        return _uvLightRepository.isOnSameNetwork()
    }

    // Get current WiFi name
    fun getCurrentWiFiName(): String? {
        return _uvLightRepository.getCurrentWiFiName()
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
        durationJob?.cancel()
        _uvLightRepository.cleanup()
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