@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.aqualuminus.ui.screens.schedule

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aqualuminus.ui.screens.schedule.components.DaySelector
import com.example.aqualuminus.ui.screens.schedule.components.ScheduleBottomBar
import com.example.aqualuminus.ui.screens.schedule.components.ScheduleNameInput
import com.example.aqualuminus.ui.screens.schedule.components.TimePicker
import java.util.Calendar

@Composable
fun ScheduleCleanScreen(
    onBackClick: () -> Unit = {},
) {
    val currentTime = Calendar.getInstance()
    var selectedHour by remember { mutableIntStateOf(currentTime.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.get(Calendar.MINUTE)) }
    var selectedAmPm by remember { mutableIntStateOf(if (currentTime.get(Calendar.AM_PM) == Calendar.AM) 0 else 1) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var scheduleName by remember { mutableStateOf("") }

    val daysFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val onSaveClick = {
        val displayHour = if (selectedHour == 0) 12 else selectedHour
        val amPmText = if (selectedAmPm == 0) "AM" else "PM"
        val timeString = String.format("%02d:%02d %s", displayHour, selectedMinute, amPmText)
        val selectedDayNames = selectedDays.sorted().map { daysFullNames[it] }

        Log.d("ScheduleClean", "Schedule saved:")
        Log.d("ScheduleClean", "Time: $timeString")
        Log.d("ScheduleClean", "Days: ${selectedDayNames.joinToString(", ")}")
        Log.d("ScheduleClean", "Name: ${scheduleName.ifBlank { "Untitled Schedule" }}")

        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedule Cleaning",
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
        bottomBar = {
            ScheduleBottomBar(
                onCancelClick = onBackClick,
                onSaveClick = onSaveClick,
                isSaveEnabled = selectedDays.isNotEmpty()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DaySelector(
                selectedDays = selectedDays,
                onDaysChanged = { selectedDays = it }
            )

            TimePicker(
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                selectedAmPm = selectedAmPm,
                onHourChanged = { selectedHour = it },
                onMinuteChanged = { selectedMinute = it },
                onAmPmChanged = { selectedAmPm = it }
            )

            ScheduleNameInput(
                scheduleName = scheduleName,
                onScheduleNameChanged = { scheduleName = it }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleCleanScreen() {
    MaterialTheme {
        ScheduleCleanScreen()
    }
}