package com.example.aqualuminus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aqualuminus.ui.screens.dashboard.AquariumDashboard
import com.example.aqualuminus.ui.screens.login.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
}

@Composable
fun AquariumNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true } // Prevent going back to login
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            AquariumDashboard(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true } // Prevent going back to dashboard
                    }
                }
            )
        }
    }
}
