package com.example.aqualuminus.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqualuminus.ui.screens.dashboard.model.StatusType
import com.example.aqualuminus.ui.screens.dashboard.model.SystemStatus

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

private fun getStatusIcon(type: StatusType): ImageVector {
    return when (type) {
        StatusType.NORMAL -> Icons.Default.CheckCircle
        StatusType.WARNING -> Icons.Default.Warning
        StatusType.ERROR -> Icons.Default.Warning
    }
}

private fun getStatusColor(type: StatusType): Color {
    return when (type) {
        StatusType.NORMAL -> Color(0xFF10B981)
        StatusType.WARNING -> Color(0xFFF59E0B)
        StatusType.ERROR -> Color(0xFFEF4444)
    }
}