package com.example.aqualuminus.ui.screens.schedule.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ScheduleBottomBar(
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaveEnabled: Boolean,
    saveButtonText: String = "Save Schedule",
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSaveClick,
                enabled = isSaveEnabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Text(saveButtonText)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleBottomBar() {
    MaterialTheme {
        ScheduleBottomBar(
            onCancelClick = {},
            onSaveClick = {},
            isSaveEnabled = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditScheduleBottomBar() {
    MaterialTheme {
        ScheduleBottomBar(
            onCancelClick = {},
            onSaveClick = {},
            isSaveEnabled = true,
            saveButtonText = "Update"
        )
    }
}