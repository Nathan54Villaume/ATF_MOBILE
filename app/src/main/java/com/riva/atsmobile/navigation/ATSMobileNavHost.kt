package com.riva.atsmobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.riva.atsmobile.ui.screens.*
import com.riva.atsmobile.viewmodel.ChangeoverViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun ATSMobileNavHost(
    navController: NavHostController,
    selectionViewModel: SelectionViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = Routes.Login,
        modifier         = modifier
    ) {
        composable(Routes.Login) {
            LoginScreen(navController, selectionViewModel)
        }

        composable(Routes.Home) {
            HomeScreen(selectionViewModel, navController)
        }

        composable(Routes.ChangementGamme) {
            ChangementGammeScreen(selectionViewModel, navController)
        }

        composable(Routes.StepWizard) {
            // On récupère le ChangeoverViewModel via viewModel()
            val changeoverVm: ChangeoverViewModel = viewModel()
            StepWizardScreen(
                selectionViewModel  = selectionViewModel,
                changeoverViewModel = changeoverVm,
                navController       = navController
            )
        }

        composable(Routes.ChangePassword) {
            ChangePasswordScreen(selectionViewModel, navController)
        }

        composable(Routes.Settings) {
            ParametresScreen(navController, selectionViewModel)
        }

        composable(Routes.DevTools) {
            DevSettingsScreen(navController, selectionViewModel)
        }

        composable(Routes.TypeOperation) {
            TypeOperationScreen(selectionViewModel, navController)
        }

        composable(Routes.TypeOperationParametres) {
            TypeOperationParamScreen(selectionViewModel, navController)
        }

        composable(Routes.DashboardATS) {
            DashboardATSScreen(navController, selectionViewModel)
        }

        composable(Routes.DashboardATR) {
            DashboardATRScreen(navController, selectionViewModel)
        }
    }
}
