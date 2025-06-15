package com.riva.atsmobile.ui.screens

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.navigation.Routes
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Trim or fallback
fun String?.safeText(): String = this?.trim().takeIf { !it.isNullOrEmpty() } ?: "-"

// Map designation to drawable resource
fun getImageForGamme(designation: String): Int? = when(designation.trim().uppercase()) {
    "PAF 10"  -> R.drawable.paf10

    else       -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeOperationScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    val isConnected by viewModel.isOnline.collectAsState()
    val gammes      by viewModel.gammes.collectAsState()
    val gammesSelectionnees  by viewModel.gammesSelectionnees.collectAsState()
    val current             by viewModel.currentGamme.collectAsState()
    val desired             by viewModel.desiredGamme.collectAsState()
    val zone                by viewModel.zoneDeTravail.collectAsState()
    val intervention        by viewModel.intervention.collectAsState()
    val role                by viewModel.role.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        try {
            viewModel.chargerGammesDepuisApi(context)
            Log.d("GAMMES", "Nombre de gammes reçues : ${viewModel.gammes.value.size}")
        } catch (e: Exception) {
            loadError = "Erreur de chargement : ${e.message}"
        } finally {
            isLoading = false
        }
    }

    BaseScreen(
        title = "Type d’opération",
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
            Column(Modifier.fillMaxSize()) {
                Text(
                    "Sélectionnez vos gammes",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                when {
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(loadError!!, color = Color.Red)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                scope.launch {
                                    isLoading = true
                                    loadError = null
                                    try {
                                        viewModel.chargerGammesDepuisApi(context)
                                    } catch (e: Exception) {
                                        loadError = "Erreur de chargement : ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }) { Text("Réessayer") }
                        }
                    }
                    gammes.isNotEmpty() -> {
                        val visibles = gammes.filter { gammesSelectionnees.contains(it.codeTreillis) }
                        GammeGrid("GAMME ACTUELLE", visibles, current, viewModel::selectCurrentGamme, modifier = Modifier.weight(1f))
                        Spacer(Modifier.height(16.dp))
                        GammeGrid("GAMME VISÉE", visibles, desired, viewModel::selectDesiredGamme, restrict = current, modifier = Modifier.weight(1f))
                        Spacer(Modifier.height(24.dp))
                    }
                    else -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Aucune gamme trouvée.", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Détails en bas avec logo à côté
                DetailsCard("Gamme actuelle", current)
                DetailsCard("Gamme visée", desired)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retour")
                    }
                    if (role == "ADMIN") {
                        ElevatedButton(
                            onClick = { navController.navigate(Routes.TypeOperationParametres) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Paramètres")
                        }
                    }
                    ElevatedButton(
                        onClick = {
                            viewModel.validateGammeChange { _, msg -> scope.launch { snackbarHost.showSnackbar(msg) } }
                        },
                        enabled = current != null && desired != null,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Valider")
                    }
                }

                Footer(zone, intervention)
            }
            SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun DetailsCard(title: String, gamme: Gamme?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                gamme?.let {
                    Text("Désignation : ${it.designation.safeText()}")
                    Text("Maille : ${it.dimension} mm")
                    Text("Chaîne/Trame : ${it.diamChaineTrame}")
                    Text("Esp. fil/chaîne : ${it.espFilChaineTrame} mm")
                } ?: Text("Aucune sélection")
            }
            getImageForGamme(gamme?.designation ?: "")?.let { res ->
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = painterResource(res),
                    contentDescription = "Logo $title",
                    modifier = Modifier.size(200.dp)
                )
            }
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
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(gammes) { gamme ->
                val disabled = restrict != null && gamme == restrict
                val borderColor by animateColorAsState(
                    when {
                        disabled -> Color.LightGray
                        gamme == selected -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    }, animationSpec = tween(500, easing = FastOutSlowInEasing)
                )
                val bgColor by animateColorAsState(
                    when {
                        disabled -> Color(0xFF2E2E2E)
                        gamme == selected -> MaterialTheme.colorScheme.primary.copy(alpha = .1f)
                        else -> Color(0xFF1E1E1E)
                    }, animationSpec = tween(500)
                )
                val txtColor = when {
                    disabled -> Color.LightGray
                    gamme == selected -> MaterialTheme.colorScheme.primary
                    else -> Color.White
                }
                val fw = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
                val scale by animateFloatAsState(
                    targetValue = if (gamme == selected) 1.05f else 1f,
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
                        gamme.designation.safeText(),
                        color = txtColor,
                        fontWeight = fw,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
        Text("Zone : $zone  |  Interv. : $intervention", style = MaterialTheme.typography.bodySmall)
        Text(now, style = MaterialTheme.typography.bodySmall)
    }
}
