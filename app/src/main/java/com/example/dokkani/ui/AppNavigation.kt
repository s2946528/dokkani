package com.example.dokkani.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.screens.DokkaniApp
import com.example.dokkani.ui.screens.SplashScreen
import com.example.dokkani.ui.screens.onboarding.OnboardingWizardScreen
import com.example.dokkani.ui.screens.users.LoginViewModel
import com.example.dokkani.ui.screens.users.PinLoginScreen

@Composable
fun AppNavigation(
    dokkaniViewModel: DokkaniViewModel,
    loginViewModel: LoginViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUserRole by loginViewModel.currentUserRole.collectAsState()
    val isOnboardingCompleted by loginViewModel.isOnboardingCompleted.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    if (!isOnboardingCompleted) {
                        navController.navigate("onboarding") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingWizardScreen(
                viewModel = dokkaniViewModel,
                onFinish = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            PinLoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            DokkaniApp(
                viewModel = dokkaniViewModel,
                currentUserRole = currentUserRole ?: UserRole.ADMIN,
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onOpenOnboardingWizard = {
                    navController.navigate("onboarding")
                }
            )
        }
    }
}
