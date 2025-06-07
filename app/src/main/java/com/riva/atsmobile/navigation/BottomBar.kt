package com.riva.atsmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    NavigationBar {
        filteredItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
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
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                }
            )
        }
    }
}
