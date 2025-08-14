@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aqualuminus.ui.screens.watertest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class WaterParameter(
    val id: String,
    val name: String,
    val value: String,
    val unit: String,
    val status: ParameterStatus,
    val icon: ImageVector,
    val range: String
)

enum class ParameterStatus {
    NORMAL, WARNING, CRITICAL
}

// Pre-computed color schemes for better performance
private object WaterTestColors {
    val normalColors = StatusColors(
        background = Color(0xFFDCFCE7),
        text = Color(0xFF166534),
        dot = Color(0xFF10B981),
        icon = Color(0xFF059669)
    )

    val warningColors = StatusColors(
        background = Color(0xFFFEF3C7),
        text = Color(0xFF92400E),
        dot = Color(0xFFF59E0B),
        icon = Color(0xFFD97706)
    )

    val criticalColors = StatusColors(
        background = Color(0xFFFEE2E2),
        text = Color(0xFF991B1B),
        dot = Color(0xFFEF4444),
        icon = Color(0xFFDC2626)
    )
}

private data class StatusColors(
    val background: Color,
    val text: Color,
    val dot: Color,
    val icon: Color
)

@Composable
fun WaterTestScreen(
    onBackClick: () -> Unit = {},
    onTestWater: () -> Unit = {}
) {
    val waterParameters = remember {
        listOf(
            WaterParameter(
                id = "ph",
                name = "pH Level",
                value = "7.2",
                unit = "",
                status = ParameterStatus.NORMAL,
                icon = Icons.Default.Warning,
                range = "6.8-7.6"
            ),
            WaterParameter(
                id = "ammonia",
                name = "Ammonia",
                value = "0.25",
                unit = "ppm",
                status = ParameterStatus.WARNING,
                icon = Icons.Default.Warning,
                range = "0-0.25"
            ),
            WaterParameter(
                id = "nitrite",
                name = "Nitrite",
                value = "0.0",
                unit = "ppm",
                status = ParameterStatus.NORMAL,
                icon = Icons.Default.Warning,
                range = "0-0.25"
            ),
            WaterParameter(
                id = "nitrate",
                name = "Nitrate",
                value = "10",
                unit = "ppm",
                status = ParameterStatus.NORMAL,
                icon = Icons.Default.Warning,
                range = "0-20"
            ),
            WaterParameter(
                id = "temperature",
                name = "Temperature",
                value = "24.5",
                unit = "°C",
                status = ParameterStatus.NORMAL,
                icon = Icons.Default.Warning,
                range = "22-26"
            ),
            WaterParameter(
                id = "salinity",
                name = "Salinity",
                value = "35",
                unit = "ppt",
                status = ParameterStatus.NORMAL,
                icon = Icons.Default.Warning,
                range = "34-36"
            )
        )
    }

    // Derived state for overall status
    val overallStatus = remember(waterParameters) {
        derivedStateOf {
            val criticalCount = waterParameters.count { it.status == ParameterStatus.CRITICAL }
            val warningCount = waterParameters.count { it.status == ParameterStatus.WARNING }

            when {
                criticalCount > 0 -> Triple("Critical", "Immediate attention required", Color(0xFFEF4444))
                warningCount > 0 -> Triple("Good", "$warningCount parameter${if (warningCount > 1) "s" else ""} need attention", Color(0xFFF59E0B))
                else -> Triple("Excellent", "All parameters optimal", Color(0xFF10B981))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Water Test",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(0.dp))
            }

            // Last Test Info Card
            item {
                LastTestCard()
            }

            // Water Parameters List
            items(
                items = waterParameters,
                key = { it.id }
            ) { parameter ->
                WaterParameterCard(parameter = parameter)
            }

            // Overall Status Card
            item {
                OverallStatusCard(
                    status = overallStatus.value.first,
                    description = overallStatus.value.second,
                    statusColor = overallStatus.value.third
                )
            }

            // Test Water Button
            item {
                TestWaterButton(onClick = onTestWater)
            }

            item {
                Text(
                    text = " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LastTestCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last Tested",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "2 hours ago",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OverallStatusCard(
    status: String,
    description: String,
    statusColor: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Water Quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor,
                modifier = Modifier
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TestWaterButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Test Water Now",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WaterParameterCard(parameter: WaterParameter) {
    val statusColors = remember(parameter.status) {
        when (parameter.status) {
            ParameterStatus.NORMAL -> WaterTestColors.normalColors
            ParameterStatus.WARNING -> WaterTestColors.warningColors
            ParameterStatus.CRITICAL -> WaterTestColors.criticalColors
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side - Icon and Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColors.icon.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = parameter.icon,
                            contentDescription = null,
                            tint = statusColors.icon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = parameter.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Range: ${parameter.range}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Side - Value and Status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = parameter.value,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (parameter.unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parameter.unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = statusColors.dot,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = statusColors.background
                    ) {
                        Text(
                            text = parameter.status.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColors.text,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWaterTestScreen() {
    MaterialTheme {
        WaterTestScreen()
    }
}