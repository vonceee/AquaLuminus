package com.example.aqualuminus.ui.screens.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DurationPicker(
    selectedMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minutes Picker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NumberPicker(
                        value = selectedMinutes,
                        onValueChange = onMinutesChanged,
                        range = 1..120, // 1 to 120 minutes
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Duration Preview
            Text(
                text = buildString {
                    append("Cleaning Duration: ")
                    when {
                        selectedMinutes < 60 -> append("$selectedMinutes minutes")
                        selectedMinutes == 60 -> append("1 hour")
                        selectedMinutes % 60 == 0 -> append("${selectedMinutes / 60} hours")
                        else -> {
                            val hours = selectedMinutes / 60
                            val minutes = selectedMinutes % 60
                            append("$hours hour${if (hours > 1) "s" else ""} $minutes minute${if (minutes > 1) "s" else ""}")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDurationPicker() {
    MaterialTheme {
        DurationPicker(
            selectedMinutes = 30,
            onMinutesChanged = {}
        )
    }
}