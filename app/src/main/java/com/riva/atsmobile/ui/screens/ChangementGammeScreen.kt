package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.websocket.SignalRClientAutoDetect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ChangementGammeScreen(viewModel: SelectionViewModel, navController: NavController) {
    val context = LocalContext.current
    val lastMessage = remember { mutableStateOf("Aucun message WebSocket reçu pour le moment") }
    val signalRClient = remember { SignalRClientAutoDetect(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isConnected by viewModel.isOnline.collectAsState()
    val matricule by viewModel.matricule.collectAsState()
    val devMode by viewModel.devModeEnabled.collectAsState()

    val matriculeEffectif = remember(matricule, devMode) {
        if (matricule.isNotBlank()) matricule else if (devMode) "DEV" else "INVITÉ"
    }

    // ✅ Connexion SignalR
    LaunchedEffect(Unit) {
        signalRClient.onMessage = { msg ->
            lastMessage.value = msg
            scope.launch {
                snackbarHostState.showSnackbar("📨 $msg")
            }
        }
        signalRClient.connect(matriculeEffectif)
    }

    DisposableEffect(Unit) {
        onDispose { signalRClient.disconnect() }
    }

    BaseScreen(
        title = "Changement de gamme",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔧 Changement de gamme",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "📡 WebSocket ($matriculeEffectif) :\n${lastMessage.value}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val baseUrl = ApiConfig.getBaseUrl(context)
                                val url = URL("$baseUrl/api/message/test-signalr")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.connect()
                                val code = conn.responseCode

                                val message = if (code == 200) {
                                    "✅ Message envoyé avec succès"
                                } else {
                                    "❌ Erreur HTTP ($code)"
                                }

                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("❌ Exception : ${e.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text("🔔 Envoyer un message test")
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
