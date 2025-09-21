package com.example.aqualuminus.data.repository

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.example.aqualuminus.data.model.AquariumDevice
import com.example.aqualuminus.data.model.DeviceInfo
import com.example.aqualuminus.data.model.DeviceStatus
import com.example.aqualuminus.data.model.DiscoveredDevice
import com.example.aqualuminus.data.network.UVLightService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

class DeviceConnectionRepository(private val context: Context) {

    // Device discovery
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Multiple devices support
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: Flow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<AquariumDevice>>(emptyList())
    val connectedDevices: Flow<List<AquariumDevice>> = _connectedDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: Flow<Boolean> = _isDiscovering.asStateFlow()

    // Global connection status (for backward compatibility)
    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: Flow<String> = _connectionStatus.asStateFlow()

    // Device services map
    private val deviceServices = ConcurrentHashMap<String, UVLightService>()
    private val deviceRetrofits = ConcurrentHashMap<String, Retrofit>()

    private val discoveredDevicesMap = ConcurrentHashMap<String, DiscoveredDevice>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * Find and connect to all available devices after setup
     */
    suspend fun findAndConnectAfterSetup(): Result<Boolean> {
        return try {
            _connectionStatus.value = "Searching for devices on network..."

            // Clear any old connections
            disconnectAll()

            // mDNS discovery
            val discoveryResult = discoverAndConnectAll()
            if (discoveryResult.isSuccess) {
                return discoveryResult
            }

            Result.failure(Exception("No devices detected."))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start device discovery for multiple devices
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
                if (serviceInfo.serviceType.contains("_http._tcp")) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Continue with other services
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val ip = serviceInfo.host?.hostAddress

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
     * Connect to a specific device by IP
     */
    suspend fun connectToDevice(ip: String): Result<Boolean> {
        return try {
            _connectionStatus.value = "Connecting to $ip..."

            val retrofit = setupRetrofitForIP(ip)
            val service = retrofit.create(UVLightService::class.java)

            // Test connection by getting device info
            val response = service.getDeviceInfo()
            if (response.isSuccessful && response.body() != null) {
                val deviceInfo = response.body()!!

                // Store service for this device
                deviceServices[deviceInfo.device_id] = service
                deviceRetrofits[deviceInfo.device_id] = retrofit

                // Create AquariumDevice
                val aquariumDevice = AquariumDevice(
                    deviceId = deviceInfo.device_id,
                    deviceName = deviceInfo.device_name,
                    ip = deviceInfo.ip,
                    mac = deviceInfo.mac,
                    version = deviceInfo.version,
                    hostname = deviceInfo.hostname,
                    isConnected = true,
                    status = DeviceStatus.ONLINE
                )

                // Add to connected devices
                val currentDevices = _connectedDevices.value.toMutableList()
                currentDevices.removeAll { it.deviceId == deviceInfo.device_id }
                currentDevices.add(aquariumDevice)
                _connectedDevices.value = currentDevices

                // Update global connection status
                _isConnected.value = true
                _connectionStatus.value = "Connected to ${currentDevices.size} device(s)"

                Result.success(true)
            } else {
                Result.failure(Exception("Failed to connect to $ip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect to all discovered devices
     */
    suspend fun connectToAllDevices(): Result<List<String>> {
        val connectedDeviceIds = mutableListOf<String>()
        val devices = _discoveredDevices.value

        for (device in devices) {
            try {
                if (tryConnectToIP(device.ip)) {
                    // Device ID will be extracted during connection
                    val deviceInfo = getDeviceInfoFromIP(device.ip)
                    deviceInfo?.let { connectedDeviceIds.add(it.device_id) }
                }
            } catch (e: Exception) {
                // Continue with next device
            }
        }

        return if (connectedDeviceIds.isNotEmpty()) {
            Result.success(connectedDeviceIds)
        } else {
            Result.failure(Exception("No devices connected"))
        }
    }

    /**
     * Get service for specific device
     */
    fun getServiceForDevice(deviceId: String): UVLightService? {
        return deviceServices[deviceId]
    }

    /**
     * Get device info for specific device
     */
    suspend fun getDeviceInfo(deviceId: String): Result<DeviceInfo> {
        return try {
            val service = deviceServices[deviceId]
                ?: return Result.failure(Exception("Device not connected"))

            val response = service.getDeviceInfo()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get device info: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect from specific device
     */
    fun disconnectDevice(deviceId: String) {
        deviceServices.remove(deviceId)
        deviceRetrofits.remove(deviceId)

        val currentDevices = _connectedDevices.value.toMutableList()
        currentDevices.removeAll { it.deviceId == deviceId }
        _connectedDevices.value = currentDevices

        // Update global status
        _isConnected.value = currentDevices.isNotEmpty()
        _connectionStatus.value = if (currentDevices.isEmpty()) {
            "Disconnected"
        } else {
            "Connected to ${currentDevices.size} device(s)"
        }
    }

    /**
     * Disconnect from all devices
     */
    fun disconnectAll() {
        deviceServices.clear()
        deviceRetrofits.clear()
        _connectedDevices.value = emptyList()
        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
    }

    /**
     * Auto-connect to all discovered devices
     */
    suspend fun autoConnect(): Result<Boolean> {
        return try {
            val result = connectToAllDevices()
            if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("No devices found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Network utility methods
    fun isOnSameNetwork(): Boolean {
        return try {
            wifiManager.connectionInfo?.let { wifiInfo ->
                wifiInfo.networkId != -1
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentWiFiName(): String? {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid?.replace("\"", "")
        } catch (e: Exception) {
            null
        }
    }

    fun cleanup() {
        stopDeviceDiscovery()
        disconnectAll()
    }

    // Private helper methods
    private fun setupRetrofitForIP(ip: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://$ip/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private suspend fun discoverAndConnectAll(): Result<Boolean> {
        return try {
            startDeviceDiscovery()

            // Wait for discovery with timeout
            var attempts = 0
            while (attempts < 10 && _discoveredDevices.value.isEmpty()) {
                kotlinx.coroutines.delay(1000)
                attempts++
            }

            stopDeviceDiscovery()

            val result = connectToAllDevices()
            if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("No devices found via discovery"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryConnectToIP(ip: String): Boolean {
        return try {
            if (ip == "192.168.4.1") return false

            _connectionStatus.value = "Trying $ip..."

            val retrofit = setupRetrofitForIP(ip)
            val service = retrofit.create(UVLightService::class.java)

            val response = service.getDeviceInfo()

            if (response.isSuccessful && response.body() != null) {
                val deviceInfo = response.body()!!

                if (deviceInfo.device.contains("AquaLuminus", ignoreCase = true)) {
                    deviceServices[deviceInfo.device_id] = service
                    deviceRetrofits[deviceInfo.device_id] = retrofit

                    val aquariumDevice = AquariumDevice(
                        deviceId = deviceInfo.device_id,
                        deviceName = deviceInfo.device_name,
                        ip = deviceInfo.ip,
                        mac = deviceInfo.mac,
                        version = deviceInfo.version,
                        hostname = deviceInfo.hostname,
                        isConnected = true,
                        status = DeviceStatus.ONLINE
                    )

                    val currentDevices = _connectedDevices.value.toMutableList()
                    currentDevices.add(aquariumDevice)
                    _connectedDevices.value = currentDevices

                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getDeviceInfoFromIP(ip: String): DeviceInfo? {
        return try {
            val retrofit = setupRetrofitForIP(ip)
            val service = retrofit.create(UVLightService::class.java)
            val response = service.getDeviceInfo()

            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // Backward compatibility methods
    fun getCurrentConnectionStatus(): Boolean = _isConnected.value
    fun getCurrentDeviceIP(): String? = _connectedDevices.value.firstOrNull()?.ip
}