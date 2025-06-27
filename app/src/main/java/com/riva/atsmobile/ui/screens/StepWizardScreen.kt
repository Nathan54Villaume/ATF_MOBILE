package com.riva.atsmobile.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.viewmodel.EtapeViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


@Composable
fun StepWizardScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    val context = LocalContext.current
    val etapes by etapeViewModel.etapes.collectAsState()
    val soudeuseEtapes = etapes.filter { it.affectation_etape?.contains("operateur_soudeuse") == true }
    val trefileuseEtapes = etapes.filter { it.affectation_etape?.contains("operateur_t1") == true }
    val trefileuse2Etapes = etapes.filter { it.affectation_etape?.contains("operateur_t2") == true }

    var currentIndexSoudeuse by remember { mutableStateOf(0) }
    var currentIndexTrefileuse by remember { mutableStateOf(0) }
    var currentIndexTrefileuse2 by remember { mutableStateOf(0) }
    var soudeuseExpanded by remember { mutableStateOf(true) }
    var trefileuseExpanded by remember { mutableStateOf(true) }
    var trefileuse2Expanded by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val currentGamme = selectionViewModel.memoireGammeActuelle
    val desiredGamme = selectionViewModel.memoireGammeVisee
    val nbFilsActuel = currentGamme?.nbFilChaine
    val nbFilsVise = desiredGamme?.nbFilChaine


    LaunchedEffect(true) {
        etapeViewModel.loadEtapes(context)

        if (nbFilsActuel != null && nbFilsVise != null && nbFilsActuel != nbFilsVise) {
            Log.d("StepWizardScreen", "Changement de ${nbFilsActuel} à ${nbFilsVise} fils de chaîne détecté")
        }
    }

    if (soudeuseEtapes.isEmpty() && trefileuseEtapes.isEmpty() && trefileuse2Etapes.isEmpty()) {
        Text("Aucune étape trouvée.", color = Color.White, modifier = Modifier.fillMaxSize().padding(32.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("✔ ${etapes.count { it.etat_etape == "VALIDE" }} / ${etapes.size} étapes validées", color = Color.White, style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Actuelle: ${currentGamme?.designation ?: "-"} (${nbFilsActuel ?: "-"} fils)",
                color = Color.LightGray
            )
            Text(
                text = "Visée: ${desiredGamme?.designation ?: "-"} (${nbFilsVise ?: "-"} fils)",
                color = Color.LightGray
            )
        }

        ExpandableCard(
            title = "Soudeuse",
            expanded = soudeuseExpanded,
            onToggle = { soudeuseExpanded = !soudeuseExpanded },
            content = {
                EtapeCardGroup("Soudeuse", soudeuseEtapes, currentIndexSoudeuse, { currentIndexSoudeuse = it }, etapeViewModel, Color(0xFF263238))
            }
        )

        ExpandableCard(
            title = "Tréfileuse T1",
            expanded = trefileuseExpanded,
            onToggle = { trefileuseExpanded = !trefileuseExpanded },
            content = {
                EtapeCardGroup("Tréfileuse T1", trefileuseEtapes, currentIndexTrefileuse, { currentIndexTrefileuse = it }, etapeViewModel, Color(0xFF1E272E))
            }
        )

        ExpandableCard(
            title = "Tréfileuse T2",
            expanded = trefileuse2Expanded,
            onToggle = { trefileuse2Expanded = !trefileuse2Expanded },
            content = {
                EtapeCardGroup("Tréfileuse T2", trefileuse2Etapes, currentIndexTrefileuse2, { currentIndexTrefileuse2 = it }, etapeViewModel, Color(0xFF2C3E50))
            }
        )
    }
}

@Composable
fun ExpandableCard(title: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
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
            content()
        }
    }
}


@Composable
fun EtapeCardGroup(
    title: String,
    etapes: List<Etape>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    etapeViewModel: EtapeViewModel,
    cardColor: Color
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val etape = etapes[currentIndex]

    var commentaire by remember(etape.id_etape) { mutableStateOf(etape.commentaire_etape_1 ?: "") }
    var description by remember(etape.id_etape) { mutableStateOf(etape.description_etape ?: "") }
    var isValidated by remember(etape.id_etape) { mutableStateOf(etape.etat_etape == "VALIDE") }
    var startTime by remember(etape.id_etape) { mutableStateOf(System.currentTimeMillis()) }
    var bgColor by remember { mutableStateOf(Color.Transparent) }
    val animatedBgColor by animateColorAsState(targetValue = bgColor, label = "bgColorAnim")

    LaunchedEffect(etape.id_etape) {
        startTime = System.currentTimeMillis()
        bgColor = Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(animatedBgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("$title - Étape ${currentIndex + 1}/${etapes.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(etape.libelle_etape, color = Color.White, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(12.dp))
            Text("Description", color = Color.White)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isValidated,
                placeholder = { Text("Entrer une description...", color = Color.Gray) },
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White
                )
            )

            Spacer(Modifier.height(12.dp))
            Text("Commentaire", color = Color.White)
            OutlinedTextField(
                value = commentaire,
                onValueChange = { commentaire = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isValidated,
                placeholder = { Text("Entrer un commentaire...", color = Color.Gray) },
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White
                )
            )

            Spacer(Modifier.height(12.dp))
            Text(if (isValidated) "✅ Étape validée" else "⏳ Étape en attente", color = if (isValidated) Color.Green else Color.Yellow)

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { if (currentIndex > 0) onIndexChange(currentIndex - 1) }, enabled = currentIndex > 0, modifier = Modifier.weight(1f)) {
                    Text("Précédent", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                            val filteredComment = if (commentaire.trim() == "Entrer un commentaire...") "" else commentaire.trim()
                            val filteredDesc = if (description.trim() == "Entrer une description...") "" else description.trim()
                            val success = if (isValidated) {
                                postDevalidation(context, etape.id_etape, filteredComment, filteredDesc)
                            } else {
                                postCommentaire(context, etape.id_etape, filteredComment, filteredDesc, elapsed)
                            }

                            if (success) {
                                bgColor = if (!isValidated) Color(0x3300FF00) else Color(0x33FFFF00)
                                etapeViewModel.loadEtapes(context)
                                isValidated = !isValidated
                            } else {
                                Log.e("ETAPE", "Erreur lors de l'action")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isValidated) "🔄 Dévalider" else "✅ Valider", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = { if (currentIndex < etapes.size - 1) onIndexChange(currentIndex + 1) }, enabled = currentIndex < etapes.size - 1, modifier = Modifier.weight(1f)) {
                    Text("Suivant", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

suspend fun postCommentaire(
    context: Context,
    etapeId: Int,
    commentaire: String,
    description: String,
    tempsReel: Int
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = "${ApiConfig.getBaseUrl(context)}/api/etapes/valider"
        val json = JSONObject().apply {
            put("id_etape", etapeId)
            put("commentaire", commentaire)
            put("description", description)
            put("tempsReel", tempsReel)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "application/json")
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            Log.d("POST_COMMENTAIRE", "HTTP ${response.code} - ${response.body?.string()}")
            response.isSuccessful
        }
    } catch (e: Exception) {
        Log.e("POST_COMMENTAIRE", "Erreur réseau : ${e.message}", e)
        false
    }
}

suspend fun postDevalidation(
    context: Context,
    etapeId: Int,
    commentaire: String,
    description: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = "${ApiConfig.getBaseUrl(context)}/api/etapes/devalider"
        val json = JSONObject().apply {
            put("id_etape", etapeId)
            put("commentaire", commentaire)
            put("description", description)
            put("tempsReel", 0)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "application/json")
            .build()

        OkHttpClient().newCall(request).execute().use { response ->
            Log.d("POST_DEVALIDATION", "HTTP ${response.code} - ${response.body?.string()}")
            response.isSuccessful
        }
    } catch (e: Exception) {
        Log.e("POST_DEVALIDATION", "Erreur réseau : ${e.message}", e)
        false
    }
}
