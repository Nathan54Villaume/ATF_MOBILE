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
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.viewmodel.EtapeViewModel

@Composable
fun ATSMobileNavHost(
    navController: NavHostController,
    selectionViewModel: SelectionViewModel,
    modifier: Modifier = Modifier
) {
    val role by selectionViewModel.role.collectAsState()
    val devMode by selectionViewModel.devModeEnabled.collectAsState()

    NavHost(
        navController    = navController,
        startDestination = Routes.Login,
        modifier         = modifier
    ) {
        composable(Routes.Login) {
            LoginScreen(navController, selectionViewModel)
        }

        composable(Routes.Home) {
            requireRoleOrDev(role, devMode, navController) {
                HomeScreen(selectionViewModel, navController)
            }
        }

        composable(Routes.ChangementGamme) {
            requireRoleOrDev(role, devMode, navController) {
                ChangementGammeScreen(selectionViewModel, navController)
            }
        }

        composable(Routes.ChangePassword) {
            requireRoleOrDev(role, devMode, navController) {
                ChangePasswordScreen(selectionViewModel, navController)
            }
        }

        composable(Routes.Settings) {
            requireRoleOrDev(role, devMode, navController) {
                // Injection du EtapeViewModel
                val etapeViewModel: EtapeViewModel = viewModel()
                ParametresScreen(
                    navController      = navController,
                    selectionViewModel = selectionViewModel,
                    etapeViewModel     = etapeViewModel
                )
            }
        }

        composable(Routes.DevTools) {
            requireRoleOrDev(role, devMode, navController) {
                DevSettingsScreen(navController, selectionViewModel)
            }
        }

        composable(Routes.TypeOperation) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationScreen(selectionViewModel, navController)
            }
        }

        composable(Routes.TypeOperationParametres) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationParamScreen(selectionViewModel, navController)
            }
        }

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

        // 🆕 StepWizard, protégé
        composable("step_wizard") {
            requireRoleOrDev(role, devMode, navController) {
                val etapeViewModel: EtapeViewModel = viewModel()
                EtapesScreen(
                    navController      = navController,
                    etapeViewModel     = etapeViewModel,
                    selectionViewModel = selectionViewModel
                )
            }
        }

        // **La route "param_exclusions" a été supprimée** :
        // - Vous l'affichez désormais dans l'onglet "Exclusions"
        //   de votre ParametresScreen, via ExclusionsParamSection.
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
