package com.riva.atsmobile.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.navigation.Routes
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String?.safeText(): String = this?.trim().takeIf { !it.isNullOrEmpty() } ?: "-"

data class GammeLogos(val principale: Int?, val chaines: Int?)

fun getImageForGamme(designation: String): GammeLogos {
    val key = designation.trim().uppercase()
    val principale = when (key) {
        "PAF 10" -> R.drawable.paf10
        "PAF C" -> R.drawable.pafc
        "PAF R" -> R.drawable.pafr
        "PAF V" -> R.drawable.pafv
        "ST 15 C" -> R.drawable.st15c
        "ST 20" -> R.drawable.st20
        "ST 25" -> R.drawable.st25
        "ST 25 C" -> R.drawable.st25c
        else -> null
    }
    val chaines = when (key) {
        "ST 20", "ST 25", "ST 25 C" -> R.drawable.chaines16
        "PAF 10", "PAF C", "PAF R", "PAF V", "ST 15 C" -> R.drawable.chaines12
        else -> null
    }
    return GammeLogos(principale, chaines)
}

@Composable
fun TransitionArrow(modifier: Modifier = Modifier, width: Dp = 60.dp, height: Dp = 60.dp) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetFloat by infiniteTransition.animateFloat(
        0f, 16f,
        infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
    )
    Icon(
        imageVector = Icons.Default.ArrowDownward,
        contentDescription = null,
        modifier = modifier
            .offset(y = offsetFloat.dp)
            .size(width, height)
            .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
            .padding(4.dp)
    )
}

@Composable
fun DetailsCard(title: String, gamme: Gamme?, modifier: Modifier = Modifier) {
    val logos = getImageForGamme(gamme?.designation ?: "")
    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            gamme?.let {
                Spacer(Modifier.height(8.dp))
                Text("Désignation : ${it.designation.safeText()}", color = Color.White)
                Text("Dimension : ${it.dimension} mm", color = Color.White)
                Text("Diamètres : ${it.diamChaineTrame}", color = Color.White)
                Text("Espacement : ${it.espFilChaineTrame} mm", color = Color.White)
            } ?: Text("Aucune sélection", color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))

            logos.principale?.let { Image(painterResource(it), null, Modifier.size(120.dp)) }
            logos.chaines?.let {
                Spacer(Modifier.height(8.dp))
                Image(painterResource(it), null, Modifier.size(80.dp))
            }
        }
    }
}

// Continue avec le reste de tes fonctions sans modifications
// (TypeOperationScreen, SelectionColumn, ActionRow, GammeGrid, Footer)

// Le reste du fichier étant inchangé et correct, tu peux reprendre exactement ce que tu avais précédemment.







@Composable
fun TypeOperationScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val isConnected by viewModel.isOnline.collectAsState()
    val gammes by viewModel.gammes.collectAsState()
    val gammesSelectionnees by viewModel.gammesSelectionnees.collectAsState()
    val current by viewModel.currentGamme.collectAsState()
    val desired by viewModel.desiredGamme.collectAsState()
    val zone by viewModel.zoneDeTravail.collectAsState()
    val intervention by viewModel.intervention.collectAsState()
    val role by viewModel.role.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        isLoading = true; loadError = null
        try { viewModel.chargerGammesDepuisApi(context) }
        catch (e: Exception) { loadError = "Erreur : ${e.message}" }
        finally { isLoading = false }
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
        ) {
            val selection = @Composable {
                SelectionColumn(
                    gammes = gammes,
                    selectedCodes = gammesSelectionnees,
                    current = current,
                    desired = desired,
                    isLoading = isLoading,
                    loadError = loadError,
                    viewModel = viewModel,
                    context = context,
                    scope = scope,
                    isPortrait = isPortrait
                )
            }

            val details = @Composable {
                // états pour récupérer les positions
                var topY by remember { mutableStateOf(0f) }
                var topHeight by remember { mutableStateOf(0f) }
                var bottomY by remember { mutableStateOf(0f) }
                val density = LocalDensity.current
                // calcul dynamique de la position de la flèche
                val arrowOffsetDp by remember(topY, topHeight, bottomY) {
                    derivedStateOf {
                        val midPx = (topY + topHeight + bottomY) / 2f
                        with(density) { midPx.toDp() - 1.dp } // centrer
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .onGloballyPositioned { coords ->
                                    topY = coords.positionInParent().y
                                    topHeight = coords.size.height.toFloat()
                                }
                        ) {
                            DetailsCard("Gamme actuelle", current)
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .onGloballyPositioned { coords ->
                                    bottomY = coords.positionInParent().y
                                }
                        ) {
                            DetailsCard("Gamme visée", desired)
                        }
                        ActionRow(current, desired, role, navController, viewModel, snackbarHost, zone, intervention, scope)
                        Footer(zone, intervention)
                    }

                    TransitionArrow(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = arrowOffsetDp)
                            .zIndex(1f),
                        width  = 80.dp,
                        height = 30.dp
                    )


                }
            }

            if (isPortrait) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { selection() }
                    Box(Modifier.weight(1f)) { details() }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { selection() }
                    Box(Modifier.weight(1f)) { details() }
                }
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}


@Composable
private fun SelectionColumn(
    gammes: List<Gamme>,
    selectedCodes: Set<String>,
    current: Gamme?,
    desired: Gamme?,
    isLoading: Boolean,
    loadError: String?,
    viewModel: SelectionViewModel,
    context: android.content.Context,
    scope: CoroutineScope,
    isPortrait: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Sélectionnez vos gammes",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (loadError != null) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(loadError!!, color = Color.Red)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { viewModel.chargerGammesDepuisApi(context) } }) {
                        Text("Réessayer")
                    }
                }
            }
        } else {
            val visibles = gammes.filter { selectedCodes.contains(it.codeTreillis) }
            item {
                GammeGrid(
                    title = "GAMME ACTUELLE",
                    gammes = visibles,
                    selected = current,
                    onSelect = viewModel::selectCurrentGamme,
                    restrict = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                )
            }
            item { }
            item {
                GammeGrid(
                    title = "GAMME VISÉE",
                    gammes = visibles,
                    selected = desired,
                    onSelect = viewModel::selectDesiredGamme,
                    restrict = current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    current: Gamme?,
    desired: Gamme?,
    role: String,
    navController: NavController,
    viewModel: SelectionViewModel,
    snackbarHost: SnackbarHostState,
    zone: String,
    intervention: String,
    scope: CoroutineScope
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ElevatedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp)
        ) {
            Icon(Icons.Default.WbSunny, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retour")
        }
        if (role == "ADMIN") {
            ElevatedButton(
                onClick = { navController.navigate(Routes.TypeOperationParametres) },
                modifier = Modifier.defaultMinSize(minWidth = 160.dp, minHeight = 56.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Paramètres")
            }
        }
        ElevatedButton(
            onClick = { viewModel.validateGammeChange { _, msg -> scope.launch { snackbarHost.showSnackbar(msg) } } },
            enabled = current != null && desired != null,
            modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Valider")
        }
    }
}

@Composable
fun DetailsCard(
    dimension: String,
    diametre: String,
    modifier: Modifier = Modifier
) {
    val dimensionImageResId = when (dimension) {
        "4200x2400" -> R.drawable.u4200x2400
        "6000x2400" -> R.drawable.u6000x2400
        "3600x2400" -> R.drawable.u3600x2400
        "3200x2400" -> R.drawable.u3200x2400
        else -> null
    }

    val diametreImageResId = when (diametre) {
        "6x6" -> R.drawable.u6x6
        "7x7" -> R.drawable.u7x7
        "6x7" -> R.drawable.u6x7
        "5,5x5,5" -> R.drawable.u55x55
        "4.5x4.5" -> R.drawable.u45x45
        else -> null
    }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            dimensionImageResId?.let { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Logo Dimension",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 8.dp)
                )
            }

            Text(
                text = "Dimension : $dimension mm",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            diametreImageResId?.let { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Logo Diamètre",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 8.dp)
                )
            }

            Text(
                text = "Diamètres : $diametre",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
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
    restrict: Gamme?,
    modifier: Modifier
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gammes) { gamme ->
                val disabled = restrict != null && gamme == restrict
                val borderColor by animateColorAsState(
                    when {
                        disabled -> Color.LightGray
                        gamme == selected -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    },
                    tween(500, easing = FastOutSlowInEasing)
                )
                val bgColor by animateColorAsState(
                    when {
                        disabled -> Color(0xFF2E2E2E)
                        gamme == selected -> MaterialTheme.colorScheme.primary.copy(alpha = .1f)
                        else -> Color(0xFF1E1E1E)
                    },
                    tween(500)
                )
                val txtColor = when {
                    disabled -> Color.LightGray
                    gamme == selected -> MaterialTheme.colorScheme.primary
                    else -> Color.White
                }
                val fw = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
                val scale by animateFloatAsState(
                    if (gamme == selected) 1.05f else 1f,
                    tween(300)
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
