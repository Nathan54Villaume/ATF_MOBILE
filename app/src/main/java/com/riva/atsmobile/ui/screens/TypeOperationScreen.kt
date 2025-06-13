package com.riva.atsmobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.websocket.SignalRClientAutoDetect
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeOperationScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val matricule by viewModel.matricule.collectAsState()
    val signalRClient = remember { SignalRClientAutoDetect(context) }

    // Connexion SignalR et chargement des gammes
    LaunchedEffect(Unit) {
        viewModel.InitNetworkObserverIfNeeded(context)

        // Gestion des notifications génériques
        signalRClient.onMessage = { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
        // Réception de la liste des gammes
        signalRClient.onReceiveGammes = { list: List<Gamme> ->
            viewModel.setGammes(list)
        }
        // Erreur serveur
        signalRClient.onReceiveGammesError = { err ->
            scope.launch { snackbarHostState.showSnackbar("Erreur gammes: $err") }
        }

        // Connexion et login
        signalRClient.connect(matricule)
        // Demande des gammes filtrées entre 4.5 et 7.0 mm
        signalRClient.invokeGetLatestGammes(4.5, 7.0)
    }

    val isConnected by viewModel.isOnline.collectAsState()
    val gammes by viewModel.gammes.collectAsState()
    val current by viewModel.currentGamme.collectAsState()
    val desired by viewModel.desiredGamme.collectAsState()
    val zone by viewModel.zoneDeTravail.collectAsState()
    val intervention by viewModel.intervention.collectAsState()

    BaseScreen(
        title = "Type d’opération",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Sélectionnez vos gammes",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                GammeGrid(
                    title = "GAMME ACTUELLE",
                    gammes = gammes,
                    selected = current,
                    onSelect = { viewModel.selectCurrentGamme(it) }
                )

                Spacer(Modifier.height(12.dp))

                GammeGrid(
                    title = "GAMME VISÉE",
                    gammes = gammes,
                    selected = desired,
                    onSelect = { viewModel.selectDesiredGamme(it) },
                    restrict = current
                )

                Spacer(Modifier.height(24.dp))

                AnimatedDetails(current = current, desired = desired)

                Spacer(Modifier.weight(1f))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retour")
                    }
                    ElevatedButton(
                        onClick = {
                            viewModel.validateGammeChange { success, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        enabled = current != null && desired != null,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Valider")
                    }
                }

                Footer(zone, intervention)
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun GammeGrid(
    title: String,
    gammes: List<Gamme>,
    selected: Gamme?,
    onSelect: (Gamme) -> Unit,
    restrict: Gamme? = null
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        items(gammes) { gamme ->
            val isDisabled = restrict != null && gamme == restrict
            val borderColor by animateColorAsState(
                when {
                    isDisabled         -> Color.LightGray
                    gamme == selected  -> MaterialTheme.colorScheme.primary
                    else               -> Color.Gray
                },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
            val backgroundColor by animateColorAsState(
                when {
                    isDisabled         -> Color(0xFF2E2E2E)
                    gamme == selected  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else               -> Color(0xFF1E1E1E)
                },
                animationSpec = tween(500)
            )
            val textColor = when {
                isDisabled         -> Color.LightGray
                gamme == selected  -> MaterialTheme.colorScheme.primary
                else               -> Color.White
            }
            val fontWeight = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
            val scale by animateFloatAsState(
                targetValue = if (gamme == selected) 1.05f else 1f,
                animationSpec = tween(300)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale)
                    .background(backgroundColor, RoundedCornerShape(20.dp))
                    .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(20.dp))
                    .clickable(enabled = !isDisabled) { onSelect(gamme) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = gamme.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = fontWeight
                    )
                )
            }
        }
    }
}

@Composable
private fun AnimatedDetails(current: Gamme?, desired: Gamme?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        current?.let {
            Column {
                Text("Actuelle : ${it.name}", fontWeight = FontWeight.SemiBold)
                Text("Maille: ${it.meshSize} mm")
                Text("Fil: ${it.wireDiameter} mm")
                Text("Chaîne: ${it.chainCount}")
            }
        }
        desired?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text("Souhait : ${it.name}", fontWeight = FontWeight.SemiBold)
                Text("Maille: ${it.meshSize} mm")
                Text("Fil: ${it.wireDiameter} mm")
                Text("Chaîne: ${it.chainCount}")
            }
        }
    }
}

@Composable
private fun Footer(zone: String, intervention: String) {
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Zone: $zone | Interv: $intervention", style = MaterialTheme.typography.bodySmall)
        Text(now, style = MaterialTheme.typography.bodySmall)
    }
}
