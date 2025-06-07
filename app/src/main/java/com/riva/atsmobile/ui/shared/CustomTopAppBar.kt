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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.utils.isNetworkAvailable
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    navController: NavController,
    viewModel: SelectionViewModel,
    showBack: Boolean = true,
    showLogout: Boolean = true,
    onLogoutClick: (() -> Unit)? = null
) {
    val role by viewModel.role.collectAsState()
    val scope = rememberCoroutineScope()
    var isConnected by remember { mutableStateOf(true) }

    // ✅ Ping périodique
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = isNetworkAvailable(navController.context)
            delay(5000)
        }
    }

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
                Text(title)
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
                text = if (isConnected) "✅ Connecté" else "❌ Hors ligne",
                color = if (isConnected) Color.Green else Color.Red,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp)
            )
            if (showLogout && onLogoutClick != null) {
                TextButton(onClick = onLogoutClick) {
                    Text("Déconnexion", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
