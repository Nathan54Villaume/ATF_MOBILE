package com.riva.atsmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Logging : BottomNavItem(Routes.Login, "Login", Icons.Filled.Lock)
    object Home : BottomNavItem(Routes.Home, "Accueil", Icons.Filled.Home)
    object Gamme : BottomNavItem(Routes.ChangementGamme, "Gamme", Icons.Filled.Build)
    object Settings : BottomNavItem(Routes.Settings, "Paramètres", Icons.Filled.Settings)
    object DevTools : BottomNavItem(Routes.DevTools, "Dev", Icons.Rounded.DeveloperMode)
}

// Liste complète des onglets possibles (filtrée ensuite dans BottomBar selon rôle/devMode)
val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Gamme,
    BottomNavItem.Settings,
    BottomNavItem.DevTools
)
