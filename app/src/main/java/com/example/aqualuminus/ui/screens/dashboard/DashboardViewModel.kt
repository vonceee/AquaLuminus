package com.example.aqualuminus.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val _uvLightRepository: UVLightRepository
) : ViewModel() {

    // MARK: - Managers
    private val userManager = UserManager()
    private val connectionManager = DeviceConnectionManager(_uvLightRepository)
    private val uvLightController = UVLightController(_uvLightRepository)
    private val taskManager = BackgroundTaskManager(viewModelScope)

    // MARK: - Expose Repository (for SystemHealthCard)
    val uvLightRepository: UVLightRepository get() = _uvLightRepository

    // MARK: - UI State Properties

    // User State
    var userName by mutableStateOf("User")
        private set
    var userPhotoUrl by mutableStateOf<String?>(null)
        private set

    // UV Light State
    var uvLightOn by mutableStateOf(false)
        private set
    var uvLightDuration by mutableStateOf(0L)
        private set

    // Connection State
    var isConnected by mutableStateOf(false)
        private set
    var connectionStatus by mutableStateOf("Disconnected")
        private set
    var isDiscovering by mutableStateOf(false)
        private set

    // UI State
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // MARK: - Initialization

    init {
        loadUserData()
        setupObservers()
        connectToDevice()
    }

    // MARK: - User Management Delegation

    fun refreshUserData() {
        userManager.refreshUserData {
            loadUserData()
        }
    }

    fun getCurrentUser(): FirebaseUser? = userManager.getCurrentUser()

    private fun loadUserData() {
        userName = userManager.getUserDisplayName()
        userPhotoUrl = userManager.getUserPhotoUrl()
    }

    // MARK: - Connection Management Delegation

    fun connectToDevice() {
        executeWithLoading("connecting to device") {
            val result = connectionManager.connectToDevice()
            result.fold(
                onSuccess = {
                    // connection successful - observers will handle state updates
                },
                onFailure = { exception ->
                    error = exception.message ?: "Connection failed"
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
                    error = exception.message ?: "Discovery failed"
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

    // MARK: - UV Light Control Delegation

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
        if (!isConnected) {
            error = "Not connected to device"
            return
        }

        executeWithLoading("refreshing UV status") {
            val result = uvLightController.getUVLightStatus()
            result.fold(
                onSuccess = { status ->
                    uvLightOn = status
                },
                onFailure = { exception ->
                    error = "Failed to get UV status"
                }
            )
        }
    }

    fun getFormattedDuration(): String {
        return uvLightController.formatDuration(uvLightDuration)
    }

    // MARK: - UI State Management

    fun clearError() {
        error = null
    }

    private fun executeUVCommand(
        actionName: String,
        command: suspend () -> Result<Boolean>
    ) {
        if (!isConnected) {
            error = "No Device Detected"
            return
        }

        executeWithLoading("UV light $actionName") {
            val result = command()
            result.fold(
                onSuccess = { newState ->
                    uvLightOn = newState
                },
                onFailure = { exception ->
                    handleCommandFailure(actionName, exception)
                }
            )
        }
    }

    private fun handleCommandFailure(actionName: String, exception: Throwable) {
        error = "Failed to $actionName UV light: ${exception.message}"

        // If HTTP error, might be connection issue
        if (exception.message?.contains("HTTP") == true) {
            isConnected = false
        }
    }

    private fun executeWithLoading(
        operation: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                action()
            } catch (e: Exception) {
                error = "Error during $operation: ${e.message}"
            } finally {
                isLoading = false
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
                isConnected = connected
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

    private fun startBackgroundTasks() {
        // Delegate task management to BackgroundTaskManager
        taskManager.startStatusMonitoring(
            shouldMonitor = { !isLoading && isConnected },
            onStatusCheck = { checkUVLightStatus() }
        )

        taskManager.startDurationTracking(
            shouldTrack = { uvLightOn && isConnected },
            onUpdateDuration = { updateDuration() }
        )
    }

    private suspend fun checkUVLightStatus() {
        val result = uvLightController.getUVLightStatus()
        result.fold(
            onSuccess = { status ->
                uvLightOn = status
                // clear connection errors only
                if (isConnectionError()) {
                    error = null
                }
            },
            onFailure = { exception ->
                if (!isLoading) {
                    isConnected = false
                    error = "Connection lost to device"
                }
            }
        )
    }

    private fun updateDuration() {
        uvLightDuration = uvLightController.getCurrentDuration()
    }

    private fun isConnectionError(): Boolean {
        return error?.contains("Connection") == true ||
                error?.contains("not found") == true
    }

    private fun resetConnectionState() {
        isConnected = false
        uvLightOn = false
        uvLightDuration = 0L
        connectionStatus = "Disconnected"
    }

    // MARK: - Cleanup

    override fun onCleared() {
        super.onCleared()
        taskManager.stopAllTasks()
        _uvLightRepository.cleanup()
    }
}

// MARK: - Manager Classes (Inner classes or separate files)

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