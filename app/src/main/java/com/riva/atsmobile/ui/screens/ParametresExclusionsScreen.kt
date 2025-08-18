// file: app/src/main/java/com/riva/atsmobile/ui/screens/ParametresExclusionsScreen.kt
package com.riva.atsmobile.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.riva.atsmobile.logic.StepFilterManager
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.EtapeCreateDto
import com.riva.atsmobile.model.EtapeUpdateDto
import com.riva.atsmobile.viewmodel.EtapeViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun ParametresExclusionsScreen(
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    ExclusionsParamSection(selectionViewModel, etapeViewModel)
}

@Composable
fun ExclusionsParamSection(
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    val couples = listOf("12-12", "12-16", "16-12", "16-16")
    var selectedCouple by remember { mutableStateOf(couples.first()) }

    val allEtapes by etapeViewModel.etapes.collectAsState()
    val exclusions = remember { mutableStateMapOf<String, MutableSet<Int>>() }

    var showDialog by remember { mutableStateOf(false) }
    var editingEtape by remember { mutableStateOf<Etape?>(null) }

    var libelle by remember { mutableStateOf("") }
    var affectation by remember { mutableStateOf("") }
    var roleLog by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf("") }
    var duree by remember { mutableStateOf("") }
    var predecessor by remember { mutableStateOf("") }
    var successor by remember { mutableStateOf("") }
    var conditionsAValider by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        StepFilterManager.init(context)
        exclusions.clear()
        exclusions.putAll(
            StepFilterManager
                .loadExclusions()
                .mapValues { it.value.toMutableSet() }
        )
        etapeViewModel.loadEtapes(context)
    }

    val selectedSet = exclusions.getOrPut(selectedCouple) { mutableSetOf() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Couple", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        DropdownMenuCoupleSelector(
            selected = selectedCouple,
            options = couples,
            onSelected = { selectedCouple = it }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                editingEtape = null
                libelle = ""
                affectation = ""
                roleLog = ""
                phase = ""
                duree = ""
                predecessor = ""
                successor = ""
                conditionsAValider = ""
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter une étape")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Étapes à exclure (${selectedSet.size})",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                items = allEtapes.sortedBy { it.id_Etape },
                key = { it.id_Etape }
            ) { etape ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            editingEtape = etape
                            libelle = etape.libelle_Etape
                            affectation = etape.affectation_Etape
                            roleLog = etape.role_Log
                            phase = etape.phase_Etape
                            duree = etape.duree_Etape?.toString() ?: ""

                            // on pré-remplit avec UN entier > 1 (ou vide)
                            predecessor = etape.predecesseurs
                                .flatMap { it.ids }
                                .firstOrNull { it > 1 }
                                ?.toString() ?: ""

                            successor = etape.successeurs
                                .flatMap { it.ids }
                                .firstOrNull { it > 1 }
                                ?.toString() ?: ""

                            conditionsAValider = etape.conditions_A_Valider.orEmpty()
                            showDialog = true
                        }
                ) {
                    Checkbox(
                        checked = etape.id_Etape in selectedSet,
                        onCheckedChange = { isChecked ->
                            if (isChecked) selectedSet.add(etape.id_Etape)
                            else selectedSet.remove(etape.id_Etape)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("#${etape.id_Etape} - ${etape.libelle_Etape}")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Edit, contentDescription = "Éditer")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                StepFilterManager.saveExclusions(
                    exclusions.mapValues { it.value.toList() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Enregistrer exclusions")
        }
    }

    if (showDialog) {
        val affectationOptions = allEtapes.map { it.affectation_Etape }.distinct().sorted()
        val roleOptions = allEtapes.map { it.role_Log }.distinct().sorted()
        val phaseOptions = allEtapes.map { it.phase_Etape }.distinct().sorted()

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingEtape == null) "Ajouter une étape" else "Modifier étape") },
            text = {
                Column {
                    if (editingEtape != null) {
                        OutlinedTextField(
                            value = editingEtape!!.id_Etape.toString(),
                            onValueChange = {},
                            label = { Text("ID") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = libelle,
                        onValueChange = { libelle = it },
                        label = { Text("Libellé") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuSelector(
                        label = "Affectation (opérateurs ; séparés par ';')",
                        selected = affectation,
                        options = affectationOptions,
                        onSelected = { affectation = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuSelector(
                        label = "Rôle (role_log)",
                        selected = roleLog,
                        options = roleOptions,
                        onSelected = { roleLog = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuSelector(
                        label = "Phase",
                        selected = phase,
                        options = phaseOptions,
                        onSelected = { phase = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = duree,
                        onValueChange = { input ->
                            val cleaned = input.filter { it.isDigit() }.take(6)
                            duree = cleaned
                        },
                        label = { Text("Durée (secondes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = predecessor,
                        onValueChange = { predecessor = it },
                        label = { Text("Prédécesseur (un entier > 1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = successor,
                        onValueChange = { successor = it },
                        label = { Text("Successeur (un entier > 1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = conditionsAValider,
                        onValueChange = { conditionsAValider = it },
                        label = { Text("Conditions à valider (ex: A+B)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // -> un seul entier > 1, sinon vide
                    val predValue = firstValidIdOrEmpty(predecessor)
                    val succValue = firstValidIdOrEmpty(successor)

                    Log.d(
                        "ParametresExcl",
                        "UPDATE id=${editingEtape?.id_Etape} affectation='$affectation' predRaw='$predecessor' succRaw='$successor' => pred='$predValue' succ='$succValue'"
                    )

                    val defaultEtatParRole =
                        if (editingEtape == null) {
                            val ops = affectation.split(';')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            if (ops.isEmpty()) null else ops.associateWith { "EN_ATTENTE" }
                        } else {
                            editingEtape?.etatParRole
                        }

                    val createDto = EtapeCreateDto(
                        libelle_Etape        = libelle,
                        affectation_Etape    = affectation,
                        role_Log             = roleLog,
                        phase_Etape          = phase,
                        duree_Etape          = duree.toIntOrNull(),
                        description_Etape    = editingEtape?.description_Etape,
                        etatParRole          = defaultEtatParRole,
                        temps_Reel_Etape     = editingEtape?.temps_Reel_Etape,
                        commentaire_Etape_1  = editingEtape?.commentaire_Etape_1,
                        // IMPORTANT : on envoie maintenant une simple chaîne "12" ou ""
                        predecesseur_etape   = predValue,
                        successeur_etape     = succValue,
                        conditions_A_Valider = conditionsAValider
                    )

                    val updateDto = EtapeUpdateDto(
                        libelle_Etape        = libelle,
                        affectation_Etape    = affectation,
                        role_Log             = roleLog,
                        phase_Etape          = phase,
                        duree_Etape          = duree.toIntOrNull(),
                        description_Etape    = editingEtape?.description_Etape,
                        etatParRole          = editingEtape?.etatParRole,
                        temps_Reel_Etape     = editingEtape?.temps_Reel_Etape,
                        commentaire_Etape_1  = editingEtape?.commentaire_Etape_1,
                        predecesseur_etape   = predValue,
                        successeur_etape     = succValue,
                        conditions_A_Valider = conditionsAValider
                    )

                    if (editingEtape == null) {
                        etapeViewModel.createEtape(context, createDto) { }
                    } else {
                        etapeViewModel.updateEtape(context, editingEtape!!.id_Etape, updateDto) { }
                    }
                    etapeViewModel.loadEtapes(context)
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/* ============================= HELPERS UI ============================= */

@Composable
fun DropdownMenuCoupleSelector(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Couple") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownMenuSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ============================ HELPERS LOGIQUES ============================ */

// Extrait le PREMIER entier > 1 du texte saisi, sinon renvoie "".
private fun firstValidIdOrEmpty(raw: String): String {
    val match = Regex("\\d+").findAll(raw)
        .mapNotNull { it.value.toIntOrNull() }
        .firstOrNull { it > 1 }
    return match?.toString() ?: ""
}
