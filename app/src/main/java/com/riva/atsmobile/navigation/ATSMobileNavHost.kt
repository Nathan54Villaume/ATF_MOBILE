package com.riva.atsmobile.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.riva.atsmobile.ui.screens.*
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun ATSMobileNavHost(
    navController: NavHostController,
    viewModel: SelectionViewModel,
    modifier: Modifier = Modifier
) {
    val role by viewModel.role.collectAsState()
    val devMode by viewModel.devModeEnabled.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.Login,
        modifier = modifier
    ) {
        composable(Routes.Login) {
            LoginScreen(navController, viewModel)
        }

        composable(Routes.Home) {
            requireRoleOrDev(role, devMode, navController) {
                HomeScreen(viewModel, navController)
            }
        }

        composable(Routes.ChangementGamme) {
            requireRoleOrDev(role, devMode, navController) {
                ChangementGammeScreen(viewModel, navController)
            }
        }

        composable(Routes.ChangePassword) {
            requireRoleOrDev(role, devMode, navController) {
                ChangePasswordScreen(viewModel, navController)
            }
        }

        composable(Routes.Settings) {
            ParametresScreen(navController, viewModel)
        }

        composable(Routes.DevTools) {
            requireRoleOrDev(role, devMode, navController) {
                DevSettingsScreen(navController, viewModel)
            }
        }
    }
}

@Composable
private fun requireRoleOrDev(
    role: String,
    devMode: Boolean,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    if (role.isNotBlank() || devMode) {
        content()
    } else {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.Login) {
                popUpTo(0)
            }
        }
    }
}
