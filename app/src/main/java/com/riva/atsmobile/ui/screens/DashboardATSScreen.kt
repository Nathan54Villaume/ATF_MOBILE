package com.riva.atsmobile.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riva.atsmobile.network.ApiAutomateClient
import com.riva.atsmobile.ui.components.SoudeuseCard
import com.riva.atsmobile.ui.components.TrefileuseCard
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay
import kotlin.math.round

@Composable
fun DashboardATSScreen(navController: NavController, viewModel: SelectionViewModel) {
    val trefileuses = listOf(
        "T1" to "DB2020",
        "T2" to "DB2022",
        "T3" to "DB2024",
        "T4" to "DB2026",
        "T5" to "DB2028",
        "T6" to "DB2030",
        "T7" to "DB2033",
        "T8" to "DB2035"
    )

    val soudeuses = listOf(
        "Soudeuse 1" to "DB2040",
        "Soudeuse 2" to "DB2042",
        "Soudeuse 3" to "DB2045"
    )
    // contexte + baseUrl
    val context = LocalContext.current
    val baseUrl = ApiConfig.getBaseUrl(context)

    var trefData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var soudData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            val trefAddresses = trefileuses.associate { (_, db) ->
                db to listOf(
                    "$db.DBX0.0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14", "$db.DBD18"
                )
            }
            val soudAddresses = soudeuses.associate { (_, db) ->
                db to listOf(
                    "$db.DBX0.0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14",
                    "$db.DBD18", "$db.DBD22", "$db.DBD26", "$db.DBD30"
                )
            }

            trefData = ApiAutomateClient.fetchGroupedValues(trefAddresses, baseUrl)
            soudData = ApiAutomateClient.fetchGroupedValues(soudAddresses, baseUrl)
            delay(1000)
        }
    }

    BaseScreen(
        title = "Dashboard ATS",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = viewModel.isOnline.collectAsState().value
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
        ) {
            item {
                Text(
                    "Tréfileuses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color.DarkGray, Color.Black)))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )
            }
            items(trefileuses) { (label, db) ->
                val values = trefData[db]
                if (values != null && values.isNotEmpty()) {
                    Log.d("Dashboard", "Trefileuse $label → $values")

                    TrefileuseCard(
                        titre = "Trefileuse $label",
                        // SUPPRESSION ICI : Le paramètre 'isActive' n'est plus utilisé par TrefileuseCard
                        vitesseConsigne = round(((values["$db.DBD2"] as? Number)?.toFloat() ?: 0f) / 100f) / 10f, // Vérifiez l'unité m/s ici
                        vitesseActuelle = round(((values["$db.DBD6"] as? Number)?.toFloat() ?: 0f) / 100f) / 10f, // Vérifiez l'unité m/s ici
                        diametre = (values["$db.DBD10"] as? Number)?.toFloat() ?: 0f,
                        longueurBobine = round(((values["$db.DBD14"] as? Number)?.toFloat() ?: 0f ) / 1f) /10f,
                        poidsBobine = round(((values["$db.DBD18"] as? Number)?.toFloat() ?: 0f ) / 1f) /10f
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Text(
                            "Trefileuse $label : données indisponibles",
                            modifier = Modifier.padding(16.dp),
                            color = Color.LightGray
                        )
                    }
                }
            }
            item {
                Text(
                    "Soudeuses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color.DarkGray, Color.Black)))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )
            }
            items(soudeuses) { (label, db) ->
                val values = soudData[db]
                if (values != null && values.isNotEmpty()) {
                    Log.d("Dashboard", "$label → $values")

                    SoudeuseCard(
                        titre = label,
                        // SUPPRESSION ICI : Le paramètre 'isActive' n'est plus utilisé par SoudeuseCard
                        vitesseConsigne = round(((values["$db.DBD2"] as? Number)?.toFloat() ?: 0f) / 100f) / 10f,
                        vitesseActuelle = round(((values["$db.DBD6"] as? Number)?.toFloat() ?: 0f) / 100f) / 10f,
                        diamFil = (values["$db.DBD10"] as? Number)?.toFloat() ?: 0f,
                        diamTrame = (values["$db.DBD14"] as? Number)?.toFloat() ?: 0f,
                        longueur = (values["$db.DBD18"] as? Number)?.toFloat() ?: 0f,
                        largeur = (values["$db.DBD22"] as? Number)?.toFloat() ?: 0f,
                        energie = (values["$db.DBD26"] as? Number)?.toInt() ?: 0,
                        nbPanneaux = (values["$db.DBD30"] as? Number)?.toInt() ?: 0
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Text(
                            "$label : données indisponibles",
                            modifier = Modifier.padding(16.dp),
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}