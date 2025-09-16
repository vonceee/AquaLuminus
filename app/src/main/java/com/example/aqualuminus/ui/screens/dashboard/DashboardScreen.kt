package com.example.aqualuminus.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus.data.repository.UVLightRepository
import com.example.aqualuminus.ui.screens.dashboard.components.DeviceSelectionCard
import com.example.aqualuminus.ui.screens.dashboard.components.HeaderSection
import com.example.aqualuminus.ui.screens.dashboard.components.QuickActionsCard
import com.example.aqualuminus.ui.screens.dashboard.components.SystemHealthCard
import com.example.aqualuminus.ui.screens.dashboard.components.TemperatureCard
import com.example.aqualuminus.ui.screens.dashboard.components.UVLightControlCard
import com.example.aqualuminus.ui.screens.dashboard.components.WaterClarityCard
import com.example.aqualuminus.ui.screens.dashboard.model.StatusType
import com.example.aqualuminus.ui.screens.dashboard.model.SystemStatus

@Composable
fun AquariumDashboard(
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onScheduleCleanClick: () -> Unit = {},
    onWaterTestClick: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = createDashboardViewModel()
) {
    val uiState by remember { dashboardViewModel.uiState }.collectAsState()

    DashboardContent(
        uiState = uiState,
        onProfileClick = onProfileClick,
        onLogout = onLogout,
        onScheduleCleanClick = onScheduleCleanClick,
        onWaterTestClick = onWaterTestClick,
        onUvLightToggle = { dashboardViewModel.toggleUVLight() },
        onRefresh = { dashboardViewModel.refreshUVStatus() },
        onClearError = { dashboardViewModel.clearError() },
        uvLightRepository = dashboardViewModel.uvLightRepository
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onScheduleCleanClick: () -> Unit,
    onWaterTestClick: () -> Unit,
    onUvLightToggle: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    uvLightRepository: UVLightRepository
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderSection(
            userName = uiState.userName,
            userPhotoUrl = uiState.userPhotoUrl,
            onProfileClick = onProfileClick,
            onLogout = onLogout
        )

        UVLightControlCard(
            uvLightOn = uiState.uvLightOn,
            isLoading = uiState.isLoading,
            isConnected = uiState.isConnected,
            error = uiState.error,
            uvLightDuration = uiState.uvLightDuration,
            onUvLightToggle = { _ -> onUvLightToggle() },
            onRefresh = onRefresh,
            onClearError = onClearError
        )

        DeviceSelectionCard(
            // Sample devices for UI demonstration
            // In production, this would come from your ViewModel
            onBulkToggle = {
                // Handle bulk toggle action here
                // This will be connected to your repository later
                // For now, it's just a UI demonstration
            },
            onDeviceSelectionChange = { selectedDevices ->
                // Handle device selection changes
                // This could trigger UI updates or store selection state
                // For now, it's just for UI demonstration
            }
        )

        EnvironmentalMonitoringSection()

        SystemHealthCard(
            systemStatus = getSystemStatus(),
            uvLightRepository = uvLightRepository
        )

        QuickActionsCard(
            onScheduleCleanClick = onScheduleCleanClick,
            onWaterTestClick = onWaterTestClick
        )
    }
}

@Composable
private fun EnvironmentalMonitoringSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TemperatureCard(
            temperature = getTemperature(),
            modifier = Modifier.weight(1f)
        )
        WaterClarityCard(
            waterClarity = getWaterClarity(),
            modifier = Modifier.weight(1f)
        )
    }
}

// Data providers - can be moved to ViewModel later
private fun getTemperature(): Float = 24.5f
private fun getWaterClarity(): Int = 92
private fun getSystemStatus(): SystemStatus = SystemStatus(
    type = StatusType.NORMAL,
    message = "All Systems Operational"
)

@Composable
private fun createDashboardViewModel(): DashboardViewModel {
    val context = LocalContext.current
    val repository = remember { UVLightRepository(context.applicationContext) }
    return viewModel(
        factory = DashboardViewModelFactory(repository)
    )
}

@Preview(showBackground = true)
@Composable
fun AquariumDashboardPreview() {
    MaterialTheme {
        AquariumDashboard()
    }
}