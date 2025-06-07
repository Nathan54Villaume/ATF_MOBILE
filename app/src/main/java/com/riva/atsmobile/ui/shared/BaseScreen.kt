package com.riva.atsmobile.ui.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.viewmodel.SelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    navController: NavController,
    viewModel: SelectionViewModel,
    showBack: Boolean = true,
    showLogout: Boolean = false,
    connectionStatus: Boolean? = null,
    onLogout: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val rawRole by viewModel.role.collectAsState()
    val devMode by viewModel.devModeEnabled.collectAsState()
    val role = if (devMode) "ADMIN" else rawRole
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.InitNetworkObserverIfNeeded(context)
    }

    val resolvedConnection = connectionStatus ?: isOnline

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (role == "ADMIN") {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Mode développeur",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(text = title)
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                actions = {
                    Text(
                        text = if (resolvedConnection) "✅ Connecté" else "❌ Hors ligne",
                        color = if (resolvedConnection) Color.Green else Color.Red,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (showLogout && onLogout != null) {
                        TextButton(onClick = onLogout) {
                            Text("Déconnexion", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    actions()
                }
            )
        },
        content = content
    )
}
