// file: app/src/main/java/com/riva/atsmobile/ui/screens/EtapesScreen.kt
package com.riva.atsmobile.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.logic.StepFilterManager
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.utils.SessionManager
import com.riva.atsmobile.viewmodel.EtapeViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Vérification des conditions personnalisées (ET / OU / exclusions)
private fun checkCustomConditions(
    conditionString: String?,
    allEtapes: Map<Int, Etape>,
    excludedIds: List<Int>
): Boolean {
    if (conditionString.isNullOrBlank()) return true
    return conditionString.split('+').all { group ->
        val trimmed = group.trim()
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            // OR group
            val ids = trimmed
                .removeSurrounding("(", ")")
                .split("ou")
                .mapNotNull { it.trim().toIntOrNull() }
            val valid = ids.filterNot { it in excludedIds }
            valid.isEmpty() || valid.any { allEtapes[it]?.etat_Etape == "VALIDE" }
        } else {
            // Single ID
            val id = trimmed.toIntOrNull()
            id == null || id in excludedIds || allEtapes[id]?.etat_Etape == "VALIDE"
        }
    }
}

@Composable
fun EtapesScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Chargement de la session si existante
    val savedSession = SessionManager.loadSession(context)

    LaunchedEffect(Unit) {
        StepFilterManager.init(context)
        etapeViewModel.loadEtapes(context)
    }

    val etapes by etapeViewModel.etapes.collectAsState()
    val currentGamme by selectionViewModel.currentGamme.collectAsState()
    val desiredGamme by selectionViewModel.desiredGamme.collectAsState()
    val nbFilsActuel by selectionViewModel.nbFilsActuelFlow.collectAsState()
    val nbFilsVise by selectionViewModel.nbFilsViseFlow.collectAsState()
    val zone by selectionViewModel.zoneDeTravail.collectAsState()
    val intervention by selectionViewModel.intervention.collectAsState()
    val isAdmin by selectionViewModel.isAdmin.collectAsState()

    var idsToExclude by remember { mutableStateOf(emptyList<Int>()) }
    LaunchedEffect(nbFilsActuel, nbFilsVise) {
        idsToExclude = StepFilterManager.getExcludedSteps(nbFilsActuel, nbFilsVise)
        Log.d("EtapesScreen", "Exclusions: $idsToExclude")
    }

    val etapesFiltres = remember(etapes, idsToExclude) { etapes.filter { it.id_Etape !in idsToExclude } }
    val etapesTriees = remember(etapesFiltres) { getOrderedSteps(etapesFiltres) }

    // Gestion du Back Android
    BackHandler {
        scope.launch {
            if (confirmReset(context)) {
                etapeViewModel.resetSession(context)
                SessionManager.clearSession(context)
                navController.popBackStack()
            }
        }
    }

    BaseScreen(
        title = "Étapes de changement",
        navController = navController,
        viewModel = selectionViewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = true,
        onBack = {
            scope.launch {
                if (confirmReset(context)) {
                    etapeViewModel.resetSession(context)
                    SessionManager.clearSession(context)
                    navController.popBackStack()
                }
            }
        }
    ) { padding ->
        if (etapesTriees.isEmpty()) {
            Text(
                "Aucune étape trouvée.",
                color = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .padding(padding)
            )
            return@BaseScreen
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // En-tête avec gammes et nombre de fils
            StepHeader(currentGamme, desiredGamme, nbFilsActuel, nbFilsVise)

            // Section Soudeuse
            MachineSection(
                title = "Soudeuse",
                etapes = etapesTriees.filter { it.affectation_Etape.contains("operateur_soudeuse") },
                savedSession = savedSession,
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                cardColor = Color(0xFF263238)
            )

            // Section Tréfileuse T1
            MachineSection(
                title = "Tréfileuse T1",
                etapes = etapesTriees.filter { it.affectation_Etape.contains("operateur_t1") },
                savedSession = savedSession,
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                cardColor = Color(0xFF1E272E)
            )

            // Section Tréfileuse T2
            MachineSection(
                title = "Tréfileuse T2",
                etapes = etapesTriees.filter { it.affectation_Etape.contains("operateur_t2") },
                savedSession = savedSession,
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                cardColor = Color(0xFF2C3E50)
            )
        }
    }
}

@Composable
private fun StepHeader(
    current: Gamme?, desired: Gamme?, nbAct: Int?, nbVis: Int?
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Gamme Actuelle : ${current?.designation ?: "-"} (${nbAct ?: "-"})",
            color = Color.LightGray,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Gamme Visée : ${desired?.designation ?: "-"} (${nbVis ?: "-"})",
            color = Color.LightGray,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun MachineSection(
    title: String,
    etapes: List<Etape>,
    savedSession: SessionManager.SessionData?,
    currentGamme: Gamme?,
    desiredGamme: Gamme?,
    zone: String,
    intervention: String,
    etapeViewModel: EtapeViewModel,
    context: Context,
    isAdmin: Boolean,
    excludedIds: List<Int>,
    cardColor: Color
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val resumeIndex = savedSession
        ?.takeIf { it.current == currentGamme && it.desired == desiredGamme }
        ?.stepIndex ?: 0

    ExpandableCard(
        title = title,
        expanded = expanded,
        onToggle = { expanded = !expanded }
    ) {
        EtapeCardGroup(
            title = title,
            etapes = etapes,
            resumeIndex = resumeIndex,
            currentGamme = currentGamme,
            desiredGamme = desiredGamme,
            zone = zone,
            intervention = intervention,
            etapeViewModel = etapeViewModel,
            context = context,
            cardColor = cardColor,
            isAdmin = isAdmin,
            allEtapes = etapes,
            excludedIds = excludedIds
        )
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp)
        )
        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun EtapeCardGroup(
    title: String,
    etapes: List<Etape>,
    resumeIndex: Int,
    currentGamme: Gamme?,
    desiredGamme: Gamme?,
    zone: String,
    intervention: String,
    etapeViewModel: EtapeViewModel,
    context: Context,
    cardColor: Color,
    isAdmin: Boolean,
    allEtapes: List<Etape>,
    excludedIds: List<Int>
) {
    var currentIndex by rememberSaveable { mutableStateOf(resumeIndex) }
    if (currentIndex >= etapes.size) currentIndex = etapes.lastIndex
    val etape = etapes.getOrNull(currentIndex) ?: return

    // Sauvegarde automatique de la progression
    DisposableEffect(currentIndex) {
        if (currentGamme != null && desiredGamme != null) {
            SessionManager.saveSession(
                context,
                SessionManager.SessionData(
                    current = currentGamme,
                    desired = desiredGamme,
                    zone = zone,
                    intervention = intervention,
                    stepIndex = currentIndex
                )
            )
        }
        onDispose { }
    }

    // Timer et animations
    var startTime by rememberSaveable(etape.id_Etape) { mutableStateOf(System.currentTimeMillis()) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor)

    // Description / Commentaire
    var description by rememberSaveable(etape.id_Etape) { mutableStateOf(etape.description_Etape.orEmpty()) }
    var commentaire by rememberSaveable(etape.id_Etape) { mutableStateOf(etape.commentaire_Etape_1.orEmpty()) }

    // État validé
    var isValidated by remember(etape.etat_Etape) { mutableStateOf(etape.etat_Etape == "VALIDE") }
    LaunchedEffect(etape.etat_Etape) { isValidated = etape.etat_Etape == "VALIDE" }

    // Conditions préalables
    val arePreconditionsMet = remember(etape, allEtapes, excludedIds) {
        val map = allEtapes.associateBy { it.id_Etape }
        checkCustomConditions(etape.conditions_A_Valider, map, excludedIds)
    }

    Card(
        Modifier
            .fillMaxWidth()
            .background(animatedBgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Titre et navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$title – Étape ${currentIndex + 1} / ${etapes.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isAdmin) {
                    Text(
                        text = "ID: ${etape.id_Etape}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // Libellé
            Text(
                etape.libelle_Etape,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            // Conditions à valider
            if (!etape.conditions_A_Valider.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Conditions à valider :", color = Color.White, style = MaterialTheme.typography.titleSmall)

                val map = allEtapes.associateBy { it.id_Etape }
                val condString = etape.conditions_A_Valider
                    .replace("+", " ET ")
                    .replace("ou", " OU ")
                    .replace("(", "( ")
                    .replace(")", " )")
                val parts = condString.split(" ")

                val styled = buildAnnotatedString {
                    parts.forEach { part ->
                        val id = part.toIntOrNull()
                        if (id != null) {
                            val dep = map[id]
                            val excluded = id in excludedIds
                            val color = when {
                                excluded -> Color.Gray
                                dep?.etat_Etape == "VALIDE" -> Color(0xFF4CAF50)
                                else -> Color(0xFFF44336)
                            }
                            val status = if (excluded) "👻" else if (dep?.etat_Etape == "VALIDE") "✅" else "❌"
                            withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                                append("$id$status ")
                            }
                        } else {
                            val col = if (part in listOf("ET", "OU")) Color.Yellow else Color.LightGray
                            withStyle(style = SpanStyle(color = col)) { append("$part ") }
                        }
                    }
                }
                Text(styled, style = MaterialTheme.typography.bodySmall)
            }

            // Description (admin) et commentaire (opérateur)
            if (!etape.description_Etape.isNullOrBlank() || isAdmin) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    enabled = isAdmin,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White)
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = commentaire,
                onValueChange = { commentaire = it },
                label = { Text("Commentaire") },
                enabled = !isValidated,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White)
            )

            Spacer(Modifier.height(12.dp))
            // Statut
            Text(
                text = when {
                    isValidated -> "✅ Validée"
                    !arePreconditionsMet -> "❌ Conditions non remplies"
                    else -> "⏳ En attente"
                },
                color = when {
                    isValidated -> Color.Green
                    !arePreconditionsMet -> Color.Red
                    else -> Color.Yellow
                }
            )

            Spacer(Modifier.height(20.dp))
            // Boutons Précédent / Valider / Suivant
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("Précédent") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!isValidated && !arePreconditionsMet) {
                            Toast.makeText(context, "Validez d'abord les prérequis.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        val comTrim = commentaire.trim()
                        val descTrim = description.trim()
                        if (isValidated) {
                            etapeViewModel.devaliderEtape(
                                context, etape.id_Etape, comTrim, descTrim, 0
                            ) { success, msg ->
                                if (success) bgColor = Color(0x33FFFF00)
                                else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            }
                        } else {
                            etapeViewModel.validerEtape(
                                context, etape.id_Etape, comTrim, descTrim, elapsed
                            ) { success, msg ->
                                if (success) {
                                    bgColor = Color(0x3300FF00)
                                    if (currentIndex < etapes.lastIndex) currentIndex++
                                    else Toast.makeText(context, "Dernière étape validée !", Toast.LENGTH_SHORT).show()
                                } else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            }
                        }
                    },
                    enabled = isValidated || arePreconditionsMet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isValidated) "🔄 Annuler" else "✅ Valider")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { if (currentIndex < etapes.lastIndex) currentIndex++ },
                    enabled = currentIndex < etapes.lastIndex,
                    modifier = Modifier.weight(1f)
                ) { Text("Suivant") }
            }
        }
    }
}

// Dialogue de confirmation (suspend)
suspend fun showConfirmationDialog(context: Context, message: String): Boolean =
    suspendCancellableCoroutine { cont ->
        AlertDialog.Builder(context)
            .setTitle("Attention")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Oui") { _, _ -> cont.resume(true) }
            .setNegativeButton("Non") { _, _ -> cont.resume(false) }
            .show()
    }

// Confirmation pour réinitialiser la session
private suspend fun confirmReset(context: Context): Boolean =
    suspendCancellableCoroutine { cont ->
        AlertDialog.Builder(context)
            .setTitle("Attention")
            .setMessage("Voulez-vous vraiment quitter ? Cela réinitialisera la session.")
            .setCancelable(false)
            .setPositiveButton("Oui") { _, _ -> cont.resume(true) }
            .setNegativeButton("Non") { _, _ -> cont.resume(false) }
            .show()
    }

// Tri topologique des étapes selon leurs prédécesseurs
private fun getOrderedSteps(etapes: List<Etape>): List<Etape> {
    val map = etapes.associateBy { it.id_Etape }
    val visited = mutableSetOf<Int>()
    val out = mutableListOf<Etape>()
    fun dfs(e: Etape) {
        if (!visited.add(e.id_Etape)) return
        e.predecesseurs
            .flatMap { it.ids }
            .filter { it != 0 }
            .mapNotNull { map[it] }
            .forEach { dfs(it) }
        if (e !in out) out += e
    }
    etapes.forEach { dfs(it) }
    return out

}
