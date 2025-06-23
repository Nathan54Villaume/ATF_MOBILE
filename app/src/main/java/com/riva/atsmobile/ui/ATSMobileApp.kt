package com.riva.atsmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riva.atsmobile.navigation.ATSMobileNavHost
import com.riva.atsmobile.navigation.BottomBar
import com.riva.atsmobile.navigation.BottomNavItem
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun ATSMobileApp(viewModel: SelectionViewModel = viewModel()) {
    val navController = rememberNavController()
    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value
        ?.destination
        ?.route

    // Tu peux toujours récupérer le rôle pour afficher/masquer des items
    val role by viewModel.role.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Gamme,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != BottomNavItem.Logging.route) {
                BottomBar(
                    navController = navController,
                    viewModel     = viewModel,
                    items         = bottomNavItems
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // ⚠️ On ne passe plus viewModel ici !
            ATSMobileNavHost(
                navController = navController,
                modifier      = Modifier.fillMaxSize()
            )
        }
    }
}
