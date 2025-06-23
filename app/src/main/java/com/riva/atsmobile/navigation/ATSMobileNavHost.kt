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
    // États globaux issus du SelectionViewModel
    val role by selectionViewModel.role.collectAsState()
    val devMode by selectionViewModel.devModeEnabled.collectAsState()

    NavHost(
        navController    = navController,
        startDestination = Routes.Login,
        modifier         = modifier
    ) {
        // Écran de login
        composable(Routes.Login) {
            LoginScreen(navController, selectionViewModel)
        }

        // Écran Home, protégé
        composable(Routes.Home) {
            requireRoleOrDev(role, devMode, navController) {
                HomeScreen(selectionViewModel, navController)
            }
        }

        // Intro Changement de gamme, protégé
        composable(Routes.ChangementGamme) {
            requireRoleOrDev(role, devMode, navController) {
                ChangementGammeScreen(selectionViewModel, navController)
            }
        }

        // Wizard, protégé
        composable(Routes.StepWizard) {
            requireRoleOrDev(role, devMode, navController) {
                // ChangeoverViewModel récupéré ici
                val changeoverVm: ChangeoverViewModel = viewModel()
                StepWizardScreen(
                    selectionViewModel  = selectionViewModel,
                    changeoverViewModel = changeoverVm,
                    navController       = navController
                )
            }
        }

        // Change Password, protégé
        composable(Routes.ChangePassword) {
            requireRoleOrDev(role, devMode, navController) {
                ChangePasswordScreen(selectionViewModel, navController)
            }
        }

        // Paramètres (non protégé)
        composable(Routes.Settings) {
            ParametresScreen(navController, selectionViewModel)
        }

        // DevTools, protégé
        composable(Routes.DevTools) {
            requireRoleOrDev(role, devMode, navController) {
                DevSettingsScreen(navController, selectionViewModel)
            }
        }

        // Type d’opération, protégé
        composable(Routes.TypeOperation) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationScreen(selectionViewModel, navController)
            }
        }

        // Paramètres TypeOp, protégé
        composable(Routes.TypeOperationParametres) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationParamScreen(selectionViewModel, navController)
            }
        }

        // Dashboards, protégés
        composable(Routes.DashboardATS) {
            requireRoleOrDev(role, devMode, navController) {
                DashboardATSScreen(navController, selectionViewModel)
            }
        }
        composable(Routes.DashboardATR) {
            requireRoleOrDev(role, devMode, navController) {
                DashboardATRScreen(navController, selectionViewModel)
            }
        }
    }
}

/** N’autorise l’accès qu’aux utilisateurs loggués (ou en devMode) */
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
