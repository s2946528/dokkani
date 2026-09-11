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

@Composable
fun AppNavigation(
    dokkaniViewModel: DokkaniViewModel,
    loginViewModel: LoginViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUserRole by loginViewModel.currentUserRole.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
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
            if (currentUserRole != null) {
                // Pass currentUserRole to DokkaniApp so it can restrict tabs
                DokkaniApp(
                    viewModel = dokkaniViewModel,
                    currentUserRole = currentUserRole!!,
                    onLogout = {
                        loginViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            } else {
                // Fallback if role is null (e.g. data cleared)
                navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            }
        }
    }
}
