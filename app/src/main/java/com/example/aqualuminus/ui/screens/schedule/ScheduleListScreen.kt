@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aqualuminus.ui.screens.schedule

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus.ui.screens.schedule.components.schedule_list.ScheduleCard
import com.example.aqualuminus.ui.screens.schedule.components.schedule_list.ScheduleEmptyState
import com.example.aqualuminus.ui.screens.schedule.components.schedule_list.ScheduleFloatingActionButton
import com.example.aqualuminus.ui.screens.schedule.components.schedule_list.ScheduleLoadingState
import com.example.aqualuminus.ui.screens.schedule.components.schedule_list.ScheduleTopBar
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule

@Composable
fun SchedulesListScreen(
    onBackClick: () -> Unit = {},
    onCreateNewClick: () -> Unit = {},
    onEditScheduleClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(context = context)
    )

    val schedules by viewModel.schedules.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val scheduleActions = ScheduleActions(
        onToggle = { scheduleId ->
            val schedule = schedules.find { it.id == scheduleId }
            schedule?.let {
                viewModel.updateScheduleStatus(scheduleId, !it.isActive)
            }
        },
        onDelete = viewModel::deleteSchedule,
        onEdit = onEditScheduleClick
    )

    // Handle error messages
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            ScheduleTopBar(
                onBackClick = onBackClick,
                onCreateNewClick = onCreateNewClick
            )
        },
        floatingActionButton = {
            ScheduleFloatingActionButton(onClick = onCreateNewClick)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        ScheduleListContent(
            schedules = schedules,
            actions = scheduleActions,
            isLoading = isLoading,
            onCreateNewClick = onCreateNewClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        )
    }
}

// Rest of the file remains the same...
@Composable
private fun ScheduleListContent(
    schedules: List<SavedSchedule>,
    actions: ScheduleActions,
    isLoading: Boolean,
    onCreateNewClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues
) {
    when {
        isLoading && schedules.isEmpty() -> {
            ScheduleLoadingState(
                modifier = modifier.padding(contentPadding)
            )
        }
        schedules.isEmpty() -> {
            ScheduleEmptyState(
                onCreateNewClick = onCreateNewClick,
                modifier = modifier.padding(contentPadding)
            )
        }
        else -> {
            ScheduleList(
                schedules = schedules,
                actions = actions,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun ScheduleList(
    schedules: List<SavedSchedule>,
    actions: ScheduleActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = modifier
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(schedules) { schedule ->
            ScheduleCard(
                schedule = schedule,
                onToggle = { actions.onToggle(schedule.id) },
                onEdit = { actions.onEdit(schedule.id) },
                onDelete = { actions.onDelete(schedule.id) }
            )
        }
    }
}

// Data class to group schedule actions
data class ScheduleActions(
    val onToggle: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onEdit: (String) -> Unit
)