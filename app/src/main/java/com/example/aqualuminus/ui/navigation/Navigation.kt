package com.example.aqualuminus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aqualuminus.data.auth.AuthState
import com.example.aqualuminus.ui.screens.dashboard.AquariumDashboard
import com.example.aqualuminus.ui.screens.login.LoginScreen
import com.example.aqualuminus.ui.screens.register.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Register : Screen("register")
}

@Composable
fun AquariumNavGraph(
    authState: AuthState,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    // Auto-navigate based on auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // User is logged in, navigate to dashboard
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                // User is logged out, navigate to login
                if (navController.currentDestination?.route == Screen.Dashboard.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            }
        }
    }

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Dashboard.route
        is AuthState.Unauthenticated -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Firebase Auth state will automatically trigger navigation
                    // No manual navigation needed here
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    // After successful registration, go back to login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Dashboard.route) {
            AquariumDashboard(
                user = (authState as? AuthState.Authenticated)?.user,
                onLogout = {
                    onSignOut()
                    // Firebase Auth state will automatically trigger navigation to login
                }
            )
        }
    }
}