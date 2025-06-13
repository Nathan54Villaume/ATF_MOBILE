package com.riva.atsmobile.ui.screens

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.DisposableEffect
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
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val matricule    by viewModel.matricule.collectAsState()

    // Instanciation unique du client SignalR
    val signalRClient = remember(context, matricule) {
        SignalRClientAutoDetect(context, matricule)
    }

    // Démarrage une seule fois et configuration des callbacks
    LaunchedEffect(Unit) {
        Log.i("TypeOpScreen", "▶ Lancement (matricule=$matricule)")
        signalRClient.apply {
            onReceiveGammesError = { err ->
                scope.launch { snackbarHost.showSnackbar("Erreur gammes : $err") }
            }
            onReceiveGammes = { list ->
                Log.i("SignalR-RAW", "RAW JSON gammes reçues : $list")
                viewModel.setGammes(list)
            }
            connectAndFetchGammes(4.5, 7.0)
        }
    }

    DisposableEffect(Unit) {
        onDispose { signalRClient.disconnect() }
    }

    val isConnected  by viewModel.isOnline.collectAsState()
    val gammes       by viewModel.gammes.collectAsState()
    val current      by viewModel.currentGamme.collectAsState()
    val desired      by viewModel.desiredGamme.collectAsState()
    val zone         by viewModel.zoneDeTravail.collectAsState()
    val intervention by viewModel.intervention.collectAsState()

    BaseScreen(
        title            = "Type d’opération",
        navController    = navController,
        viewModel        = viewModel,
        showBack         = true,
        showLogout       = false,
        connectionStatus = isConnected
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    "Sélectionnez vos gammes",
                    style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (gammes.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chargement...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    GammeGrid(
                        title    = "GAMME ACTUELLE",
                        gammes   = gammes,
                        selected = current,
                        onSelect = viewModel::selectCurrentGamme,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.height(16.dp))
                    GammeGrid(
                        title    = "GAMME VISÉE",
                        gammes   = gammes,
                        selected = desired,
                        onSelect = viewModel::selectDesiredGamme,
                        restrict = current,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                DetailsRow(current, desired)
                Spacer(Modifier.weight(1f))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(
                        onClick  = { navController.popBackStack() },
                        shape    = RoundedCornerShape(50),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retour")
                    }
                    ElevatedButton(
                        onClick  = {
                            viewModel.validateGammeChange { _, msg ->
                                scope.launch { snackbarHost.showSnackbar(msg) }
                            }
                        },
                        enabled  = current != null && desired != null,
                        shape    = RoundedCornerShape(50),
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
                hostState = snackbarHost,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
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
    restrict: Gamme? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns               = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.fillMaxSize()
        ) {
            items(gammes) { gamme ->
                val disabled = restrict != null && gamme == restrict
                val borderColor by animateColorAsState(
                    when {
                        disabled          -> Color.LightGray
                        gamme == selected -> MaterialTheme.colorScheme.primary
                        else              -> Color.Gray
                    },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
                val bgColor by animateColorAsState(
                    when {
                        disabled          -> Color(0xFF2E2E2E)
                        gamme == selected -> MaterialTheme.colorScheme.primary.copy(alpha = .1f)
                        else              -> Color(0xFF1E1E1E)
                    },
                    animationSpec = tween(500)
                )
                val txtColor = when {
                    disabled          -> Color.LightGray
                    gamme == selected -> MaterialTheme.colorScheme.primary
                    else              -> Color.White
                }
                val fw = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
                val scale by animateFloatAsState(
                    targetValue   = if (gamme == selected) 1.05f else 1f,
                    animationSpec = tween(300)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(scale)
                        .background(bgColor, RoundedCornerShape(20.dp))
                        .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(20.dp))
                        .clickable(enabled = !disabled) { onSelect(gamme) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        gamme.designation,
                        color      = txtColor,
                        fontWeight = fw,
                        style      = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsRow(current: Gamme?, desired: Gamme?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        current?.let {
            Column {
                Text("Actuelle : ${it.designation}", fontWeight = FontWeight.SemiBold)
                Text("Maille           : ${it.dimension} mm")
                Text("Chaîne/Trame     : ${it.diamChaineTrame}")
                Text("Esp. fil/chaîne  : ${it.espFilChaineTrame} mm")
            }
        }
        desired?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text("Souhait : ${it.designation}", fontWeight = FontWeight.SemiBold)
                Text("Maille           : ${it.dimension} mm")
                Text("Chaîne/Trame     : ${it.diamChaineTrame}")
                Text("Esp. fil/chaîne  : ${it.espFilChaineTrame} mm")
            }
        }
    }
}

@Composable
private fun Footer(zone: String, intervention: String) {
    val now = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Zone : $zone  |  Interv. : $intervention", style = MaterialTheme.typography.bodySmall)
        Text(now, style = MaterialTheme.typography.bodySmall)
    }
}
