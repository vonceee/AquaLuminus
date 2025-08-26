package com.example.aqualuminus.ui.screens.schedule.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun TimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    selectedAmPm: Int,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onAmPmChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Display current selected time
            val displayHour = if (selectedHour == 0) 12 else selectedHour
            val amPmText = if (selectedAmPm == 0) "AM" else "PM"
            Text(
                text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPmText),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hour Picker
                NumberPicker(
                    value = selectedHour,
                    onValueChange = onHourChanged,
                    range = 1..12,
                    modifier = Modifier.weight(1f),
                    label = "Hour"
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Minute Picker
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = onMinuteChanged,
                    range = 0..59,
                    modifier = Modifier.weight(1f),
                    formatValue = { "%02d".format(it) },
                    label = "Minute"
                )

                // AM/PM Picker
                AmPmPicker(
                    value = selectedAmPm,
                    onValueChange = onAmPmChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTimePicker() {
    MaterialTheme {
        TimePicker(
            selectedHour = 9,
            selectedMinute = 30,
            selectedAmPm = 0,
            onHourChanged = {},
            onMinuteChanged = {},
            onAmPmChanged = {}
        )
    }
}