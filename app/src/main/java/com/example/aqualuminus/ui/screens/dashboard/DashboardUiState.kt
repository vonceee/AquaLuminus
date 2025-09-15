package com.example.aqualuminus.ui.screens.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    // Firebase User State
    val userName: String = "User",
    val userPhotoUrl: String? = null,

    // UV Light State
    val uvLightOn: Boolean = false,
    val uvLightDuration: Long = 0L,

    // Connection State
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val isDiscovering: Boolean = false,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardUiStateManager {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun updateState(update: DashboardUiState.() -> DashboardUiState) {
        _uiState.value = _uiState.value.update()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun setError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(error = errorMessage)
    }

    fun updateUserInfo(name: String, photoUrl: String?) {
        _uiState.value = _uiState.value.copy(
            userName = name,
            userPhotoUrl = photoUrl
        )
    }

    fun updateUVLightState(isOn: Boolean, duration: Long) {
        _uiState.value = _uiState.value.copy(
            uvLightOn = isOn,
            uvLightDuration = duration
        )
    }

    fun updateConnectionState(connected: Boolean, status: String, discovering: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            isConnected = connected,
            connectionStatus = status,
            isDiscovering = discovering
        )
    }
}