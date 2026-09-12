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
import com.example.dokkani.ui.screens.users.LoginViewModel
import com.example.dokkani.ui.screens.users.PinLoginScreen

import com.example.dokkani.ui.screens.SplashScreen

@Composable
fun AppNavigation(
    dokkaniViewModel: DokkaniViewModel,
    loginViewModel: LoginViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUserRole by loginViewModel.currentUserRole.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
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
                }
            )
        }
    }
}
