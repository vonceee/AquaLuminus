package com.example.aqualuminus.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DeviceInfo(
    val id: String,
    val name: String,
    val ipAddress: String,
    val isConnected: Boolean,
    val uvLightOn: Boolean,
    val lastSeen: String? = null
)

@Composable
fun DeviceSelectionCard(
    devices: List<DeviceInfo> = getSampleDevices(),
    selectedDevices: Set<String> = emptySet(),
    onDeviceSelectionChange: (Set<String>) -> Unit = {},
    onBulkToggle: () -> Unit = {},
    onAddDevice: () -> Unit = {},
    onRemoveDevice: (String) -> Unit = {},
    onRemoveSelectedDevices: (Set<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var localSelectedDevices by remember(selectedDevices) {
        mutableStateOf(selectedDevices)
    }
    var isSelectionMode by remember { mutableStateOf(false) }

    // calculate if bulk action is available
    val connectedDevices = devices.filter { it.isConnected }
    val selectedConnectedDevices = connectedDevices.filter { it.id in localSelectedDevices }
    val canBulkToggle = selectedConnectedDevices.isNotEmpty() &&
            selectedConnectedDevices.all { it.uvLightOn == selectedConnectedDevices.first().uvLightOn }

    val exitSelectionMode = {
        isSelectionMode = false
        localSelectedDevices = emptySet()
        onDeviceSelectionChange(emptySet())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Devices,
                        contentDescription = "Devices",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isSelectionMode) "Select Devices" else "Device Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Device Count Badge or Cancel Button
                if (isSelectionMode) {
                    IconButton(
                        onClick = exitSelectionMode,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel Selection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "${connectedDevices.size}/${devices.size} Online",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (connectedDevices.size == devices.size)
                                Color(0xFF22C55E).copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (connectedDevices.size == devices.size)
                                Color(0xFF22C55E)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (connectedDevices.size == devices.size)
                                Color(0xFF22C55E).copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selection Mode Controls
            if (isSelectionMode && localSelectedDevices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${localSelectedDevices.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            onRemoveSelectedDevices(localSelectedDevices)
                            exitSelectionMode()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove Selected",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remove All Selected",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Device List with Add Device Card
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = devices,
                    key = { it.id }
                ) { device ->
                    DeviceItem(
                        device = device,
                        isSelected = device.id in localSelectedDevices,
                        isSelectionMode = isSelectionMode,
                        canSelect = canSelectDevice(device, localSelectedDevices, devices),
                        onSelectionChange = { isSelected ->
                            val newSelection = if (isSelected) {
                                localSelectedDevices + device.id
                            } else {
                                localSelectedDevices - device.id
                            }
                            localSelectedDevices = newSelection
                            onDeviceSelectionChange(newSelection)
                        },
                        onEnterSelectionMode = {
                            isSelectionMode = true
                            localSelectedDevices = setOf(device.id)
                            onDeviceSelectionChange(setOf(device.id))
                        },
                        onRemoveDevice = onRemoveDevice
                    )
                }

                item {
                    AddDeviceCard(onAddDevice = onAddDevice)
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: DeviceInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    canSelect: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onRemoveDevice: (String) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canSelect || isSelected || !isSelectionMode) 1f else 0.6f)
            .clickable(enabled = canSelect || isSelected || !isSelectionMode) {
                if (isSelectionMode) {
                    onSelectionChange(!isSelected)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else if (device.isConnected)
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            else
                Color.Red.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Checkbox - only visible in selection mode
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChange,
                        enabled = canSelect || isSelected,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                // Device Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (!device.isConnected && device.lastSeen != null) {
                        Text(
                            text = "active ${device.lastSeen}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Status Indicators and Menu
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Connection Status
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = if (device.isConnected) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (device.isConnected)
                                Color(0xFF22C55E).copy(alpha = 0.1f)
                            else
                                Color(0xFFEF4444).copy(alpha = 0.1f),
                            labelColor = if (device.isConnected)
                                Color(0xFF22C55E)
                            else
                                Color(0xFFEF4444)
                        ),
                        border = BorderStroke(
                            0.5.dp,
                            if (device.isConnected)
                                Color(0xFF22C55E).copy(alpha = 0.3f)
                            else
                                Color(0xFFEF4444).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(20.dp)
                            .width(66.dp)
                    )

                    // UV Status (only if connected)
                    if (device.isConnected) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = if (device.uvLightOn)
                                            Color(0xFF3B82F6)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (device.uvLightOn) "ON" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (device.uvLightOn)
                                    Color(0xFF3B82F6).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (device.uvLightOn)
                                    Color(0xFF3B82F6)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(
                                0.5.dp,
                                if (device.uvLightOn)
                                    Color(0xFF3B82F6).copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(20.dp)
                                .width(66.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Menu Button
                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select") },
                                onClick = {
                                    showDropdownMenu = false
                                    onEnterSelectionMode()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Remove",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    onRemoveDevice(device.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDeviceCard(
    onAddDevice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddDevice() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Device",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add New Device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun canSelectDevice(
    device: DeviceInfo,
    currentSelection: Set<String>,
    allDevices: List<DeviceInfo>
): Boolean {
    if (!device.isConnected) return false
    if (currentSelection.isEmpty()) return true
    if (device.id in currentSelection) return true

    val selectedDevices = allDevices.filter { it.id in currentSelection && it.isConnected }
    if (selectedDevices.isEmpty()) return true

    return selectedDevices.all { it.uvLightOn == device.uvLightOn }
}

// Sample Data
private fun getSampleDevices() = listOf(
    DeviceInfo(
        id = "device1",
        name = "Large AQ 1",
        ipAddress = "192.168.1.101",
        isConnected = true,
        uvLightOn = true
    ),
    DeviceInfo(
        id = "device2",
        name = "Large AQ 2",
        ipAddress = "192.168.1.102",
        isConnected = true,
        uvLightOn = true
    ),
    DeviceInfo(
        id = "device3",
        name = "Small AQ 1",
        ipAddress = "192.168.1.103",
        isConnected = true,
        uvLightOn = false,
        lastSeen = "2 hours ago"
    ),
    DeviceInfo(
        id = "device4",
        name = "Small AQ 2",
        ipAddress = "192.168.1.104",
        isConnected = true,
        uvLightOn = false
    ),
    DeviceInfo(
        id = "device5",
        name = "Indoor AQ 1",
        ipAddress = "192.168.1.105",
        isConnected = false,
        uvLightOn = false,
        lastSeen = "1 day ago"
    )
)