package com.riva.atsmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color // Importation de Color
import androidx.compose.ui.graphics.Brush // Importation de Brush si vous voulez un dégradé
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun BottomBar(
    navController: NavController,
    viewModel: SelectionViewModel,
    items: List<BottomNavItem> = bottomNavItems
) {
    val role by viewModel.role.collectAsState()
    val devModeEnabled by viewModel.devModeEnabled.collectAsState()

    val filteredItems = remember(role, devModeEnabled) {
        items.filter {
            when (it.route) {
                Routes.Home -> devModeEnabled || role.isNotBlank()
                Routes.ChangementGamme -> devModeEnabled || role == "ATS" || role == "ADMIN"
                Routes.Settings -> true
                Routes.DevTools -> devModeEnabled
                else -> false
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        // Fond de la barre de navigation : très sombre pour un look industriel
        containerColor = Color(0xFF1A1A1A), // Un gris très foncé, presque noir
        // Couleur par défaut du contenu des items (icônes, texte)
        contentColor = Color.White
    ) {
        filteredItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        // Couleur de l'icône : vert accent si sélectionné, gris clair sinon
                        tint = if (isSelected) Color(0xFF64FFDA) else Color.LightGray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        // Couleur du texte : vert accent si sélectionné, gris clair sinon
                        color = if (isSelected) Color(0xFF64FFDA) else Color.LightGray
                    )
                },
                // Couleurs de l'indicateur de sélection (le fond sous l'icône/texte)
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF64FFDA),
                    selectedTextColor = Color(0xFF64FFDA),
                    indicatorColor = Color(0xFF2E2E2E) // Un gris foncé pour l'indicateur de sélection
                )
            )
        }
    }
}