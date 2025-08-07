@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aqualuminus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.aqualuminus.ui.theme.AquaLuminusTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class SystemStatus(
    val type: StatusType,
    val message: String
)

enum class StatusType {
    NORMAL, WARNING, ERROR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquariumDashboard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquariumDashboard() {
    var uvLightOn by remember { mutableStateOf(false) }
    val temperature = 24.5f
    val waterClarity = 92
    val systemStatus = SystemStatus(
        type = StatusType.NORMAL,
        message = "All Systems Operational"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        HeaderSection()

        // UV Light Control
        UVLightControlCard(
            uvLightOn = uvLightOn,
            onUvLightToggle = { uvLightOn = it }
        )

        // Environmental Monitoring
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TemperatureCard(
                temperature = temperature,
                modifier = Modifier.weight(1f)
            )
            WaterClarityCard(
                waterClarity = waterClarity,
                modifier = Modifier.weight(1f)
            )
        }

        // System Health Status
        SystemHealthCard(systemStatus = systemStatus)

        // Quick Actions
        QuickActionsCard()
    }
}

@Composable
fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aquarium UV Cleaner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "IoT Control Dashboard",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UVLightControlCard(
    uvLightOn: Boolean,
    onUvLightToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = "UV Light",
                        tint = if (uvLightOn) Color(0xFF3B82F6) else Color.Gray
                    )
                    Text(
                        text = "UV Light",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (uvLightOn) "ON" else "OFF",
                            fontSize = 12.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (uvLightOn)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (uvLightOn)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Power Control",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = uvLightOn,
                    onCheckedChange = onUvLightToggle
                )
            }

            // UV Active Status
            if (uvLightOn) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEBF8FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "UV Sterilization Active",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF1E40AF),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureCard(
    temperature: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Temperature",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Temperature",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${temperature}°C",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "optimal range",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WaterClarityCard(
    waterClarity: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Water Clarity",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Water Clarity",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${waterClarity}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "excellent quality",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SystemHealthCard(systemStatus: SystemStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "System Health"
                )
                Text(
                    text = "System Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Row
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
                        imageVector = getStatusIcon(systemStatus.type),
                        contentDescription = "Status",
                        tint = getStatusColor(systemStatus.type),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = systemStatus.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = systemStatus.type.name,
                            fontSize = 12.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = getStatusColor(systemStatus.type).copy(alpha = 0.1f),
                        labelColor = getStatusColor(systemStatus.type)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Additional Status Items
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusRow(
                    label = "Connection Status",
                    value = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(text = "Connected")
                        }
                    }
                )

                StatusRow(
                    label = "Last Maintenance",
                    value = { Text(text = "3 days ago") }
                )

                StatusRow(
                    label = "Filter Life",
                    value = { Text(text = "78% remaining") }
                )
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    value: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        value()
    }
}

@Composable
fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Warning,
                    label = "Water Test",
                    iconColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { /* Handle water test */ }
                )

                QuickActionButton(
                    icon = Icons.Default.Warning,
                    label = "Schedule Clean",
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { /* Handle schedule clean */ }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getStatusIcon(type: StatusType): ImageVector {
    return when (type) {
        StatusType.NORMAL -> Icons.Default.CheckCircle
        StatusType.WARNING -> Icons.Default.Warning
        StatusType.ERROR -> Icons.Default.Warning
    }
}

fun getStatusColor(type: StatusType): Color {
    return when (type) {
        StatusType.NORMAL -> Color(0xFF10B981)
        StatusType.WARNING -> Color(0xFFF59E0B)
        StatusType.ERROR -> Color(0xFFEF4444)
    }
}

@Preview(showBackground = true)
@Composable
fun AquariumDashboardPreview() {
    MaterialTheme {
        AquariumDashboard()
    }
}