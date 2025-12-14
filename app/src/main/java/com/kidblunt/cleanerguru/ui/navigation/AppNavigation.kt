package com.kidblunt.cleanerguru.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kidblunt.cleanerguru.ui.screens.BatterySaverScreen
import com.kidblunt.cleanerguru.ui.screens.DashboardScreen
import com.kidblunt.cleanerguru.ui.screens.GamingModeScreen
import com.kidblunt.cleanerguru.ui.screens.OnboardingScreen
import com.kidblunt.cleanerguru.ui.screens.PhotoCleanupScreen
import com.kidblunt.cleanerguru.ui.screens.SettingsScreen
import com.kidblunt.cleanerguru.ui.viewmodel.AuthViewModel
import com.kidblunt.cleanerguru.ui.viewmodel.AuthViewModelFactory

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object PhotoCleanup : Screen("photo_cleanup")
    object BatterySaver : Screen("battery_saver")
    object GamingMode : Screen("gaming_mode")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                authViewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPhotoCleanup = {
                    navController.navigate(Screen.PhotoCleanup.route)
                },
                onNavigateToBatterySaver = {
                    navController.navigate(Screen.BatterySaver.route)
                },
                onNavigateToGamingMode = {
                    navController.navigate(Screen.GamingMode.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.PhotoCleanup.route) {
            PhotoCleanupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BatterySaver.route) {
            BatterySaverScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.GamingMode.route) {
            GamingModeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}