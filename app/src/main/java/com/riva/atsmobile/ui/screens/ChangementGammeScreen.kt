package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.navigation.Routes
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.websocket.SignalRClientAutoDetect
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de changement de gamme simplifié :
 * utilise uniquement SignalRClientAutoDetect pour récupérer les gammes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangementGammeScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val signalRClient = remember(context) { SignalRClientAutoDetect(context) }
    val isConnected by viewModel.isOnline.collectAsState()

    DisposableEffect(Unit) {
        signalRClient.onReceiveGammes = { list ->
            viewModel.setGammes(list)
        }
        signalRClient.onReceiveGammesError = { err ->
            scope.launch {
                snackbarHostState.showSnackbar("Erreur gammes : $err")
            }
        }
        signalRClient.connectAndFetchGammes(4.5, 7.0)
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "🔧 Changement de gamme",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(Modifier.height(24.dp))

                ElevatedButton(
                    onClick = { navController.navigate(Routes.TypeOperation) }
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Type d’opération")
                }

                Spacer(Modifier.height(32.dp))

                ElevatedButton(
                    onClick = {
                        viewModel.currentGamme.value?.let {
                            // action si besoin
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Valider GAMMES")
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Dernière mise à jour : " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    fontWeight = FontWeight.Light
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
