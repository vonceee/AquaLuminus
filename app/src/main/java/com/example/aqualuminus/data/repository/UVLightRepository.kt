package com.example.aqualuminus.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.example.aqualuminus.data.model.DeviceInfo
import com.example.aqualuminus.data.model.DiscoveredDevice
import com.example.aqualuminus.data.model.UVLightResponse
import com.example.aqualuminus.data.network.UVLightService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

class UVLightRepository(private val context: Context) {

    // Current device connection
    private var currentDeviceIP: String? = null
    private var retrofit: Retrofit? = null
    private var uvLightService: UVLightService? = null

    // Device discovery
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: Flow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: Flow<Boolean> = _isDiscovering.asStateFlow()

    // Connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: Flow<String> = _connectionStatus.asStateFlow()

    // UV Light Timer
    private val _uvLightStartTime = MutableStateFlow<Long?>(null)
    val uvLightStartTime: Flow<Long?> = _uvLightStartTime.asStateFlow()

    private val _uvLightDuration = MutableStateFlow(0L)
    val uvLightDuration: Flow<Long> = _uvLightDuration.asStateFlow()

    // Device info
    private val _currentDevice = MutableStateFlow<DeviceInfo?>(null)
    val currentDevice: Flow<DeviceInfo?> = _currentDevice.asStateFlow()

    private val discoveredDevicesMap = ConcurrentHashMap<String, DiscoveredDevice>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * Stop device discovery
     */
    fun stopDeviceDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                // Ignore
            }
        }
        _isDiscovering.value = false
    }

    /**
     * Connect to a specific device
     */
    suspend fun connectToDevice(ip: String): Result<Boolean> {
        return try {
            _connectionStatus.value = "Connecting to $ip..."

            setupRetrofitForIP(ip)

            // Test connection by getting device info
            val deviceInfoResult = getDeviceInfo()
            if (deviceInfoResult.isSuccess) {
                currentDeviceIP = ip
                _isConnected.value = true
                _connectionStatus.value = "Connected to ${deviceInfoResult.getOrNull()?.device ?: "AquaLuminus"}"

                // Also get current UV light status
                getUVLightStatus()

                Result.success(true)
            } else {
                _isConnected.value = false
                _connectionStatus.value = "Failed to connect to $ip"
                Result.failure(deviceInfoResult.exceptionOrNull() ?: Exception("Connection failed"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            _connectionStatus.value = "Connection error: ${e.message}"
            Result.failure(e)
        }
    }

    /**
     * Auto-connect to the first discovered device
     */
    suspend fun autoConnect(): Result<Boolean> {
        val devices = _discoveredDevices.value
        return if (devices.isNotEmpty()) {
            connectToDevice(devices.first().ip)
        } else {
            Result.failure(Exception("No devices found"))
        }
    }

    private fun setupRetrofitForIP(ip: String) {
        retrofit = Retrofit.Builder()
            .baseUrl("http://$ip/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        uvLightService = retrofit?.create(UVLightService::class.java)
    }

    /**
     * Get device information
     */
    suspend fun getDeviceInfo(): Result<DeviceInfo> {
        return try {
            val response = uvLightService?.getDeviceInfo()
            if (response?.isSuccessful == true && response.body() != null) {
                val deviceInfo = response.body()!!
                _currentDevice.value = deviceInfo
                Result.success(deviceInfo)
            } else {
                Result.failure(Exception("Failed to get device info: HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the current UV light status
     * @return Result<Boolean> - true if UV light is on, false if off
     */
    suspend fun getUVLightStatus(): Result<Boolean> {
        return try {
            val response = uvLightService?.getStatus()
            if (response?.isSuccessful == true && response.body() != null) {
                _isConnected.value = true
                val isOn = response.body()!!.uvLightOn
                updateUVLightTiming(isOn)
                Result.success(isOn)
            } else {
                _isConnected.value = false
                _connectionStatus.value = "Device not responding"
                Result.failure(Exception("Failed to get status: HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            _connectionStatus.value = "Connection lost"
            Result.failure(e)
        }
    }

    /**
     * Turn on the UV light
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun turnOnUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService?.turnOn() }
    }

    /**
     * Turn off the UV light
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun turnOffUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService?.turnOff() }
    }

    /**
     * Toggle the UV light state
     * @return Result<Boolean> - the new state of the UV light
     */
    suspend fun toggleUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService?.toggle() }
    }

    /**
     * Private helper method to execute UV light commands
     */
    private suspend fun executeUVLightCommand(
        command: suspend () -> Response<UVLightResponse>?
    ): Result<Boolean> {
        return try {
            if (uvLightService == null) {
                return Result.failure(Exception("Not connected to device"))
            }

            val response = command()
            if (response?.isSuccessful == true && response.body() != null) {
                _isConnected.value = true
                val isOn = response.body()!!.uvLightOn
                updateUVLightTiming(isOn)
                Result.success(isOn)
            } else {
                _isConnected.value = false
                _connectionStatus.value = "Command failed"
                Result.failure(Exception("Command failed: HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            _connectionStatus.value = "Connection lost"
            Result.failure(e)
        }
    }

    /**
     * Update UV light timing based on current state
     */
    private fun updateUVLightTiming(isOn: Boolean) {
        val currentTime = System.currentTimeMillis()

        if (isOn) {
            // If light is on and we don't have a start time, set it
            if (_uvLightStartTime.value == null) {
                _uvLightStartTime.value = currentTime
            }
            // Update duration if we have a start time
            _uvLightStartTime.value?.let { startTime ->
                _uvLightDuration.value = currentTime - startTime
            }
        } else {
            // If light is off, reset timing
            _uvLightStartTime.value = null
            _uvLightDuration.value = 0L
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        currentDeviceIP = null
        retrofit = null
        uvLightService = null
        _isConnected.value = false
        _currentDevice.value = null
        _connectionStatus.value = "Disconnected"
        resetTimer()
    }

    /**
     * Get current connection status synchronously
     */
    fun getCurrentConnectionStatus(): Boolean {
        return _isConnected.value
    }

    /**
     * Get current device IP
     */
    fun getCurrentDeviceIP(): String? {
        return currentDeviceIP
    }

    /**
     * Get current UV light duration in milliseconds
     */
    fun getCurrentDuration(): Long {
        return _uvLightDuration.value
    }

    /**
     * Reset the timer (useful when manually turning off)
     */
    fun resetTimer() {
        _uvLightStartTime.value = null
        _uvLightDuration.value = 0L
    }

    /**
     * Format duration for display
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            else -> String.format("00:%02d", seconds)
        }
    }

    /**
     * Check if device is on same WiFi network
     */
    fun isOnSameNetwork(): Boolean {
        return try {
            wifiManager.connectionInfo?.let { wifiInfo ->
                wifiInfo.networkId != -1
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get WiFi network name
     */
    fun getCurrentWiFiName(): String? {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid?.replace("\"", "")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopDeviceDiscovery()
        disconnect()
    }

    suspend fun findAndConnectAfterSetup(): Result<Boolean> {
        return try {
            _connectionStatus.value = "Searching for device on network..."

            // Clear any old setup mode connections
            disconnect()

            // mDNS discovery
            val discoveryResult = discoverAndConnect()
            if (discoveryResult.isSuccess) {
                return discoveryResult
            }

            Result.failure(Exception("No Device Detected."))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Try mDNS discovery to find AquaLuminus device
     */
    private suspend fun discoverAndConnect(): Result<Boolean> {
        return try {
            startDeviceDiscovery()

            // Wait for discovery with timeout
            var attempts = 0
            while (attempts < 10 && _discoveredDevices.value.isEmpty()) {
                kotlinx.coroutines.delay(1000)
                attempts++
            }

            stopDeviceDiscovery()

            val devices = _discoveredDevices.value
            if (devices.isNotEmpty()) {
                // Try each discovered device
                for (device in devices) {
                    if (tryConnectToIP(device.ip)) {
                        return Result.success(true)
                    }
                }
            }

            Result.failure(Exception("No devices found via discovery"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Try connecting to specific IP and verify it's AquaLuminus device
     */
    private suspend fun tryConnectToIP(ip: String): Boolean {
        return try {
            // Skip setup mode IP
            if (ip == "192.168.4.1") {
                return false
            }

            _connectionStatus.value = "Trying $ip..."

            setupRetrofitForIP(ip)

            // Quick test - try to get device info
            val response = uvLightService?.getDeviceInfo()

            if (response?.isSuccessful == true) {
                val deviceInfo = response.body()

                // Verify it's actually an AquaLuminus device
                if (deviceInfo?.device?.contains("AquaLuminus", ignoreCase = true) == true) {
                    currentDeviceIP = ip
                    _isConnected.value = true
                    _currentDevice.value = deviceInfo
                    _connectionStatus.value = "Connected to ${deviceInfo.device} at $ip"

                    // Get initial UV status
                    getUVLightStatus()

                    return true
                }
            }

            false

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Enhanced device discovery that excludes setup mode IP
     */
    fun startDeviceDiscovery() {
        if (_isDiscovering.value) return

        _isDiscovering.value = true
        _connectionStatus.value = "Discovering devices..."
        discoveredDevicesMap.clear()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
                _connectionStatus.value = "Discovery failed"
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                _connectionStatus.value = "Scanning network..."
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
                _connectionStatus.value = if (discoveredDevicesMap.isEmpty()) {
                    "No devices found"
                } else {
                    "${discoveredDevicesMap.size} device(s) found"
                }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Look for HTTP services (our ESP32 advertises _http._tcp)
                if (serviceInfo.serviceType.contains("_http._tcp")) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Continue with other services
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val ip = serviceInfo.host?.hostAddress

                            // Only add non-setup mode IPs
                            if (ip != null && ip != "192.168.4.1") {
                                val device = DiscoveredDevice(
                                    name = serviceInfo.serviceName,
                                    ip = ip,
                                    port = serviceInfo.port,
                                    hostname = "${serviceInfo.serviceName}.local"
                                )

                                discoveredDevicesMap[device.ip] = device
                                _discoveredDevices.value = discoveredDevicesMap.values.toList()
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                serviceInfo.host?.hostAddress?.let { ip ->
                    discoveredDevicesMap.remove(ip)
                    _discoveredDevices.value = discoveredDevicesMap.values.toList()
                }
            }
        }

        try {
            nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            _isDiscovering.value = false
            _connectionStatus.value = "Discovery error: ${e.message}"
        }
    }
}