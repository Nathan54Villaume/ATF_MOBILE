package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.navigation.Routes
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangementGammeScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    // On récupère simplement l'état de connexion
    val isConnected by viewModel.isOnline.collectAsState()
    // On récupère les gammes sélectionnées
    val selectedCodes by viewModel.gammesSelectionnees.collectAsState()

    BaseScreen(
        title            = "Changement de gamme",
        navController    = navController,
        viewModel        = viewModel,
        showBack         = true,
        showLogout       = false,
        connectionStatus = isConnected
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "🔧 Changement de gamme",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        // On enregistre les gammes sélectionnées et on navigue vers TypeOperationScreen
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedGammes", selectedCodes)
                        navController.navigate(Routes.TypeOperation)
                    }
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Type Opération")
                }
            }
        }
    }
}
