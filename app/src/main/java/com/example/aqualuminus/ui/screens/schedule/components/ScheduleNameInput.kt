package com.example.aqualuminus.ui.screens.schedule.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ScheduleNameInput(
    scheduleName: String,
    onScheduleNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = scheduleName,
        onValueChange = onScheduleNameChanged,
        label = { Text("Schedule Name (Optional)") },
        placeholder = { Text("e.g., Weekly Tank Cleaning") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleNameInput() {
    MaterialTheme {
        ScheduleNameInput(
            scheduleName = "",
            onScheduleNameChanged = {}
        )
    }
}