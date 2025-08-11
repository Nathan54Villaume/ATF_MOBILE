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

/* ----------------- Normalisation des rôles ----------------- */

private fun normalizeRoleKey(raw: String): String {
    var s = raw.trim().lowercase()
    s = s.replace(" ", "_")
        .replace("-", "_")
    // operateur_t_1 / operateur t-1 / operateur_t1 -> operateur_t1
    s = s.replace(Regex("^operateur_t[_\\- ]?([0-9])$"), "operateur_t$1")
    // mecanicien-1 -> mecanicien_1
    s = s.replace(Regex("^mecanicien[_\\- ]?([0-9])$"), "mecanicien_$1")
    return s
}

private fun rolesOfNorm(etape: Etape): List<String> =
    etape.affectation_Etape
        .split(';', ',')
        .map { normalizeRoleKey(it) }
        .filter { it.isNotEmpty() }
        .distinct()

private fun roleMapNorm(etape: Etape): Map<String, String> =
    (etape.etatParRole ?: emptyMap()).mapKeys { (k, _) -> normalizeRoleKey(k) }

/* ----------------- Conditions personnalisées ----------------- */
/**
 * Interprétation:
 *  - '+' = OU (entre blocs)
 *  - 'ou' dans les parenthèses = OU interne (ex: "(29 ou 31)")
 *  - Un ID est “valide” s’il est dans fullyValidatedIds (ou bien exclu).
 */
private fun checkCustomConditions(
    conditionString: String?,
    fullyValidatedIds: Set<Int>,
    excludedIds: List<Int>
): Boolean {
    if (conditionString.isNullOrBlank()) return true
    val parts = conditionString.split('+').map { it.trim() }

    fun idOk(id: Int): Boolean {
        if (id in excludedIds) return true
        return id in fullyValidatedIds
    }

    fun partOk(part: String): Boolean {
        return if (part.startsWith("(") && part.endsWith(")")) {
            val inside = part.removeSurrounding("(", ")")
            val ids = inside.split("ou").mapNotNull { it.trim().toIntOrNull() }
            ids.any { idOk(it) } || ids.isEmpty()
        } else {
            val id = part.toIntOrNull()
            id == null || idOk(id)
        }
    }

    // '+' = OU global
    return parts.any { partOk(it) }
}

/* ----------------- Calcule les étapes entièrement validées ----------------- */

private fun computeFullyValidatedIds(
    allEtapes: List<Etape>,
    locallyValidatedByRole: Map<Int, Set<String>>
): Set<Int> {
    val out = mutableSetOf<Int>()
    for (e in allEtapes) {
        val roles = rolesOfNorm(e)
        val server = roleMapNorm(e)
        val local  = locallyValidatedByRole[e.id_Etape] ?: emptySet()

        val fully = if (roles.isEmpty()) {
            // Pas d'affectation -> on considère non bloquant
            true
        } else if (roles.size == 1) {
            // Mono-rôle : serveur OU cache local
            (server[roles[0]] == "VALIDE") || (roles[0] in local)
        } else {
            // Multi-rôles : chaque rôle doit être VALIDE (serveur OU local)
            roles.all { r -> (server[r] == "VALIDE") || (r in local) }
        }
        if (fully) out += e.id_Etape
    }
    return out
}

/* ----------------- Écran ----------------- */

@Composable
fun EtapesScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    val locallyValidatedIds = remember { mutableStateListOf<Int>() } // compat mono-rôle (peut rester)
    // NOUVEAU : validations locales par rôle (id_Etape -> set de rôles normalisés)
    val locallyValidatedByRole = remember { mutableStateMapOf<Int, MutableSet<String>>() }

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
                locallyValidatedIds = locallyValidatedIds,
                locallyValidatedByRole = locallyValidatedByRole
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
                locallyValidatedIds = locallyValidatedIds,
                locallyValidatedByRole = locallyValidatedByRole
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
                locallyValidatedIds = locallyValidatedIds,
                locallyValidatedByRole = locallyValidatedByRole
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
    locallyValidatedIds: SnapshotStateList<Int>,
    locallyValidatedByRole: MutableMap<Int, MutableSet<String>>
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
            locallyValidatedIds = locallyValidatedIds,
            locallyValidatedByRole = locallyValidatedByRole
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
    locallyValidatedIds: SnapshotStateList<Int>,
    locallyValidatedByRole: MutableMap<Int, MutableSet<String>>
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

    val roleKey = normalizeRoleKey(operateur)
    val serverMap = roleMapNorm(etape)

    val currentRoleValidated =
        (serverMap[roleKey] == "VALIDE") ||
                (locallyValidatedByRole[etape.id_Etape]?.contains(roleKey) == true)

    // IDs entièrement validées (serveur OU local par rôle) — utilisé pour les prérequis
    val fullyValidatedIds = remember(allEtapes, locallyValidatedByRole.keys, locallyValidatedByRole.values) {
        computeFullyValidatedIds(
            allEtapes,
            locallyValidatedByRole.mapValues { it.value.toSet() }
        )
    }

    val arePreconditionsMet = checkCustomConditions(
        etape.conditions_A_Valider,
        fullyValidatedIds,
        excludedIds
    )

    // Liste informative d’IDs manquants (approximative si expression complexe)
    val missingIds: List<Int> = remember(etape.conditions_A_Valider, fullyValidatedIds, excludedIds) {
        val raw = etape.conditions_A_Valider ?: return@remember emptyList()
        raw.replace("(", "").replace(")", "").replace("ou", "+").split('+')
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .filter { id -> id !in excludedIds && id !in fullyValidatedIds }
    }

    // Tous rôles validés ? (serveur OU local)
    val allRolesValidated = run {
        val roles = rolesOfNorm(etape)
        val local = locallyValidatedByRole[etape.id_Etape] ?: emptySet()
        roles.isNotEmpty() && roles.all { r -> (serverMap[r] == "VALIDE") || (r in local) }
    }

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
                                val ok = if (excluded) false else id in fullyValidatedIds
                                val color = when {
                                    excluded -> Color.Gray
                                    ok       -> Color(0xFF4CAF50) // ✅
                                    else     -> Color(0xFFF44336) // ❌
                                }
                                val status = when {
                                    excluded -> "👻"
                                    ok       -> "✅"
                                    else     -> "❌"
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
                    label = { Text("Commentaire") }, enabled = !currentRoleValidated,
                    modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = Color.White)
                )

                Spacer(Modifier.height(12.dp))
                val statusText = when {
                    allRolesValidated     -> "✅ Validée (tous rôles)"
                    currentRoleValidated  -> "✅ Validée (mon rôle)"
                    !arePreconditionsMet  -> "❌ Conditions non remplies"
                    else                  -> "⏳ En attente"
                }
                val statusColor = when {
                    allRolesValidated     -> Color.Green
                    currentRoleValidated  -> Color(0xFF8BC34A)
                    !arePreconditionsMet  -> Color.Red
                    else                  -> Color.Yellow
                }
                Text(
                    text = statusText + if (!arePreconditionsMet && missingIds.isNotEmpty())
                        "  |  Manquantes: ${missingIds.joinToString(", ")}" else "",
                    color = statusColor
                )

                val canValidate = !currentRoleValidated && arePreconditionsMet
                val canUnvalidate = currentRoleValidated

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
                                        locallyValidatedByRole[etape.id_Etape]?.remove(normalizeRoleKey(operateur))
                                        bgColor = Color(0x33FFFF00)
                                        etapeViewModel.loadEtapes(context)
                                    } else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                }
                            } else if (canValidate) {
                                etapeViewModel.validerEtape(
                                    context, etape.id_Etape, comTrim, descTrim, elapsed, operateur = operateur
                                ) { success, msg ->
                                    if (success) {
                                        val set = locallyValidatedByRole.getOrPut(etape.id_Etape) { mutableSetOf() }
                                        set += normalizeRoleKey(operateur)
                                        bgColor = Color(0x3300FF00)
                                        etapeViewModel.loadEtapes(context)
                                        if (currentIndex < etapes.lastIndex) {
                                            currentIndex++
                                            startTime = System.currentTimeMillis()
                                        } else {
                                            Toast.makeText(context, "Dernière étape validée !", Toast.LENGTH_SHORT).show()
                                        }
                                    } else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                }
                            }
                        },
                        enabled = canValidate || canUnvalidate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                canUnvalidate -> "🔄 Annuler"
                                canValidate   -> "✅ Valider"
                                else          -> "Pré-requis manquants"
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
                        enabled = currentIndex < etapes.lastIndex, // navigation toujours possible
                        modifier = Modifier.weight(1f)
                    ) { Text("Suivant") }
                }
            }
        }
    }
}

/* ----------------- Dialog & utils ----------------- */

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

// Tri topologique des étapes selon leurs prédecesseurs (tous opérateurs confondus)
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
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp)
        )
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().background(Color(0xFF121212)).padding(8.dp),
                content = content
            )
        }
    }
}
