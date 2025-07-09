// file: app/src/main/java/com/riva/atsmobile/ui/screens/EtapesScreen.kt
package com.riva.atsmobile.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.logic.StepFilterManager
import com.riva.atsmobile.model.*
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.*

@Composable
fun EtapesScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        StepFilterManager.init(context)
        etapeViewModel.loadEtapes(context)
    }

    val etapes by etapeViewModel.etapes.collectAsState()
    val currentGamme: Gamme? = selectionViewModel.memoireGammeActuelle
    val desiredGamme: Gamme? = selectionViewModel.memoireGammeVisee
    val nbFilsActuel by selectionViewModel.nbFilsActuelFlow.collectAsState()
    val nbFilsVise by selectionViewModel.nbFilsViseFlow.collectAsState()
    val isAdmin by selectionViewModel.isAdmin.collectAsState()

    var idsToExclude by remember { mutableStateOf(emptyList<Int>()) }
    LaunchedEffect(nbFilsActuel, nbFilsVise) {
        idsToExclude = StepFilterManager.getExcludedSteps(nbFilsActuel, nbFilsVise)
        Log.d("StepWizard", "Exclusions: $idsToExclude")
    }

    val etapesFiltres by remember(etapes, idsToExclude) {
        derivedStateOf { etapes.filter { it.id_Etape !in idsToExclude } }
    }

    val etapesFiltresTriees by remember(etapesFiltres) {
        derivedStateOf { getOrderedSteps(etapesFiltres) }
    }

    BaseScreen(
        title = "Étapes de changement",
        navController = navController,
        viewModel = selectionViewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = true
    ) { padding ->
        if (etapesFiltresTriees.isEmpty()) {
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gamme Actuelle : ${currentGamme?.designation ?: "-"} (${nbFilsActuel ?: "-"})",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Gamme Visée : ${desiredGamme?.designation ?: "-"} (${nbFilsVise ?: "-"})",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            var soudeuseExpanded by remember { mutableStateOf(true) }
            var tref1Expanded by remember { mutableStateOf(true) }
            var tref2Expanded by remember { mutableStateOf(true) }

            ExpandableCard("Soudeuse", soudeuseExpanded, { soudeuseExpanded = !soudeuseExpanded }) {
                EtapeCardGroup(
                    title = "Soudeuse",
                    etapes = etapesFiltresTriees.filter { it.affectation_Etape.contains("operateur_soudeuse") },
                    etapeViewModel = etapeViewModel,
                    context = context,
                    cardColor = Color(0xFF263238),
                    isAdmin = isAdmin,
                    allEtapes = etapes,
                    excludedIds = idsToExclude
                )
            }
            ExpandableCard("Tréfileuse T1", tref1Expanded, { tref1Expanded = !tref1Expanded }) {
                EtapeCardGroup(
                    title = "Tréfileuse T1",
                    etapes = etapesFiltresTriees.filter { it.affectation_Etape.contains("operateur_t1") },
                    etapeViewModel = etapeViewModel,
                    context = context,
                    cardColor = Color(0xFF1E272E),
                    isAdmin = isAdmin,
                    allEtapes = etapes,
                    excludedIds = idsToExclude
                )
            }
            ExpandableCard("Tréfileuse T2", tref2Expanded, { tref2Expanded = !tref2Expanded }) {
                EtapeCardGroup(
                    title = "Tréfileuse T2",
                    etapes = etapesFiltresTriees.filter { it.affectation_Etape.contains("operateur_t2") },
                    etapeViewModel = etapeViewModel,
                    context = context,
                    cardColor = Color(0xFF2C3E50),
                    isAdmin = isAdmin,
                    allEtapes = etapes,
                    excludedIds = idsToExclude
                )
            }
        }
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
    etapeViewModel: EtapeViewModel,
    context: Context,
    cardColor: Color,
    isAdmin: Boolean,
    allEtapes: List<Etape>,
    excludedIds: List<Int>
) {
    var currentIndex by remember { mutableStateOf(0) }
    val etape = etapes.getOrNull(currentIndex) ?: return

    var startTime by remember(etape.id_Etape) { mutableStateOf(System.currentTimeMillis()) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor)

    var description by remember(etape.id_Etape) { mutableStateOf(etape.description_Etape.orEmpty()) }
    LaunchedEffect(etape.description_Etape) { description = etape.description_Etape.orEmpty() }

    var commentaire by remember(etape.id_Etape) { mutableStateOf(etape.commentaire_Etape_1.orEmpty()) }
    LaunchedEffect(etape.commentaire_Etape_1) { commentaire = etape.commentaire_Etape_1.orEmpty() }

    var isValidated by remember(etape.etat_Etape) { mutableStateOf(etape.etat_Etape == "VALIDE") }
    LaunchedEffect(etape.etat_Etape) { isValidated = etape.etat_Etape == "VALIDE" }

    val arePreconditionsMet = remember(etape, allEtapes, excludedIds) {
        if (etape.predecesseurs.isEmpty()) {
            true
        } else {
            val etapeMap = allEtapes.associateBy { it.id_Etape }
            etape.predecesseurs.all { relation ->
                val validIds = relation.ids.filter { it != 0 && it !in excludedIds }
                if (validIds.isEmpty()) return@all true

                when (relation.operateur.uppercase()) {
                    "OU" -> validIds.any { id -> etapeMap[id]?.etat_Etape == "VALIDE" }
                    else -> validIds.all { id -> etapeMap[id]?.etat_Etape == "VALIDE" }
                }
            }
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
            Text(
                etape.libelle_Etape,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            if (etape.predecesseurs.isNotEmpty()) {
                val validPredecesseurs = etape.predecesseurs.map { relation ->
                    relation.copy(ids = relation.ids.filter { it !in excludedIds })
                }.filter { it.ids.isNotEmpty() }

                if (validPredecesseurs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Conditions à valider :", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    validPredecesseurs.forEach { relation ->
                        val conditionText = relation.ids.joinToString(separator = " ${relation.operateur.uppercase()} ") { id ->
                            val dependencyEtape = allEtapes.find { it.id_Etape == id }
                            val status = if (dependencyEtape?.etat_Etape == "VALIDE") "✅" else "❌"
                            val role = dependencyEtape?.affectation_Etape?.let { "[$it]" } ?: ""
                            "($id: ${dependencyEtape?.libelle_Etape ?: "Inconnu"}) $role $status"
                        }
                        Text(
                            text = conditionText,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (!etape.description_Etape.isNullOrBlank() || isAdmin) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (isAdmin) description = it else Log.d("EtapesScreen", "Non-admin user tried to edit description") },
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
            Text(
                text = if (isValidated) "✅ Validée" else if (!arePreconditionsMet) "❌ Conditions non remplies" else "⏳ En attente",
                color = if (isValidated) Color.Green else if (!arePreconditionsMet) Color.Red else Color.Yellow
            )

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("Précédent") }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!isValidated && !arePreconditionsMet) {
                            Toast.makeText(context, "Veuillez valider les étapes précédentes requises.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        val trimmedCommentaire = commentaire.trim()
                        val trimmedDescription = description.trim()

                        if (isValidated) {
                            etapeViewModel.devaliderEtape(
                                context = context,
                                id_Etape = etape.id_Etape,
                                commentaire = trimmedCommentaire,
                                description = trimmedDescription,
                                tempsReel = 0
                            ) { success, message ->
                                if (success) {
                                    bgColor = Color(0x33FFFF00)
                                } else {
                                    message?.let {
                                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } else {
                            etapeViewModel.validerEtape(
                                context = context,
                                id_Etape = etape.id_Etape,
                                commentaire = trimmedCommentaire,
                                description = trimmedDescription,
                                tempsReel = elapsed
                            ) { success, message ->
                                if (success) {
                                    bgColor = Color(0x3300FF00)
                                } else {
                                    message?.let {
                                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = isValidated || arePreconditionsMet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isValidated) "🔄 Annuler" else "✅ Valider")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { if (currentIndex < etapes.lastIndex) currentIndex++ },
                    enabled = currentIndex < etapes.lastIndex,
                    modifier = Modifier.weight(1f)
                ) { Text("Suivant") }
            }
        }
    }
}


private fun getOrderedSteps(etapes: List<Etape>): List<Etape> {
    val etapeMap = etapes.associateBy { it.id_Etape }
    val visited = mutableSetOf<Int>()
    val orderedList = mutableListOf<Etape>()

    fun visit(etape: Etape) {
        if (!visited.add(etape.id_Etape)) return
        etape.predecesseurs.flatMap { it.ids }.filter { it != 0 }.mapNotNull { etapeMap[it] }.forEach(::visit)
        if (etape !in orderedList) orderedList += etape
    }

    etapes.forEach(::visit)
    return orderedList
}