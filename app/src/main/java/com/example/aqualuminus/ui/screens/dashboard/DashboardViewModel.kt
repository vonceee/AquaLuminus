package com.example.aqualuminus.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.manager.BackgroundTaskManager
import com.example.aqualuminus.data.manager.DeviceConnectionManager
import com.example.aqualuminus.data.repository.UVLightRepository
import com.example.aqualuminus.ui.screens.dashboard.controllers.UVLightController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val _uvLightRepository: UVLightRepository
) : ViewModel() {

    // MARK: - UI State Management
    private val uiStateManager = DashboardUiStateManager()
    val uiState: StateFlow<DashboardUiState> = uiStateManager.uiState

    // MARK: - Managers
    private val userManager = UserManager()
    private val connectionManager = DeviceConnectionManager(_uvLightRepository)
    private val uvLightController = UVLightController(_uvLightRepository)
    private val taskManager = BackgroundTaskManager(viewModelScope)

    // MARK: - Expose Repository (for SystemHealthCard)
    val uvLightRepository: UVLightRepository get() = _uvLightRepository

    // MARK: - Initialization

    init {
        loadUserData()
        setupObservers()
        connectToDevice()
    }

    // MARK: - Public API Methods

    fun toggleUVLight() {
        executeUVCommand("toggle") {
            uvLightController.toggleUVLight()
        }
    }

    fun turnOnUVLight() {
        executeUVCommand("turn on") {
            uvLightController.turnOnUVLight()
        }
    }

    fun turnOffUVLight() {
        executeUVCommand("turn off") {
            uvLightController.turnOffUVLight()
        }
    }

    fun refreshUVStatus() {
        if (!uiState.value.isConnected) {
            uiStateManager.setError("Not connected to device")
            return
        }

        executeWithLoading("refreshing UV status") {
            val result = uvLightController.getUVLightStatus()
            result.fold(
                onSuccess = { status ->
                    uiStateManager.updateUVLightState(status, uiState.value.uvLightDuration)
                },
                onFailure = { exception ->
                    uiStateManager.setError("Failed to get UV status")
                }
            )
        }
    }

    fun clearError() {
        uiStateManager.clearError()
    }

    fun getFormattedDuration(): String {
        return uvLightController.formatDuration(uiState.value.uvLightDuration)
    }

    // MARK: - User Management

    fun refreshUserData() {
        userManager.refreshUserData {
            loadUserData()
        }
    }

    fun getCurrentUser(): FirebaseUser? = userManager.getCurrentUser()

    private fun loadUserData() {
        val userName = userManager.getUserDisplayName()
        val userPhotoUrl = userManager.getUserPhotoUrl()
        uiStateManager.updateUserInfo(userName, userPhotoUrl)
    }

    // MARK: - Connection Management

    fun connectToDevice() {
        executeWithLoading("connecting to device") {
            val result = connectionManager.connectToDevice()
            result.fold(
                onSuccess = {
                    // connection successful - observers will handle state updates
                },
                onFailure = { exception ->
                    uiStateManager.setError(exception.message ?: "Connection failed")
                }
            )
        }
    }

    fun retryConnection() {
        connectToDevice()
    }

    fun forceDiscovery() {
        executeWithLoading("discovering devices") {
            val result = connectionManager.forceDiscovery()
            result.fold(
                onSuccess = {
                    // Discovery successful
                },
                onFailure = { exception ->
                    uiStateManager.setError(exception.message ?: "Discovery failed")
                }
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            taskManager.stopAllTasks()
            connectionManager.disconnect()
            resetConnectionState()
        }
    }

    fun isOnSameNetwork(): Boolean = connectionManager.isOnSameNetwork()
    fun getCurrentWiFiName(): String? = connectionManager.getCurrentWiFiName()

    // MARK: - Private Helper Methods

    private fun executeUVCommand(
        actionName: String,
        command: suspend () -> Result<Boolean>
    ) {
        if (!uiState.value.isConnected) {
            uiStateManager.setError("No Device Detected")
            return
        }

        executeWithLoading("UV light $actionName") {
            val result = command()
            result.fold(
                onSuccess = { newState ->
                    uiStateManager.updateUVLightState(newState, uiState.value.uvLightDuration)
                },
                onFailure = { exception ->
                    handleCommandFailure(actionName, exception)
                }
            )
        }
    }

    private fun handleCommandFailure(actionName: String, exception: Throwable) {
        uiStateManager.setError("Failed to $actionName UV light: ${exception.message}")

        // If HTTP error, might be connection issue
        if (exception.message?.contains("HTTP") == true) {
            uiStateManager.updateConnectionState(false, "Connection Lost")
        }
    }

    private fun executeWithLoading(
        operation: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                uiStateManager.setLoading(true)
                uiStateManager.clearError()
                action()
            } catch (e: Exception) {
                uiStateManager.setError("Error during $operation: ${e.message}")
            } finally {
                uiStateManager.setLoading(false)
            }
        }
    }

    // MARK: - Observers and Background Tasks

    private fun setupObservers() {
        observeConnectionStatus()
        observeConnectionStatusText()
        observeUVLightDuration()
        observeDiscovering()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            _uvLightRepository.isConnected.collectLatest { connected ->
                uiStateManager.updateConnectionState(
                    connected = connected,
                    status = if (connected) "Connected" else "Disconnected"
                )
                if (connected) {
                    startBackgroundTasks()
                    refreshUVStatus()
                }
            }
        }
    }

    private fun observeConnectionStatusText() {
        viewModelScope.launch {
            _uvLightRepository.connectionStatus.collectLatest { status ->
                uiStateManager.updateConnectionState(
                    connected = uiState.value.isConnected,
                    status = status,
                    discovering = uiState.value.isDiscovering
                )
            }
        }
    }

    private fun observeUVLightDuration() {
        viewModelScope.launch {
            _uvLightRepository.uvLightDuration.collectLatest { duration ->
                uiStateManager.updateUVLightState(uiState.value.uvLightOn, duration)
            }
        }
    }

    private fun observeDiscovering() {
        viewModelScope.launch {
            _uvLightRepository.isDiscovering.collectLatest { discovering ->
                uiStateManager.updateConnectionState(
                    connected = uiState.value.isConnected,
                    status = uiState.value.connectionStatus,
                    discovering = discovering
                )
            }
        }
    }

    private fun startBackgroundTasks() {
        // Delegate task management to BackgroundTaskManager
        taskManager.startStatusMonitoring(
            shouldMonitor = { !uiState.value.isLoading && uiState.value.isConnected },
            onStatusCheck = { checkUVLightStatus() }
        )

        taskManager.startDurationTracking(
            shouldTrack = { uiState.value.uvLightOn && uiState.value.isConnected },
            onUpdateDuration = { updateDuration() }
        )
    }

    private suspend fun checkUVLightStatus() {
        val result = uvLightController.getUVLightStatus()
        result.fold(
            onSuccess = { status ->
                uiStateManager.updateUVLightState(status, uiState.value.uvLightDuration)
                // clear connection errors only
                if (isConnectionError()) {
                    uiStateManager.clearError()
                }
            },
            onFailure = { exception ->
                if (!uiState.value.isLoading) {
                    uiStateManager.updateConnectionState(false, "Connection Lost")
                    uiStateManager.setError("Connection lost to device")
                }
            }
        )
    }

    private fun updateDuration() {
        val currentDuration = uvLightController.getCurrentDuration()
        uiStateManager.updateUVLightState(uiState.value.uvLightOn, currentDuration)
    }

    private fun isConnectionError(): Boolean {
        return uiState.value.error?.contains("Connection") == true ||
                uiState.value.error?.contains("not found") == true
    }

    private fun resetConnectionState() {
        uiStateManager.updateConnectionState(false, "Disconnected")
        uiStateManager.updateUVLightState(false, 0L)
    }

    // MARK: - Cleanup

    override fun onCleared() {
        super.onCleared()
        taskManager.stopAllTasks()
        _uvLightRepository.cleanup()
    }
}

// MARK: - Manager Classes

private class UserManager {
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserDisplayName(): String {
        return auth.currentUser?.let { user ->
            user.displayName ?: user.email ?: "User"
        } ?: "User"
    }

    fun getUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    fun refreshUserData(onComplete: () -> Unit) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            onComplete()
        }
    }
}

private class DeviceConnectionManager(
    private val uvLightRepository: UVLightRepository
) {
    companion object {
        private const val DISCOVERY_TIMEOUT = 8000L
    }

    suspend fun connectToDevice(): Result<Unit> {
        return try {
            val result = uvLightRepository.findAndConnectAfterSetup()

            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No Device Detected."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forceDiscovery(): Result<Unit> {
        return try {
            uvLightRepository.startDeviceDiscovery()
            delay(DISCOVERY_TIMEOUT)

            val result = uvLightRepository.autoConnect()
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No Device Detected."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        uvLightRepository.disconnect()
    }

    fun isOnSameNetwork(): Boolean = uvLightRepository.isOnSameNetwork()
    fun getCurrentWiFiName(): String? = uvLightRepository.getCurrentWiFiName()
}

private class UVLightController(
    private val uvLightRepository: UVLightRepository
) {
    suspend fun toggleUVLight(): Result<Boolean> {
        return uvLightRepository.toggleUVLight()
    }

    suspend fun turnOnUVLight(): Result<Boolean> {
        return uvLightRepository.turnOnUVLight()
    }

    suspend fun turnOffUVLight(): Result<Boolean> {
        return uvLightRepository.turnOffUVLight()
    }

    suspend fun getUVLightStatus(): Result<Boolean> {
        return uvLightRepository.getUVLightStatus()
    }

    fun formatDuration(duration: Long): String {
        return uvLightRepository.formatDuration(duration)
    }

    fun getCurrentDuration(): Long {
        return uvLightRepository.getCurrentDuration()
    }
}

private class BackgroundTaskManager(
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    companion object {
        private const val STATUS_CHECK_INTERVAL = 5000L
        private const val DURATION_UPDATE_INTERVAL = 1000L
        private const val ERROR_RETRY_INTERVAL = 10000L
    }

    private var monitoringJob: Job? = null
    private var durationJob: Job? = null

    fun startStatusMonitoring(
        shouldMonitor: () -> Boolean,
        onStatusCheck: suspend () -> Unit
    ) {
        stopStatusMonitoring()
        monitoringJob = coroutineScope.launch {
            while (true) {
                try {
                    if (shouldMonitor()) {
                        onStatusCheck()
                    }
                    delay(STATUS_CHECK_INTERVAL)
                } catch (e: Exception) {
                    delay(ERROR_RETRY_INTERVAL)
                }
            }
        }
    }

    fun startDurationTracking(
        shouldTrack: () -> Boolean,
        onUpdateDuration: () -> Unit
    ) {
        stopDurationTracking()
        durationJob = coroutineScope.launch {
            while (true) {
                if (shouldTrack()) {
                    onUpdateDuration()
                }
                delay(DURATION_UPDATE_INTERVAL)
            }
        }
    }

    fun stopAllTasks() {
        stopStatusMonitoring()
        stopDurationTracking()
    }

    private fun stopStatusMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun stopDurationTracking() {
        durationJob?.cancel()
        durationJob = null
    }
}

// MARK: - ViewModelFactory

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