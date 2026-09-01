package com.phoenix.phoenixnet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phoenix.phoenixnet.ui.auth.AuthScreen
import com.phoenix.phoenixnet.ui.auth.AuthViewModel
import com.phoenix.phoenixnet.ui.blockvoice.BlockvoiceViewModel
import com.phoenix.phoenixnet.ui.dashboard.MainDashboardScreen
import com.phoenix.phoenixnet.ui.dashboard.MeshDashboardViewModel
import com.phoenix.phoenixnet.ui.health.MeshHealthScreen
import com.phoenix.phoenixnet.ui.inbox.MeshInboxScreen
import com.phoenix.phoenixnet.ui.inbox.MeshInboxViewModel
import com.phoenix.phoenixnet.ui.theme.LocalPhoenixThemeState
import com.phoenix.phoenixnet.ui.theme.PhoenixTheme
import com.phoenix.phoenixnet.ui.theme.ThemeViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Inbox : Screen("inbox")
    object Health : Screen("health")
}

@Composable
fun PhoenixNavHost(
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeState = themeViewModel.themeState

    CompositionLocalProvider(LocalPhoenixThemeState provides themeState) {
        PhoenixTheme(
            isDarkMode = themeState.isDarkMode,
            isFireMode = themeState.isFireMode
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Auth.route
            ) {
                composable(Screen.Auth.route) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Dashboard.route) {
                    val meshViewModel: MeshDashboardViewModel = hiltViewModel()
                    val voiceViewModel: BlockvoiceViewModel = hiltViewModel()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    MainDashboardScreen(
                        meshViewModel = meshViewModel,
                        voiceViewModel = voiceViewModel,
                        onNavigateToInbox = { navController.navigate(Screen.Inbox.route) },
                        onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                        onLogout = {
                            authViewModel.onLogout {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable(Screen.Inbox.route) {
                    val inboxViewModel: MeshInboxViewModel = hiltViewModel()
                    MeshInboxScreen(
                        viewModel = inboxViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Health.route) {
                    val meshViewModel: MeshDashboardViewModel = hiltViewModel()
                    MeshHealthScreen(
                        viewModel = meshViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
