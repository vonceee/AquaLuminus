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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    // Get user data from ViewModel
    val userName = dashboardViewModel.userName
    val userPhotoUrl = dashboardViewModel.userPhotoUrl

    var uvLightOn by remember { mutableStateOf(false) }
    val temperature = 24.5f
    val waterClarity = 92
    val systemStatus = SystemStatus(
        type = StatusType.NORMAL,
        message = "All Systems Operational"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with User Greeting, Profile Picture, and Logout
        HeaderSection(
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onProfileClick = onProfileClick,
            onLogout = onLogout
        )

        // UV Light Control
        UVLightControlCard(
            uvLightOn = uvLightOn,
            onUvLightToggle = { uvLightOn = it }
        )

        // Environmental Monitoring
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TemperatureCard(
                temperature = temperature,
                modifier = Modifier.weight(1f)
            )
            WaterClarityCard(
                waterClarity = waterClarity,
                modifier = Modifier.weight(1f)
            )
        }

        // System Health Status
        SystemHealthCard(systemStatus = systemStatus)

        // Quick Actions
        QuickActionsCard(
            onScheduleCleanClick = onScheduleCleanClick,
            onWaterTestClick = onWaterTestClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AquariumDashboardPreview() {
    MaterialTheme {
        AquariumDashboard()
    }
}