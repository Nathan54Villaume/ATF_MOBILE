// file: app/src/main/java/com/riva/atsmobile/ui/screens/SelectionGammes.kt
package com.riva.atsmobile.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
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

// Trim or fallback
fun String?.safeText(): String =
    this?.trim().takeIf { !it.isNullOrEmpty() } ?: "-"

// Logos principal + chaines
data class GammeLogos(
    val principale: Int?,
    val chaines: Int?,
    val dimension: Int?,
    val diametre: Int?
)

// Associe désignation → logos
fun getImageForGamme(gamme: Gamme?): GammeLogos {
    val key = gamme?.designation?.trim()?.uppercase() ?: ""
    return GammeLogos(
        principale = when (key) {
            "PAF 10" -> R.drawable.paf10
            "PAF C" -> R.drawable.pafc
            "PAF R" -> R.drawable.pafr
            "PAF V" -> R.drawable.pafv
            "ST 15 C" -> R.drawable.st15c
            "ST 20" -> R.drawable.st20
            "ST 25" -> R.drawable.st25
            "ST 25 C" -> R.drawable.st25c
            else -> null
        },
        chaines = when (key) {
            "ST 20", "ST 25", "ST 25 C" -> R.drawable.chaines16
            "PAF 10", "PAF C", "PAF R", "PAF V", "ST 15 C" -> R.drawable.chaines12
            else -> null
        },
        dimension = when (gamme?.dimension?.trim() ?: "") {
            "4200x2400" -> R.drawable.us4200x2400
            "6000x2400" -> R.drawable.us6000x2400
            "3600x2400" -> R.drawable.us3600x2400
            "3200x2400" -> R.drawable.us3200x2400
            "4000x2400" -> R.drawable.us4000x2400
            else -> null
        },
        diametre = when (gamme?.diamChaineTrame?.trim() ?: "") {
            "6x6" -> R.drawable.u6x6
            "7x7" -> R.drawable.u7x7
            "6x7" -> R.drawable.u6x7
            "5,5x5,5", "5.5x5.5" -> R.drawable.u55x55
            "4,5x4,5", "4.5x4.5" -> R.drawable.u45x45
            else -> null
        }
    )
}

@Composable
fun TransitionArrow(
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    height: Dp = 60.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetFloat by infiniteTransition.animateFloat(
        0f, 16f,
        infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val offset = offsetFloat.dp

    Icon(
        imageVector = Icons.Default.ArrowDownward,
        contentDescription = null,
        modifier = modifier
            .offset(y = offset)
            .size(width, height)
            .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
            .padding(4.dp)
    )
}

@Composable
fun DetailsCard(
    gamme: Gamme?,
    detailStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.SansSerif
    ),
    noSelectionStyle: TextStyle = TextStyle(
        color = Color.Gray,
        fontSize = 16.sp,
        fontStyle = FontStyle.Italic,
        fontFamily = FontFamily.SansSerif
    )
) {
    val logos = getImageForGamme(gamme)
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val mainSize = if (isPortrait) 150.dp else 140.dp
    val secondarySize = if (isPortrait) 120.dp else 90.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                gamme?.let {
                    Text(" ${it.designation.safeText()}", style = detailStyle)
                    Text(" ${it.dimension} mm", style = detailStyle)
                    Text(" ${it.diamChaine} mm x ${it.diamTrame} mm", style = detailStyle)
                    Text(" ${it.espFilChaineTrame} mm", style = detailStyle)
                } ?: Text("Aucune sélection", style = noSelectionStyle)
            }
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                logos.principale?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(mainSize)) }
                logos.chaines?.let    { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(secondarySize)) }
                logos.dimension?.let  { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(secondarySize)) }
                logos.diametre?.let   { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(secondarySize)) }
            }
        }
    }
}

@Composable
fun SelectionColumn(
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (loadError != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(loadError, color = Color.Red)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { scope.launch { viewModel.chargerGammesDepuisApi(context) } }) {
                    Text("Réessayer")
                }
            }
        } else {
            val visibles = gammes.filter { selectedCodes.contains(it.codeTreillis) }
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    "Sélectionnez vos gammes",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
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
fun DetailsColumn(
    current: Gamme?,
    desired: Gamme?,
    role: String,
    navController: NavController,
    viewModel: SelectionViewModel,
    snackbarHost: SnackbarHostState,
    zone: String,
    intervention: String
) {
    var topY by remember { mutableStateOf(0f) }
    var topHeight by remember { mutableStateOf(0f) }
    var bottomY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val arrowOffset by remember(topY, topHeight, bottomY) {
        derivedStateOf {
            val midPx = (topY + topHeight + bottomY) / 2f
            with(density) { midPx.toDp() - 1.dp }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Gamme actuelle
            Column(
                modifier = Modifier.onGloballyPositioned {
                    topY = it.positionInParent().y
                    topHeight = it.size.height.toFloat()
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GAMME ACTUELLE",
                    style = TextStyle(
                        color = Color(0xFFFF9800),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
                Spacer(Modifier.height(8.dp))
                DetailsCard(
                    gamme = current,
                    detailStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 20.sp),
                    noSelectionStyle = TextStyle(color = Color.Red, fontSize = 20.sp, fontStyle = FontStyle.Italic)
                )
            }
            Spacer(Modifier.height(24.dp))
            // Gamme visée
            Column(
                modifier = Modifier.onGloballyPositioned { bottomY = it.positionInParent().y },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GAMME VISÉE",
                    style = TextStyle(
                        color = Color(0xFFFF9800),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                )
                Spacer(Modifier.height(8.dp))
                DetailsCard(
                    gamme = desired,
                    detailStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 20.sp),
                    noSelectionStyle = TextStyle(color = Color.Red, fontSize = 20.sp, fontStyle = FontStyle.Italic)
                )
            }
            Spacer(Modifier.height(32.dp))
            // Action row moved down to always show "Valider"
        }
    }
}

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // make scrollable
                .padding(padding)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selection
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
                Spacer(Modifier.height(32.dp))
                // Details
                DetailsColumn(
                    current = current,
                    desired = desired,
                    role = role,
                    navController = navController,
                    viewModel = viewModel,
                    snackbarHost = snackbarHost,
                    zone = zone,
                    intervention = intervention
                )
            }

            // Always-visible "Valider" button
            val context = LocalContext.current

            ActionRow(
                context = context,
                current = current,
                desired = desired,
                role = role,
                navController = navController,
                viewModel = viewModel,
                snackbarHost = snackbarHost,
                zone = zone,
                intervention = intervention,
                scope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )


            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun ActionRow(
    current: Gamme?, desired: Gamme?, role: String, navController: NavController,
    viewModel: SelectionViewModel, snackbarHost: SnackbarHostState, zone: String,
    intervention: String, scope: CoroutineScope, context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val openDialog = remember { mutableStateOf(false) }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    openDialog.value = false
                    scope.launch {
                        val success = viewModel.demarrerNouvelleSession(context)
                        if (success) {
                            viewModel.sauvegarderSessionLocalement(context)
                            navController.currentBackStackEntry?.savedStateHandle?.set("currentDesignation", current?.designation.orEmpty())
                            navController.currentBackStackEntry?.savedStateHandle?.set("desiredDesignation", desired?.designation.orEmpty())
                            navController.navigate(Routes.StepWizard)
                        } else {
                            snackbarHost.showSnackbar("Erreur lors du démarrage de session")
                        }
                    }
                }) { Text("Continuer") }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) {
                    Text("Annuler")
                }
            },
            title = { Text("Démarrer une session ?") },
            text = { Text("Une session va commencer. Vous ne pourrez pas revenir en arrière sans perdre la progression.") }
        )
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        ElevatedButton(
            onClick = { openDialog.value = true },
            enabled = current != null && desired != null,
            modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 56.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Valider", fontSize = 18.sp)
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
    modifier: Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    cardShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedBgColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    defaultBgColor: Color = Color(0xFF1E1E1E),
    disabledBgColor: Color = Color(0xFF2E2E2E),
    disabledColor: Color = Color.LightGray,
    defaultColor: Color = Color.White,
    cardPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
) {
    Column {
        Text(title, style = titleStyle)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gammes) { gamme ->
                val disabled = restrict != null && gamme == restrict
                val borderColor by animateColorAsState(
                    when {
                        disabled       -> disabledColor
                        gamme == selected -> selectedColor
                        else           -> Color.Gray
                    },
                    tween(500)
                )
                val bgColor by animateColorAsState(
                    when {
                        disabled       -> disabledBgColor
                        gamme == selected -> selectedBgColor
                        else           -> defaultBgColor
                    },
                    tween(500)
                )
                val textColor = when {
                    disabled       -> disabledColor
                    gamme == selected -> selectedColor
                    else           -> defaultColor
                }
                val fontWeight = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
                val scale by animateFloatAsState(if (gamme == selected) 1.05f else 1f, tween(300))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(scale)
                        .background(bgColor, cardShape)
                        .border(BorderStroke(2.dp, borderColor), cardShape)
                        .clickable(enabled = !disabled) { onSelect(gamme) }
                        .padding(cardPadding)
                ) {
                    Text(
                        text = gamme.designation.safeText(),
                        color = textColor,
                        fontWeight = fontWeight,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
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
