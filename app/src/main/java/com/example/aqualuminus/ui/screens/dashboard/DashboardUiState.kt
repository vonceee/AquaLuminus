package com.example.aqualuminus.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

class DashboardUiStateHolder {
    var uiState by mutableStateOf(DashboardUiState())
        private set

    fun updateUiState(update: DashboardUiState.() -> DashboardUiState) {
        uiState = uiState.update()
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun setLoading(loading: Boolean) {
        uiState = uiState.copy(isLoading = loading)
    }

    fun setError(errorMessage: String) {
        uiState = uiState.copy(error = errorMessage)
    }
}