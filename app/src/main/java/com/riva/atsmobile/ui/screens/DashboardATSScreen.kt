package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.network.ApiAutomateClient
import com.riva.atsmobile.ui.components.TrefileuseCard
import com.riva.atsmobile.ui.components.SoudeuseCard
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay

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
        "S1" to "DB2040",
        "S2" to "DB2042",
        "S3" to "DB2045"
    )

    var trefData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var soudData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            val trefMap = trefileuses.associate { (_, db) ->
                db to listOf(
                    "$db.DBX0.0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14", "$db.DBD18"
                )
            }
            val soudMap = soudeuses.associate { (_, db) ->
                db to listOf(
                    "$db.DBX0.0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14",
                    "$db.DBD18", "$db.DBD22", "$db.DBD26", "$db.DBW30"
                )
            }
            trefData = ApiAutomateClient.fetchGroupedValues(trefMap)
            soudData = ApiAutomateClient.fetchGroupedValues(soudMap)
            delay(2000)
        }
    }

    BaseScreen(
        title = "Dashboard ATS",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = true
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
        ) {
            items(trefileuses) { (label, db) ->
                val values = trefData[db]
                if (values != null) {
                    TrefileuseCard(
                        titre = "Trefileuse $label",
                        isActive = values["DBX0.0"] as? Boolean ?: false,
                        vitesseActuelle = (values["DBD6"] as? Number)?.toFloat() ?: 0f,
                        vitesseConsigne = (values["DBD2"] as? Number)?.toFloat() ?: 0f,
                        diametre = (values["DBD10"] as? Number)?.toFloat() ?: 0f,
                        longueurBobine = (values["DBD14"] as? Number)?.toFloat() ?: 0f,
                        poidsBobine = (values["DBD18"] as? Number)?.toFloat() ?: 0f
                    )
                }
            }
            items(soudeuses) { (label, db) ->
                val values = soudData[db]
                if (values != null) {
                    SoudeuseCard(
                        titre = "Soudeuse $label",
                        isActive = values["DBX0.0"] as? Boolean ?: false,
                        vitesseActuelle = (values["DBD6"] as? Number)?.toFloat() ?: 0f,
                        vitesseConsigne = (values["DBD2"] as? Number)?.toFloat() ?: 0f,
                        diamFil = (values["DBD10"] as? Number)?.toFloat() ?: 0f,
                        diamTrame = (values["DBD14"] as? Number)?.toFloat() ?: 0f,
                        longueur = (values["DBD18"] as? Number)?.toFloat() ?: 0f,
                        largeur = (values["DBD22"] as? Number)?.toFloat() ?: 0f,
                        energie = (values["DBD26"] as? Number)?.toInt() ?: 0,
                        nbPanneaux = (values["DBW30"] as? Number)?.toInt() ?: 0
                    )
                }
            }
        }
    }
}