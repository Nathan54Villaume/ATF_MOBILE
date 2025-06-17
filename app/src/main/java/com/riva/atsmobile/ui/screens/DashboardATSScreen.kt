package com.riva.atsmobile.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.network.ApiAutomateClient
import com.riva.atsmobile.ui.components.SoudeuseCard
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardATSScreen(navController: NavController, viewModel: SelectionViewModel) {
    val lignes = listOf(
        "Soudeuse 1" to "DB2040",
        "Soudeuse 2" to "DB2042",
        "Soudeuse 3" to "DB2045"
    )

    var data by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            val addresses = lignes.associate { (_, db) ->
                db to listOf(
                    "$db.DBB0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14",
                    "$db.DBD18", "$db.DBD22", "$db.DBD26", "$db.DBD30", "$db.DBD34"
                )
            }
            data = ApiAutomateClient.fetchGroupedValues(addresses)
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
            items(lignes) { (label, db) ->
                val values = data[db]
                if (values != null) {
                    Log.d("Dashboard", "$label → $values")

                    SoudeuseCard(
                        titre = label,
                        isActive = when (val v = values["$db.DBB0"]) {
                            is Boolean -> v
                            is Number -> v.toInt() != 0
                            else -> false
                        },
                        vitesseConsigne = (values["$db.DBD2"] as? Number)?.toFloat() ?: 0f,
                        vitesseActuelle = (values["$db.DBD6"] as? Number)?.toFloat() ?: 0f,
                        diamFil = (values["$db.DBD10"] as? Number)?.toFloat() ?: 0f,
                        diamTrame = (values["$db.DBD14"] as? Number)?.toFloat() ?: 0f,
                        longueur = (values["$db.DBD18"] as? Number)?.toFloat() ?: 0f,
                        largeur = (values["$db.DBD22"] as? Number)?.toFloat() ?: 0f,
                        energie = (values["$db.DBD26"] as? Number)?.toInt() ?: 0,
                        nbPanneaux = (values["$db.DBD30"] as? Number)?.toInt() ?: 0
                    )
                }
            }
        }
    }
}
