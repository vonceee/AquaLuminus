package com.example.aqualuminus.data.manager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.example.aqualuminus.data.model.DiscoveredDevice
import com.example.aqualuminus.data.network.UVLightService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class DeviceDiscoveryManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val discoveredDevicesMap = ConcurrentHashMap<String, DiscoveredDevice>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: Flow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: Flow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveryStatus = MutableStateFlow("Ready to scan")
    val discoveryStatus: Flow<String> = _discoveryStatus.asStateFlow()

    /**
     * Start mDNS service discovery for AquaLuminus devices
     */
    fun startDiscovery() {
        if (_isDiscovering.value) return

        _isDiscovering.value = true
        _discoveryStatus.value = "Discovering devices..."
        discoveredDevicesMap.clear()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
                _discoveryStatus.value = "Discovery failed"
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                _discoveryStatus.value = "Scanning network..."
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
                _discoveryStatus.value = if (discoveredDevicesMap.isEmpty()) {
                    "No devices found"
                } else {
                    "${discoveredDevicesMap.size} device(s) found"
                }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Look for HTTP services (ESP32 advertises _http._tcp)
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
            _discoveryStatus.value = "Discovery error: ${e.message}"
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                // Ignore
            }
        }
        _isDiscovering.value = false
        _discoveryStatus.value = if (discoveredDevicesMap.isEmpty()) {
            "No devices found"
        } else {
            "${discoveredDevicesMap.size} device(s) found"
        }
    }

    /**
     * Scan local network for AquaLuminus devices by checking IP range
     */
    suspend fun scanLocalNetwork(): List<DiscoveredDevice> {
        val devices = mutableListOf<DiscoveredDevice>()

        try {
            // Get current WiFi network info
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            if (ipAddress == 0) {
                _discoveryStatus.value = "Not connected to WiFi"
                return devices
            }

            // Convert IP to subnet (e.g., 192.168.1.x)
            val subnet = String.format(
                "%d.%d.%d.",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff
            )

            _discoveryStatus.value = "Scanning $subnet..."

            // Scan subnet for devices - check common IoT device ranges
            val ipRanges = listOf(
                (100..120),  // Common IoT device range
                (2..50),     // Lower range
                (200..254)   // Higher range
            )

            for (range in ipRanges) {
                for (i in range) {
                    try {
                        val host = "$subnet$i"

                        // Skip setup mode IP
                        if (host == "192.168.4.1") continue

                        val addr = InetAddress.getByName(host)

                        if (addr.isReachable(500)) { // Reduced timeout for faster scanning
                            // Try to connect and check if it's an AquaLuminus device
                            if (isAquaLuminusDevice(host)) {
                                val device = DiscoveredDevice(
                                    name = "AquaLuminus",
                                    ip = host,
                                    port = 80,
                                    hostname = host
                                )
                                devices.add(device)
                                discoveredDevicesMap[host] = device
                            }
                        }
                    } catch (e: Exception) {
                        // Continue scanning
                    }

                    delay(50)
                }
            }

            _discoveredDevices.value = devices
            _discoveryStatus.value = "${devices.size} device(s) found"

        } catch (e: Exception) {
            _discoveryStatus.value = "Network scan failed: ${e.message}"
        }

        return devices
    }

    /**
     * Try common IP addresses for AquaLuminus devices
     */
    suspend fun tryCommonIPs(): List<DiscoveredDevice> {
        val devices = mutableListOf<DiscoveredDevice>()

        val commonIPs = listOf(
            "192.168.1.100", "192.168.1.101", "192.168.1.102",
            "192.168.0.100", "192.168.0.101", "192.168.0.102",
            "10.0.0.100", "10.0.0.101", "10.0.0.102"
        )

        _discoveryStatus.value = "Trying common IP addresses..."

        for (ip in commonIPs) {
            try {
                if (isAquaLuminusDevice(ip)) {
                    val device = DiscoveredDevice(
                        name = "AquaLuminus",
                        ip = ip,
                        port = 80,
                        hostname = ip
                    )
                    devices.add(device)
                    discoveredDevicesMap[ip] = device
                }
            } catch (e: Exception) {
                // Continue trying other IPs
            }

            // Small delay
            delay(100)
        }

        _discoveredDevices.value = discoveredDevicesMap.values.toList()
        _discoveryStatus.value = "${devices.size} device(s) found"

        return devices
    }

    /**
     * Comprehensive device discovery that tries multiple methods
     */
    suspend fun discoverDevices(): Result<List<DiscoveredDevice>> {
        return try {
            _discoveryStatus.value = "Searching for devices..."
            discoveredDevicesMap.clear()

            // Method 1: Try mDNS discovery first (most reliable)
            startDiscovery()
            var attempts = 0
            while (attempts < 10 && _discoveredDevices.value.isEmpty() && _isDiscovering.value) {
                delay(1000)
                attempts++
            }
            stopDiscovery()

            if (_discoveredDevices.value.isNotEmpty()) {
                return Result.success(_discoveredDevices.value)
            }

            // Method 2: If mDNS fails, scan local network
            _discoveryStatus.value = "Scanning network range..."
            val scanResults = scanLocalNetwork()
            if (scanResults.isNotEmpty()) {
                return Result.success(scanResults)
            }

            // Method 3: Last resort - try common IPs
            val commonResults = tryCommonIPs()
            if (commonResults.isNotEmpty()) {
                return Result.success(commonResults)
            }

            _discoveryStatus.value = "No devices found"
            Result.success(emptyList())

        } catch (e: Exception) {
            _discoveryStatus.value = "Discovery error: ${e.message}"
            Result.failure(e)
        }
    }

    /**
     * Check if a device at the given IP is an AquaLuminus device
     */
    private suspend fun isAquaLuminusDevice(ip: String): Boolean {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://$ip/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(UVLightService::class.java)
            val response = service.getDeviceInfo()

            response.isSuccessful &&
                    response.body()?.device?.contains("AquaLuminus", ignoreCase = true) == true
        } catch (e: Exception) {
            false
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
     * Get current WiFi subnet for display purposes
     */
    fun getCurrentSubnet(): String? {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            if (ipAddress != 0) {
                String.format(
                    "%d.%d.%d.x",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear discovered devices list
     */
    fun clearDevices() {
        discoveredDevicesMap.clear()
        _discoveredDevices.value = emptyList()
        _discoveryStatus.value = "Ready to scan"
    }

    /**
     * Get current discovered devices count
     */
    fun getDeviceCount(): Int = discoveredDevicesMap.size

    /**
     * Check if discovery is currently running
     */
    fun isCurrentlyDiscovering(): Boolean = _isDiscovering.value

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopDiscovery()
        clearDevices()
    }
}