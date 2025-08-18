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
import androidx.compose.ui.Alignment
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
    s = s.replace(" ", "_").replace("-", "_")
    s = s.replace(Regex("^operateur_t[_\\- ]?([0-9])$"), "operateur_t$1")
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
 * Évalue la chaîne de conditions personnalisées.
 * - '+' sépare des blocs en AND (tous les blocs doivent être vrais)
 * - 'ou' est un OR *dans* un bloc
 * - Les IDs EXCLUS ne comptent pas comme "valides" ; on les **ignore** simplement.
 *   => Un bloc qui ne contient **que** des IDs exclus devient sans contrainte (vrai).
 */
private fun checkCustomConditions(
    conditionString: String?,
    fullyValidatedIds: Set<Int>,
    excludedIds: List<Int>
): Boolean {
    if (conditionString.isNullOrBlank()) return true

    // Sépare les blocs AND
    val blocks = conditionString.split('+')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    fun parseOrIds(raw: String): List<Int> {
        val inside = raw.removePrefix("(").removeSuffix(")")
        return inside
            .split(Regex("\\bou\\b", RegexOption.IGNORE_CASE))
            .mapNotNull { it.trim().toIntOrNull() }
    }

    return blocks.all { block ->
        // On enlève du bloc les IDs exclus (ils ne rendent PAS le bloc "vrai")
        val candidateIds = parseOrIds(block).filter { it !in excludedIds }
        when {
            // Bloc ne contenant que des IDs exclus => pas de contrainte
            candidateIds.isEmpty() -> true
            // Il faut qu'au moins un ID non-exclu soit validé
            else -> candidateIds.any { it in fullyValidatedIds }
        }
    }
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
            true
        } else if (roles.size == 1) {
            (server[roles[0]] == "VALIDE") || (roles[0] in local)
        } else {
            roles.all { r -> (server[r] == "VALIDE") || (r in local) }
        }
        if (fully) out += e.id_Etape
    }
    return out
}

/* ----------------- UI helpers (pastille + chips) ----------------- */

@Composable
private fun StepPill(currentIndex: Int, total: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0x22FFFFFF),
        contentColor = Color.White
    ) {
        Text(
            text = "Étape ${currentIndex + 1} / $total",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun RoleChips(etape: Etape, localRoles: Set<String>) {
    val roles = rolesOfNorm(etape)
    val server = roleMapNorm(etape)
    if (roles.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.forEach { role ->
            val validated = (server[role] == "VALIDE") || (role in localRoles)
            val bg = if (validated) Color(0x334CAF50) else Color(0x331E88E5)
            val fg = if (validated) Color(0xFF4CAF50) else Color(0xFF90CAF9)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = bg,
                contentColor = fg
            ) {
                val label = when (role) {
                    "operateur_t1" -> "T1"
                    "operateur_t2" -> "T2"
                    "operateur_soudeuse" -> "Soudeuse"
                    "mecanicien_1" -> "Mécanicien 1"
                    "mecanicien_2" -> "Mécanicien 2"
                    "pontier" -> "Pontier"
                    else -> role
                }
                Text(
                    text = (if (validated) "✅ " else "⏳ ") + label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/* ----------------- Écran ----------------- */

@Composable
fun EtapesScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    // Validations locales par rôle (id_Etape -> set de rôles normalisés)
    val locallyValidatedByRole = remember { mutableStateMapOf<Int, MutableSet<String>>() }

    val scope = rememberCoroutineScope()

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

            // --- Opérateurs de prod ---
            MachineSection(
                title = "Soudeuse",
                operateur = "operateur_soudeuse",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("operateur_soudeuse") },
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
                locallyValidatedByRole = locallyValidatedByRole
            )
            MachineSection(
                title = "Tréfileuse T1",
                operateur = "operateur_t1",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("operateur_t1") },
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
                locallyValidatedByRole = locallyValidatedByRole
            )
            MachineSection(
                title = "Tréfileuse T2",
                operateur = "operateur_t2",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("operateur_t2") },
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
                locallyValidatedByRole = locallyValidatedByRole
            )

            // --- Pontier & Maintenance ---
            MachineSection(
                title = "Pontier",
                operateur = "pontier",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("pontier") },
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                allEtapes = etapesTriees,
                cardColor = Color(0xFF37474F), // bleu-gris sombre
                locallyValidatedByRole = locallyValidatedByRole
            )
            MachineSection(
                title = "Mécanicien 1",
                operateur = "mecanicien_1",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("mecanicien_1") },
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                allEtapes = etapesTriees,
                cardColor = Color(0xFF283593), // indigo sombre
                locallyValidatedByRole = locallyValidatedByRole
            )
            MachineSection(
                title = "Mécanicien 2",
                operateur = "mecanicien_2",
                etapes = etapesTriees.filter { rolesOfNorm(it).contains("mecanicien_2") },
                currentGamme = currentGamme,
                desiredGamme = desiredGamme,
                zone = zone,
                intervention = intervention,
                etapeViewModel = etapeViewModel,
                context = context,
                isAdmin = isAdmin,
                excludedIds = idsToExclude,
                allEtapes = etapesTriees,
                cardColor = Color(0xFF00695C), // teal sombre
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
    locallyValidatedByRole: MutableMap<Int, MutableSet<String>>
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    ExpandableCard(title = title, expanded = expanded, onToggle = { expanded = !expanded }) {
        EtapeCardGroup(
            operateur = operateur,
            title = title,
            etapes = etapes,
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
            locallyValidatedByRole = locallyValidatedByRole
        )
    }
}

@Composable
private fun EtapeCardGroup(
    operateur: String,
    title: String,
    etapes: List<Etape>,
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
    locallyValidatedByRole: MutableMap<Int, MutableSet<String>>
) {
    // Toujours au début (pas de reprise)
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    if (currentIndex >= etapes.size) currentIndex = etapes.lastIndex
    val etape = etapes.getOrNull(currentIndex) ?: return

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

    val fullyValidatedIds = remember(allEtapes, locallyValidatedByRole.entries.toList()) {
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

    val missingIds: List<Int> = remember(etape.conditions_A_Valider, fullyValidatedIds, excludedIds) {
        val raw = etape.conditions_A_Valider ?: return@remember emptyList()
        raw.replace("(", "")
            .replace(")", "")
            .split('+') // AND blocks
            .flatMap { it.split(Regex("\\bou\\b", RegexOption.IGNORE_CASE)) } // OR ids
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .filter { id -> id !in excludedIds && id !in fullyValidatedIds }
    }

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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepPill(currentIndex, etapes.size)
                    if (isAdmin) {
                        Text(
                            "ID: ${etape.id_Etape}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(etape.libelle_Etape, color = Color.White, style = MaterialTheme.typography.titleLarge)

                // Rôles (visuel)
                Spacer(Modifier.height(6.dp))
                RoleChips(
                    etape = etape,
                    localRoles = (locallyValidatedByRole[etape.id_Etape] ?: emptySet())
                )

                if (!etape.conditions_A_Valider.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Conditions à valider :", color = Color.White, style = MaterialTheme.typography.titleSmall)

                    val condString = etape.conditions_A_Valider
                        .replace("+", " ET ")
                        .replace(Regex("\\bou\\b", RegexOption.IGNORE_CASE), " OU ")
                        .replace("(", "( ")
                        .replace(")", " )")
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("Précédent") }

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
                                    } else msg?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                }
                            }
                        },
                        enabled = canValidate || canUnvalidate,
                        modifier = Modifier.weight(1f),
                        colors = if (canUnvalidate)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        else ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            when {
                                canUnvalidate -> "❌ Dévalider"
                                canValidate   -> "✅ Valider"
                                else          -> "Pré-requis manquants"
                            }
                        )
                    }

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
// --- Helpers -------------------------------------------------------------

/**
 * Retourne les groupes de prédécesseurs tels que fournis (1 groupe = 1 rôle, par index).
 * - Les "0" sont des sentinelles "début de rôle" => on les exclut des arêtes.
 */
private fun predecessorGroups(e: Etape): List<List<Int>> =
    e.predecesseurs.map { grp -> grp.ids.filter { it != 0 } }

/**
 * Lecture "safe" de la propriété successeurs si elle existe.
 * - Accepte List<Int> ou List<Objets{ ids: List<Int> }>
 * - Retourne des groupes alignés par rôle (même indexation que prédecesseurs).
 * - Si le champ est absent dans Etape, renvoie une liste vide.
 */
private fun safeSuccesseurGroups(e: Etape): List<List<Int>> {
    // Essaye "successeurs" (ou "successeur" si besoin)
    val tryNames = listOf("successeurs", "successeur")
    for (name in tryNames) {
        try {
            val f = e.javaClass.getDeclaredField(name)
            f.isAccessible = true
            val raw = f.get(e)
            return when (raw) {
                is List<*> -> {
                    if (raw.isEmpty()) emptyList() else {
                        val first = raw.first()
                        if (first is Int) listOf(raw.mapNotNull { it as? Int })
                        else raw.map { item ->
                            try {
                                val ff = item?.javaClass?.getDeclaredField("ids")?.apply { isAccessible = true }
                                val value = ff?.get(item)
                                (value as? List<*>)?.mapNotNull { it as? Int }.orEmpty()
                            } catch (_: Throwable) { emptyList() }
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (_: Throwable) { /* champ absent : on ignore */ }
    }
    return emptyList()
}

/**
 * Calcule un "score de départ par rôles" : +1 pour chaque groupe de prédécesseurs vide (après retrait des 0).
 * Sert uniquement à départager les sources : plus le score est élevé, plus l’étape est prioritaire dans la file.
 */
private fun startScoreForRoles(e: Etape): Int {
    val groups = predecessorGroups(e) // 0 retirés
    if (groups.isEmpty()) return 0
    return groups.count { it.isEmpty() }
}

// --- Tri topologique Kahn avec alignement Rôle ↔ Groupe ------------------

/**
 * Construit le graphe dirigé en fusionnant :
 *  1) les PRÉDÉCESSEURS alignés par rôle (groupe i -> étape),
 *  2) les SUCCESSEURS alignés par rôle (étape -> groupe i),
 * en considérant "0" comme "début de rôle" (pas d'arête).
 * La file de Kahn est déterministe : on priorise les étapes "débutantes" (plus de rôles à 0),
 * puis on départage par id.
 */
private fun getOrderedSteps(etapes: List<Etape>): List<Etape> {
    val byId = etapes.associateBy { it.id_Etape }

    // Graphe u -> {v} et degrés entrants
    val adj = mutableMapOf<Int, MutableSet<Int>>()
    val indeg = mutableMapOf<Int, Int>().apply { etapes.forEach { put(it.id_Etape, 0) } }

    fun addEdge(u: Int, v: Int) {
        if (u == v) return
        if (!byId.containsKey(u) || !byId.containsKey(v)) return // ignore ids exclus/absents
        val set = adj.getOrPut(u) { mutableSetOf() }
        if (set.add(v)) indeg[v] = (indeg[v] ?: 0) + 1
    }

    // Pré-calcul du "start score" pour prioriser les sources ayant des 0 (début de rôle)
    val startScore = etapes.associate { e -> e.id_Etape to startScoreForRoles(e) }

    // Construit les arêtes en respectant l’alignement par rôle
    for (e in etapes) {
        val roles = rolesOfNorm(e) // ordre des affectations
        val predGroups = predecessorGroups(e)           // 0 retirés ici
        val succGroups = safeSuccesseurGroups(e)        // peut être vide si champ absent

        // 1) Prédécesseurs (groupe i -> e)
        val nPred = minOf(roles.size, predGroups.size)
        for (i in 0 until nPred) predGroups[i].forEach { p -> addEdge(p, e.id_Etape) }
        // Groupes surnuméraires éventuels (sécurité)
        for (i in nPred until predGroups.size) predGroups[i].forEach { p -> addEdge(p, e.id_Etape) }

        // 2) Successeurs (e -> groupe i) si dispo
        val nSucc = minOf(roles.size, succGroups.size)
        for (i in 0 until nSucc) succGroups[i].forEach { s -> addEdge(e.id_Etape, s) }
        for (i in nSucc until succGroups.size) succGroups[i].forEach { s -> addEdge(e.id_Etape, s) }
    }

    // Kahn déterministe : on trie d'abord par "start score" décroissant (plus de débuts de rôle en premier), puis par id croissant
    val queue = java.util.PriorityQueue<Int> { a, b ->
        val sa = startScore[a] ?: 0
        val sb = startScore[b] ?: 0
        when {
            sa != sb -> sb - sa                 // score plus grand en premier
            else     -> a.compareTo(b)          // sinon par id
        }
    }.apply {
        indeg.filter { it.value == 0 }.keys.forEach { add(it) }
    }

    val out = mutableListOf<Int>()
    while (queue.isNotEmpty()) {
        val u = queue.poll()
        out += u
        for (v in adj[u].orEmpty()) {
            val nv = (indeg[v] ?: 0) - 1
            indeg[v] = nv
            if (nv == 0) queue.add(v)
        }
    }

    // Fallback si cycle/incohérence (on ne perd rien)
    if (out.size != etapes.size) {
        val remaining = etapes.map { it.id_Etape }.toSet() - out.toSet()
        Log.w("EtapesScreen", "Cycle/incohérence détecté, fallback tri par id: $remaining")
        out += remaining.sorted()
    }

    return out.mapNotNull { byId[it] }
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
