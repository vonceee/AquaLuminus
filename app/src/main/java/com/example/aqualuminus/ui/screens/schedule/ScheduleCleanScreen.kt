@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.aqualuminus.ui.screens.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun ScheduleCleanScreen(
    onBackClick: () -> Unit,
) {
    val currentTime = Calendar.getInstance()
    var selectedHour by remember { mutableIntStateOf(currentTime.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.get(Calendar.MINUTE)) }
    var selectedAmPm by remember { mutableIntStateOf(if (currentTime.get(Calendar.AM_PM) == Calendar.AM) 0 else 1) }

    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var scheduleName by remember { mutableStateOf("") }

    val daysOfWeek = listOf("M", "T", "W", "Th", "F", "Sa", "Su")
    val daysFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedule Cleaning",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val displayHour = if (selectedHour == 0) 12 else selectedHour
                            val amPmText = if (selectedAmPm == 0) "AM" else "PM"
                            val timeString = String.format(
                                "%02d:%02d %s",
                                displayHour,
                                selectedMinute,
                                amPmText
                            )

                            val selectedDayNames = selectedDays.sorted().map { daysFullNames[it] }

                            Log.d("ScheduleClean", "Schedule saved:")
                            Log.d("ScheduleClean", "Time: $timeString")
                            Log.d("ScheduleClean", "Days: ${selectedDayNames.joinToString(", ")}")
                            Log.d("ScheduleClean", "Name: ${scheduleName.ifBlank { "Untitled Schedule" }}")

                            onBackClick()
                        },
                        enabled = selectedDays.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Schedule")
                    }
                }
            }
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
            // Days Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Select Days",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedDays.contains(index))
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedDays.contains(index))
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedDays = if (selectedDays.contains(index)) {
                                            selectedDays - index
                                        } else {
                                            selectedDays + index
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (selectedDays.contains(index))
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val selectedDaysText = when {
                        selectedDays.size == 7 -> "Every day"
                        selectedDays.isEmpty() -> "No days selected"
                        else -> {
                            val selectedDayNames = selectedDays.sorted().map { daysFullNames[it] }
                            "Selected days: ${selectedDayNames.joinToString(", ")}"
                        }
                    }
                    Text(
                        text = selectedDaysText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Time Picker
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Display current selected time
                    val displayHour = if (selectedHour == 0) 12 else selectedHour
                    val amPmText = if (selectedAmPm == 0) "AM" else "PM"
                    Text(
                        text = String.format("%02d:%02d %s", displayHour, selectedMinute, amPmText),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
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
                            onValueChange = { selectedHour = it },
                            range = 1..12,
                            modifier = Modifier.weight(1f),
                            label = "Hour"
                        )

                        // Separator
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Minute Picker
                        NumberPicker(
                            value = selectedMinute,
                            onValueChange = { selectedMinute = it },
                            range = 0..59,
                            modifier = Modifier.weight(1f),
                            formatValue = { "%02d".format(it) },
                            label = "Minute"
                        )

                        // AM/PM Picker
                        AmPmPicker(
                            value = selectedAmPm,
                            onValueChange = { selectedAmPm = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Schedule Name
            OutlinedTextField(
                value = scheduleName,
                onValueChange = { scheduleName = it },
                label = { Text("Schedule Name (Optional)") },
                placeholder = { Text("e.g., Weekly Tank Cleaning") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedDays.isEmpty()) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    formatValue: (Int) -> String = { it.toString() },
    label: String = ""
) {
    val listState = rememberLazyListState()
    val values = range.toList()
    val itemHeight = 48.dp
    val visibleItemsCount = 3

    // find the index of current value
    val currentIndex = values.indexOf(value).coerceAtLeast(0)

    // track if we're programmatically scrolling
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != currentIndex) {
            isProgrammaticScroll = true
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = 0
            )
            isProgrammaticScroll = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .height(itemHeight * visibleItemsCount)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Add top spacer for proper centering
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }

                itemsIndexed(values) { index, item ->
                    val isSelected = item == value
                    val alpha = if (isSelected) 1f else 0.6f

                    Text(
                        text = formatValue(item),
                        style = if (isSelected) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onValueChange(item) }
                    )
                }

                // Add bottom spacer for proper centering
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun AmPmPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("AM", "PM")
    val listState = rememberLazyListState()
    val itemHeight = 48.dp

    // track if we're programmatically scrolling
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != value) {
            isProgrammaticScroll = true
            listState.animateScrollToItem(value)
            isProgrammaticScroll = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Period",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .height(itemHeight * 3)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Add top spacer
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }

                itemsIndexed(options) { index, item ->
                    val isSelected = index == value
                    val alpha = if (isSelected) 1f else 0.6f

                    Text(
                        text = item,
                        style = if (isSelected) {
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onValueChange(index) }
                    )
                }

                // Add bottom spacer
                item {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }

            // Selection Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleCleanScreen() {
    MaterialTheme {
        ScheduleCleanScreen({})
    }
}