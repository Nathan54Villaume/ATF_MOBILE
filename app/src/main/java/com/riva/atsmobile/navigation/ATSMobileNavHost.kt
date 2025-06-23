package com.riva.atsmobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.riva.atsmobile.ui.screens.*
import com.riva.atsmobile.viewmodel.ChangeoverViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun ATSMobileNavHost(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = Routes.Login,
        modifier         = modifier
    ) {
        // LOGIN
        composable(Routes.Login) {
            val vm = hiltViewModel<SelectionViewModel>()
            LoginScreen(navController, vm)
        }

        // HOME
        composable(Routes.Home) {
            val vm = hiltViewModel<SelectionViewModel>()
            HomeScreen(vm, navController)
        }

        // CHANGEMENT GAMME – vue d’intro
        composable(Routes.ChangementGamme) {
            val vm = hiltViewModel<SelectionViewModel>()
            ChangementGammeScreen(vm, navController)
        }

        // WIZARD
        composable(Routes.StepWizard) {
            val selVm    = hiltViewModel<SelectionViewModel>()
            val covVm    = hiltViewModel<ChangeoverViewModel>()
            StepWizardScreen(
                selectionViewModel  = selVm,
                changeoverViewModel = covVm,
                navController       = navController
            )
        }

        // AUTRES ROUTES
        composable(Routes.ChangePassword) {
            val vm = hiltViewModel<SelectionViewModel>()
            ChangePasswordScreen(vm, navController)
        }
        composable(Routes.Settings) {
            val vm = hiltViewModel<SelectionViewModel>()
            ParametresScreen(navController, vm)
        }
        composable(Routes.DevTools) {
            val vm = hiltViewModel<SelectionViewModel>()
            DevSettingsScreen(navController, vm)
        }
        composable(Routes.TypeOperation) {
            val vm = hiltViewModel<SelectionViewModel>()
            TypeOperationScreen(vm, navController)
        }
        composable(Routes.TypeOperationParametres) {
            val vm = hiltViewModel<SelectionViewModel>()
            TypeOperationParamScreen(vm, navController)
        }
        composable(Routes.DashboardATS) {
            val vm = hiltViewModel<SelectionViewModel>()
            DashboardATSScreen(navController, vm)
        }
        composable(Routes.DashboardATR) {
            val vm = hiltViewModel<SelectionViewModel>()
            DashboardATRScreen(navController, vm)
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
