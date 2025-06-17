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
import com.riva.atsmobile.ui.components.TrefileuseCard
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardATRScreen(navController: NavController, viewModel: SelectionViewModel) {
    val lignes = listOf(
        "Ligne 1" to "DB2003",
        "Ligne 2" to "DB2005",
        "Ligne 3" to "DB2007",
        "Ligne 4" to "DB2009",
        "Ligne 5" to "DB2011",
        "Ligne 6" to "DB2013",
        "Ligne 7" to "DB2015"
    )

    var data by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            val addresses = lignes.associate { (_, db) ->
                db to listOf(
                    "$db.DBB0", "$db.DBD2", "$db.DBD6", "$db.DBD10", "$db.DBD14", "$db.DBD18"
                )
            }
            data = ApiAutomateClient.fetchGroupedValues(addresses)
            delay(2000)
        }
    }

    BaseScreen(
        title = "Dashboard ATR",
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
                    Log.d("Dashboard", "$label → $values") // 🔍 LOG TEMPORAIRE

                    TrefileuseCard(
                        titre = label,
                        isActive = when (val v = values["DBB0"]) {
                            is Boolean -> v
                            is Number -> v.toInt() != 0
                            else -> false
                        },
                        vitesseConsigne = (values["DBD2"] as? Number)?.toFloat() ?: 0f,
                        vitesseActuelle = (values["DBD6"] as? Number)?.toFloat() ?: 0f,
                        diametre = (values["DBD10"] as? Number)?.toFloat() ?: 0f,
                        longueurBobine = (values["DBD14"] as? Number)?.toFloat() ?: 0f,
                        poidsBobine = (values["DBD18"] as? Number)?.toFloat() ?: 0f
                    )
                }
            }
        }
    }
}
