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
import androidx.compose.runtime.snapshots.SnapshotStateList
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

/* ------------ Normalisation / rôles ------------ */

private fun normRole(s: String): String =
    s.trim().lowercase().replace(Regex("[^a-z0-9]"), "") // enlève _ - espaces etc.

private fun rolesOfRaw(etape: Etape): List<String> =
    etape.affectation_Etape
        .split(';', ',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private fun rolesOfNorm(etape: Etape): List<String> =
    rolesOfRaw(etape).map(::normRole)

private fun roleMapNorm(etape: Etape?): Map<String, String> =
    (etape?.etatParRole ?: emptyMap()).entries.associate { normRole(it.key) to it.value }

/**
 * Étape entièrement validée :
 *  - Si UNE SEULE affectation : "VALIDE" serveur OU validation locale (optimiste).
 *  - Si PLUSIEURS affectations : TOUS les rôles doivent être "VALIDE" côté serveur.
 */
private fun isStepFullyValidated(etape: Etape?, locallyValidatedIds: Set<Int>): Boolean {
    if (etape == null) return false
    val roles = rolesOfNorm(etape)
    val map = roleMapNorm(etape)
    return if (roles.size <= 1) {
        (roles.firstOrNull()?.let { r -> map[r] == "VALIDE" } == true) ||
                locallyValidatedIds.contains(etape.id_Etape)
    } else {
        roles.all { r -> map[r] == "VALIDE" }
    }
}

/** Détail par rôle pour affichage (ex: operateur_t1:✅ operateur_t2:❌). */
private fun roleBadgesFor(etape: Etape?, locallyValidatedIds: Set<Int>): String {
    if (etape == null) return "?"
    val rolesRaw = rolesOfRaw(etape)
    val rolesN = rolesOfNorm(etape)
    val mapN = roleMapNorm(etape)
    val badges = rolesRaw.indices.joinToString(" ") { i ->
        val keyN = rolesN[i]
        val ok = mapN[keyN] == "VALIDE"
        val symb = if (ok) "✅" else "❌"
        "${rolesRaw[i]}:$symb"
    }
    return if (rolesN.size <= 1 && locallyValidatedIds.contains(etape.id_Etape)) "$badges (local)" else badges
}

/* ------------ Snapshot validations & parsing conditions ------------ */

/** Construit l’ensemble des IDs d’étapes considérées “entièrement validées” à l’instant t. */
private fun computeFullyValidatedIds(
    allEtapes: List<Etape>,
    locallyValidatedIds: Set<Int>
): Set<Int> {
    val out = mutableSetOf<Int>()
    allEtapes.forEach { e ->
        val roles = rolesOfNorm(e)
        val map = roleMapNorm(e)
        val fully = if (roles.size <= 1) {
            (roles.firstOrNull()?.let { r -> map[r] == "VALIDE" } == true) ||
                    (e.id_Etape in locallyValidatedIds)
        } else {
            roles.all { r -> map[r] == "VALIDE" }
        }
        if (fully) out += e.id_Etape
    }
    return out
}

/** Teste les conditions à partir d’un Set d’IDs déjà validés. '+' = OU global, 'ou' intra-parenthèses = OU. */
private fun areConditionsMet(
    cond: String?,
    fullyValidatedIds: Set<Int>,
    excludedIds: Set<Int>
): Boolean {
    if (cond.isNullOrBlank()) return true
    val parts = cond.split('+').map { it.trim() }
    fun idOk(id: Int): Boolean = id in excludedIds || id in fullyValidatedIds
    fun partOk(p: String): Boolean {
        return if (p.startsWith("(") && p.endsWith(")")) {
            val ids = p.removeSurrounding("(", ")").split("ou").mapNotNull { it.trim().toIntOrNull() }
            ids.any { idOk(it) } || ids.isEmpty()
        } else {
            p.toIntOrNull()?.let { idOk(it) } ?: true
        }
    }
    return parts.any { partOk(it) }
}

/** Pour l’affichage des manquants. */
private fun extractAllIds(cond: String?): List<Int> =
    cond?.replace("(", "")?.replace(")", "")?.replace("ou", "+")
        ?.split('+')?.mapNotNull { it.trim().toIntOrNull() }?.distinct()
        ?: emptyList()

/* ------------ Écran ------------ */

@Composable
fun EtapesScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    val locallyValidatedIds = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
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
        connectionStatus = true
    ) { padding ->
        if (etapesTriees.isEmpty()) {
            Text(
                "Aucune étape trouvée.",
                color = Color.White,
                modifier = Modifier.fillMaxSize().padding(32.dp).padding(padding)
            )
            return@BaseScreen
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StepHeader(currentGamme, desiredGamme, nbFilsActuel, nbFilsVise)

            MachineSection(
                title = "Soudeuse",
                operateur = "operateur_soudeuse",
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
                allEtapes = etapesTriees,
                cardColor = Color(0xFF263238),
                locallyValidatedIds = locallyValidatedIds
            )
            MachineSection(
                title = "Tréfileuse T1",
                operateur = "operateur_t1",
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
                allEtapes = etapesTriees,
                cardColor = Color(0xFF1E272E),
                locallyValidatedIds = locallyValidatedIds
            )
            MachineSection(
                title = "Tréfileuse T2",
                operateur = "operateur_t2",
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
                allEtapes = etapesTriees,
                cardColor = Color(0xFF2C3E50),
                locallyValidatedIds = locallyValidatedIds
            )
        }
    }
}

@Composable
private fun StepHeader(current: Gamme?, desired: Gamme?, nbAct: Int?, nbVis: Int?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Gamme Actuelle : ${current?.designation ?: "-"} ($nbAct)", color = Color.LightGray, style = MaterialTheme.typography.titleLarge)
        Text(text = "Gamme Visée : ${desired?.designation ?: "-"} (${nbVis ?: "-"})", color = Color.LightGray, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun MachineSection(
    title: String,
    operateur: String,
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
    allEtapes: List<Etape>,
    cardColor: Color,
    locallyValidatedIds: SnapshotStateList<Int>
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val resumeIndex = savedSession?.takeIf { it.current == currentGamme && it.desired == desiredGamme }?.stepIndex ?: 0

    ExpandableCard(title = title, expanded = expanded, onToggle = { expanded = !expanded }) {
        EtapeCardGroup(
            operateur = operateur,
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
            allEtapes = allEtapes,
            excludedIds = excludedIds,
            locallyValidatedIds = locallyValidatedIds
        )
    }
}

@Composable
private fun EtapeCardGroup(
    operateur: String,
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
    excludedIds: List<Int>,
    locallyValidatedIds: SnapshotStateList<Int>
) {
    var currentIndex by rememberSaveable { mutableStateOf(resumeIndex) }
    if (currentIndex >= etapes.size) currentIndex = etapes.lastIndex
    val etape = etapes.getOrNull(currentIndex) ?: return

    DisposableEffect(currentIndex) {
        if (currentGamme != null && desiredGamme != null) {
            SessionManager.saveSession(
                context,
                SessionManager.SessionData(
                    current = currentGamme, desired = desiredGamme,
                    zone = zone, intervention = intervention, stepIndex = currentIndex
                )
            )
        }
        onDispose { }
    }

    var startTime by rememberSaveable(etape.id_Etape) { mutableStateOf(System.currentTimeMillis()) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor)

    var description by rememberSaveable(etape.id_Etape) { mutableStateOf(etape.description_Etape.orEmpty()) }
    var commentaire by rememberSaveable(etape.id_Etape) { mutableStateOf(etape.commentaire_Etape_1.orEmpty()) }

    val mapNorm = roleMapNorm(etape)
    val currentRoleValidated = mapNorm[normRole(operateur)] == "VALIDE"
    val hasCurrentRoleValidated =
        currentRoleValidated || locallyValidatedIds.contains(etape.id_Etape)

    val allRolesValidated =
        isStepFullyValidated(etape, locallyValidatedIds.toSet())

    // === Snapshot des étapes entièrement validées (rôles normalisés) ===
    val fullyValidatedIds = remember(allEtapes, locallyValidatedIds.toList()) {
        computeFullyValidatedIds(allEtapes, locallyValidatedIds.toSet())
    }

    val arePreconditionsMet = areConditionsMet(
        etape.conditions_A_Valider,
        fullyValidatedIds,
        excludedIds.toSet()
    )

    // IDs manquants pour affichage
    val missingIds = remember(etape.conditions_A_Valider, fullyValidatedIds, excludedIds) {
        extractAllIds(etape.conditions_A_Valider)
            .filter { it !in excludedIds && it !in fullyValidatedIds }
    }

    val allMap = remember(allEtapes) { allEtapes.associateBy { it.id_Etape } }

    key(etape.id_Etape) {
        Card(
            Modifier.fillMaxWidth().background(animatedBgColor),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$title – Étape ${currentIndex + 1} / ${etapes.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    if (isAdmin) Text("ID: ${etape.id_Etape}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(Modifier.height(8.dp))
                Text(etape.libelle_Etape, color = Color.White, style = MaterialTheme.typography.titleLarge)

                if (!etape.conditions_A_Valider.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Conditions à valider :", color = Color.White, style = MaterialTheme.typography.titleSmall)

                    val condString = etape.conditions_A_Valider
                        .replace("+", " OU ").replace("ou", " OU ").replace("(", "( ").replace(")", " )")
                    val parts = condString.split(" ")

                    val styled = buildAnnotatedString {
                        parts.forEach { part ->
                            val id = part.toIntOrNull()
                            if (id != null) {
                                val excluded = id in excludedIds
                                val fullyValid = !excluded && (id in fullyValidatedIds)
                                val color = when {
                                    excluded   -> Color.Gray
                                    fullyValid -> Color(0xFF4CAF50) // ✅
                                    else       -> Color(0xFFF44336) // ❌
                                }
                                val status = when {
                                    excluded   -> "👻"
                                    fullyValid -> "✅"
                                    else       -> "❌"
                                }
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

                    // Détail par rôle pour chaque ID unique
                    val idsInCond: List<Int> = extractAllIds(etape.conditions_A_Valider)
                    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        idsInCond.forEach { id ->
                            val excluded = id in excludedIds
                            val dep = allMap[id]
                            val badges = if (excluded) "👻 exclue" else roleBadgesFor(dep, locallyValidatedIds.toSet())
                            Text(
                                text = "• $id • $badges",
                                color = if (excluded) Color.Gray else Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (!etape.description_Etape.isNullOrBlank() || isAdmin) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description") }, enabled = isAdmin,
                        modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White)
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = commentaire, onValueChange = { commentaire = it },
                    label = { Text("Commentaire") }, enabled = !hasCurrentRoleValidated,
                    modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White)
                )

                Spacer(Modifier.height(12.dp))
                // Statut synthétique
                val rolesCount = rolesOfNorm(etape).size
                val statusText = when {
                    allRolesValidated -> "✅ Validée (tous rôles)"
                    hasCurrentRoleValidated -> if (rolesCount <= 1) "✅ Validée" else "✅ Validée (mon rôle)"
                    !arePreconditionsMet -> "❌ Conditions non remplies"
                    else -> "⏳ En attente"
                }
                val statusColor = when {
                    allRolesValidated -> Color.Green
                    hasCurrentRoleValidated -> Color(0xFF8BC34A)
                    !arePreconditionsMet -> Color.Red
                    else -> Color.Yellow
                }
                Text(
                    text = statusText + if (!arePreconditionsMet && missingIds.isNotEmpty())
                        "  |  Manquantes: ${missingIds.joinToString(", ")}" else "",
                    color = statusColor
                )

                // Boutons
                val canValidate = !hasCurrentRoleValidated && arePreconditionsMet
                val canUnvalidate = hasCurrentRoleValidated

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("Précédent") }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                            val comTrim = commentaire.trim()
                            val descTrim = description.trim()

                            if (canUnvalidate) {
                                etapeViewModel.devaliderEtape(
                                    context, etape.id_Etape, comTrim, descTrim, 0, operateur
                                ) { success, msg ->
                                    if (success) {
                                        locallyValidatedIds.remove(etape.id_Etape)
                                        bgColor = Color(0x33FFFF00) // jaune léger
                                        etapeViewModel.loadEtapes(context)
                                    } else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                }
                            } else if (canValidate) {
                                val id = etape.id_Etape
                                val singleRole = rolesCount <= 1

                                // Optimiste : si une seule affectation, on marque localement tout de suite
                                if (singleRole && !locallyValidatedIds.contains(id)) {
                                    locallyValidatedIds.add(id)
                                }

                                etapeViewModel.validerEtape(
                                    context, id, comTrim, descTrim, elapsed, operateur = operateur
                                ) { success, msg ->
                                    if (success) {
                                        bgColor = Color(0x3300FF00) // vert léger
                                        etapeViewModel.loadEtapes(context)
                                    } else {
                                        if (singleRole) locallyValidatedIds.remove(id)
                                        msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                    }
                                }
                            }
                        },
                        enabled = canValidate || canUnvalidate, // Valider bloqué si prérequis KO. Suivant reste libre.
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                canUnvalidate -> "🔄 Annuler"
                                canValidate -> "✅ Valider"
                                else -> "Pré-requis manquants"
                            }
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (currentIndex < etapes.lastIndex) {
                                currentIndex++
                                startTime = System.currentTimeMillis()
                            }
                        },
                        enabled = currentIndex < etapes.lastIndex, // Navigation toujours permise
                        modifier = Modifier.weight(1f)
                    ) { Text("Suivant") }
                }
            }
        }
    }
}

/* ------------ Dialog & utils ------------ */

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

// Tri topologique des étapes selon leurs prédecesseurs (tous opérateurs confondus ici)
private fun getOrderedSteps(etapes: List<Etape>): List<Etape> {
    val map = etapes.associateBy { it.id_Etape }
    val visited = mutableSetOf<Int>()
    val out = mutableListOf<Etape>()
    fun dfs(e: Etape) {
        if (!visited.add(e.id_Etape)) return
        e.predecesseurs.flatMap { it.ids }.filter { it != 0 }.mapNotNull { map[it] }.forEach { dfs(it) }
        if (e !in out) out += e
    }
    etapes.forEach { dfs(it) }
    return out
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
