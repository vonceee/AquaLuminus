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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus.ui.screens.schedule.components.DaySelector
import com.example.aqualuminus.ui.screens.schedule.components.DurationPicker
import com.example.aqualuminus.ui.screens.schedule.components.ScheduleBottomBar
import com.example.aqualuminus.ui.screens.schedule.components.ScheduleNameInput
import com.example.aqualuminus.ui.screens.schedule.components.TimePicker
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import java.util.Calendar

@Composable
fun ScheduleCleanScreen(
    scheduleId: String? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(context = context)
    )

    val isEditMode = scheduleId != null
    val currentTime = Calendar.getInstance()

    // state variables for form inputs
    var selectedHour by remember { mutableIntStateOf(currentTime.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.get(Calendar.MINUTE)) }
    var selectedAmPm by remember { mutableIntStateOf(if (currentTime.get(Calendar.AM_PM) == Calendar.AM) 0 else 1) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var scheduleName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(30) } // Default 30 minutes

    val daysFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val snackbarHostState = remember { SnackbarHostState() }

    // observe ViewModel states
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingSchedule by viewModel.isLoadingSchedule.collectAsState()
    val currentSchedule by viewModel.currentSchedule.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val error by viewModel.error.collectAsState()

    // load existing schedule if in edit mode
    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            viewModel.loadSchedule(scheduleId)
        }
    }

    // populate form with existing schedule data
    LaunchedEffect(currentSchedule) {
        currentSchedule?.let { schedule ->
            scheduleName = schedule.name
            selectedDuration = schedule.durationMinutes

            // parse time (stored as 24-hour format)
            val timeParts = schedule.time.split(":")
            if (timeParts.size == 2) {
                val hour24 = timeParts[0].toIntOrNull() ?: 12
                val minute = timeParts[1].toIntOrNull() ?: 0

                // convert to 12-hour format for display
                selectedAmPm = if (hour24 >= 12) 1 else 0 // 0 = AM, 1 = PM
                selectedHour = when {
                    hour24 == 0 -> 12 // 00:xx = 12:xx AM
                    hour24 > 12 -> hour24 - 12 // 13:xx+ = 1:xx+ PM
                    else -> hour24 // 1-12 stays the same
                }
                selectedMinute = minute
            }

            // convert selected days to indices
            val dayIndices = schedule.days.mapNotNull { dayName ->
                daysFullNames.indexOf(dayName).takeIf { it >= 0 }
            }.toSet()
            selectedDays = dayIndices
        }
    }

    // handle save result
    LaunchedEffect(saveResult) {
        when (saveResult) {
            is ScheduleViewModel.SaveResult.Success -> {
                val message = if (isEditMode) "Schedule updated successfully!" else "Schedule saved successfully!"
                snackbarHostState.showSnackbar(message)
                viewModel.clearSaveResult()
                viewModel.clearCurrentSchedule() // clear current schedule
                onBackClick()
            }
            is ScheduleViewModel.SaveResult.Error -> {
                snackbarHostState.showSnackbar("Error: ${(saveResult as ScheduleViewModel.SaveResult.Error).message}")
                viewModel.clearSaveResult()
            }
            null -> { /* no action needed */ }
        }
    }

    // handle errors
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearError()
        }
    }

    // clean up when leaving screen
    LaunchedEffect(Unit) {
        return@LaunchedEffect onDispose {
            viewModel.clearCurrentSchedule()
        }
    }

    val onSaveClick = {
        // Convert 12-hour format to 24-hour format for storage
        val hour24 = when {
            selectedAmPm == 0 && selectedHour == 12 -> 0 // 12 AM = 00
            selectedAmPm == 1 && selectedHour != 12 -> selectedHour + 12 // PM (not 12)
            else -> selectedHour // AM (not 12) or 12 PM
        }

        val timeString = String.format("%02d:%02d", hour24, selectedMinute)
        val selectedDayNames = selectedDays.sorted().map { daysFullNames[it] }

        val schedule = SavedSchedule(
            id = scheduleId ?: "", // use existing ID for updates, empty for new schedules
            name = scheduleName.ifBlank { "Untitled Schedule" },
            days = selectedDayNames,
            time = timeString,
            durationMinutes = selectedDuration,
            isActive = currentSchedule?.isActive ?: true // preserve active state or default to true
        )

        Log.d("ScheduleClean", "Saving schedule (Edit mode: $isEditMode):")
        Log.d("ScheduleClean", "ID: ${schedule.id}")
        Log.d("ScheduleClean", "Time: $timeString (24-hour format)")
        Log.d("ScheduleClean", "Days: ${selectedDayNames.joinToString(", ")}")
        Log.d("ScheduleClean", "Name: ${schedule.name}")
        Log.d("ScheduleClean", "Duration: $selectedDuration minutes")

        // use appropriate method based on edit mode
        if (isEditMode && scheduleId != null) {
            viewModel.updateSchedule(schedule)
        } else {
            viewModel.saveSchedule(schedule)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Schedule" else "Schedule Cleaning",
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
                isSaveEnabled = selectedDays.isNotEmpty() && !isLoading && !isLoadingSchedule,
                saveButtonText = if (isEditMode) "Update" else "Save"
            )
        }
    ) { padding ->
        if (isLoadingSchedule && isEditMode) {
            // show loading indicator while loading schedule for edit
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading schedule...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
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

                DurationPicker(
                    selectedMinutes = selectedDuration,
                    onMinutesChanged = { selectedDuration = it }
                )

                ScheduleNameInput(
                    scheduleName = scheduleName,
                    onScheduleNameChanged = { scheduleName = it }
                )
            }
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

@Preview(showBackground = true)
@Composable
fun PreviewEditScheduleCleanScreen() {
    MaterialTheme {
        ScheduleCleanScreen(scheduleId = "sample_id")
    }
}