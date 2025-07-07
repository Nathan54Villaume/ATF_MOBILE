package com.riva.atsmobile.navigation // Déclare le package où se trouve ce fichier, typiquement 'navigation'.

import androidx.compose.runtime.Composable // Importe l'annotation @Composable, essentielle pour les fonctions UI de Compose.
import androidx.compose.runtime.LaunchedEffect // Importe LaunchedEffect pour exécuter des effets de bord dans les Composables.
import androidx.compose.runtime.collectAsState // Importe collectAsState pour collecter des États de Flow/StateFlow.
import androidx.compose.runtime.getValue // Importe getValue pour la délégation de propriétés dans les collectAsState.
import androidx.compose.ui.Modifier // Importe Modifier pour modifier l'apparence ou le comportement des Composables.
import androidx.lifecycle.viewmodel.compose.viewModel // Importe viewModel() pour obtenir une instance de ViewModel dans un Composable.
import androidx.navigation.NavHostController // Importe NavHostController, l'objet qui gère la navigation.
import androidx.navigation.compose.NavHost // Importe NavHost, le composable qui héberge les destinations de navigation.
import androidx.navigation.compose.composable // Importe composable, la fonction pour définir une destination de navigation.
import com.riva.atsmobile.ui.screens.* // Importe toutes les fonctions composables qui représentent les écrans de votre application.
import com.riva.atsmobile.viewmodel.SelectionViewModel // Importe SelectionViewModel, un ViewModel utilisé pour la sélection et l'état global.
import com.riva.atsmobile.viewmodel.EtapeViewModel // Importe EtapeViewModel, un ViewModel spécifique aux étapes.

/**
 * Composant NavHost principal de l'application ATS Mobile.
 *
 * Ce Composable définit toutes les destinations de navigation de l'application
 * et gère le routage entre elles. Il intègre également la logique de protection
 * des routes basée sur le rôle de l'utilisateur et le mode développeur.
 *
 * @param navController Le contrôleur de navigation qui gère le graphique de navigation.
 * @param selectionViewModel Le ViewModel qui gère les sélections et l'état global de l'utilisateur (rôle, mode dev).
 * @param modifier Un Modifier pour appliquer des modifications au NavHost.
 */
@Composable
fun ATSMobileNavHost(
    navController: NavHostController,
    selectionViewModel: SelectionViewModel,
    modifier: Modifier = Modifier
) {
    // Collecte le rôle de l'utilisateur depuis le SelectionViewModel comme un état Compose.
    val role by selectionViewModel.role.collectAsState()
    // Collecte l'état du mode développeur depuis le SelectionViewModel comme un état Compose.
    val devMode by selectionViewModel.devModeEnabled.collectAsState()

    // Définit le NavHost qui gérera la navigation.
    NavHost(
        navController    = navController, // Le contrôleur de navigation.
        startDestination = Routes.Login, // La route de départ par défaut lorsque l'application est lancée.
        modifier         = modifier // Le modificateur appliqué au NavHost.
    ) {
        // Définition de la destination pour la route de connexion.
        composable(Routes.Login) {
            LoginScreen(navController, selectionViewModel)
        }

        // Définition de la destination pour la route de l'accueil.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.Home) {
            requireRoleOrDev(role, devMode, navController) {
                HomeScreen(selectionViewModel, navController)
            }
        }

        // Définition de la destination pour le changement de gamme.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.ChangementGamme) {
            requireRoleOrDev(role, devMode, navController) {
                ChangementGammeScreen(selectionViewModel, navController)
            }
        }

        // Définition de la destination pour le changement de mot de passe.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.ChangePassword) {
            requireRoleOrDev(role, devMode, navController) {
                ChangePasswordScreen(selectionViewModel, navController)
            }
        }

        // Définition de la destination pour les paramètres.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.Settings) {
            requireRoleOrDev(role, devMode, navController) {
                // Injection du EtapeViewModel pour cet écran.
                val etapeViewModel: EtapeViewModel = viewModel()
                ParametresScreen(
                    navController      = navController,
                    selectionViewModel = selectionViewModel,
                    etapeViewModel     = etapeViewModel
                )
            }
        }

        // Définition de la destination pour les outils de développement.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.DevTools) {
            requireRoleOrDev(role, devMode, navController) {
                DevSettingsScreen(navController, selectionViewModel)
            }
        }

        // Définition de la destination pour la sélection du type d'opération.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.TypeOperation) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationScreen(selectionViewModel, navController)
            }
        }

        // Définition de la destination pour les paramètres du type d'opération.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.TypeOperationParametres) {
            requireRoleOrDev(role, devMode, navController) {
                TypeOperationParamScreen(selectionViewModel, navController)
            }
        }

        // Définition de la destination pour le tableau de bord ATS.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.DashboardATS) {
            requireRoleOrDev(role, devMode, navController) {
                DashboardATSScreen(navController, selectionViewModel)
            }
        }

        // Définition de la destination pour le tableau de bord ATR.
        // Protégée par la fonction requireRoleOrDev.
        composable(Routes.DashboardATR) {
            requireRoleOrDev(role, devMode, navController) {
                DashboardATRScreen(navController, selectionViewModel)
            }
        }

        // 🆕 Nouvelle destination pour l'assistant d'étapes (StepWizard), également protégée.
        composable(Routes.StepWizard) {
            requireRoleOrDev(role, devMode, navController) {
                val etapeViewModel: EtapeViewModel = viewModel()
                EtapesScreen(
                    navController      = navController,
                    etapeViewModel     = etapeViewModel,
                    selectionViewModel = selectionViewModel
                )
            }
        }


    }
}

/**
 * Fonction Composable utilitaire pour appliquer des restrictions de rôle ou de mode développeur
 * à une destination de navigation.
 *
 * Si l'utilisateur n'a pas le rôle requis et que le mode développeur n'est pas activé,
 * il est redirigé vers l'écran de connexion.
 *
 * @param role Le rôle actuel de l'utilisateur (String).
 * @param devMode L'état du mode développeur (Boolean).
 * @param navController Le contrôleur de navigation pour effectuer la redirection.
 * @param content Le Composable à afficher si l'accès est autorisé.
 */
@Composable
private fun requireRoleOrDev(
    role: String,
    devMode: Boolean,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    // Vérifie si le rôle n'est pas vide (utilisateur connecté avec un rôle) OU si le mode développeur est activé.
    if (role.isNotBlank() || devMode) {
        // Si autorisé, exécute le contenu Composable de la destination.
        content()
    } else {
        // Si non autorisé, déclenche une navigation vers l'écran de connexion.
        // LaunchedEffect assure que cette navigation n'est déclenchée qu'une seule fois
        // et qu'elle n'est pas répétée à chaque recomposition.
        LaunchedEffect(Unit) {
            navController.navigate(Routes.Login) {
                // popUpTo(0) efface toute la pile de retour, empêchant l'utilisateur de revenir aux écrans restreints via le bouton retour.
                popUpTo(0)
            }
        }
    }
}