package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeOperationScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    // Initialisation du réseau et des données
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.InitNetworkObserverIfNeeded(context)
        // TODO: charger les gammes depuis l'API ou source locale
        // viewModel.setGammes(fetchGammes())
    }

    // États provenant du ViewModel
    val isConnected by viewModel.isOnline.collectAsState()
    val gammes by viewModel.gammes.collectAsState()
    val currentGamme by viewModel.currentGamme.collectAsState()
    val desiredGamme by viewModel.desiredGamme.collectAsState()
    val zone by viewModel.zoneDeTravail.collectAsState()
    val intervention by viewModel.intervention.collectAsState()

    BaseScreen(
        title = "Type d’opération",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Sélection : Gamme actuelle
                Text(text = "Gamme actuelle :")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    userScrollEnabled = false
                ) {
                    items(gammes) { gamme: Gamme ->
                        OutlinedButton(
                            onClick = { viewModel.selectCurrentGamme(gamme) },
                            border = BorderStroke(
                                2.dp,
                                if (currentGamme == gamme)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                        ) {
                            Text(gamme.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sélection : Gamme souhaitée
                Text(text = "Gamme souhaitée :")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    userScrollEnabled = false
                ) {
                    items(gammes) { gamme: Gamme ->
                        OutlinedButton(
                            onClick = { viewModel.selectDesiredGamme(gamme) },
                            enabled = gamme != currentGamme,
                            border = BorderStroke(
                                2.dp,
                                if (desiredGamme == gamme)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                        ) {
                            Text(gamme.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Détails des sélections
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        currentGamme?.let { gamme ->
                            Text(text = "Nom : ${gamme.name}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Maille (mm) : ${gamme.meshSize}")
                            Text(text = "Fil (mm) : ${gamme.wireDiameter}")
                            Text(text = "Chaîne : ${gamme.chainCount}")
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        desiredGamme?.let { gamme ->
                            Text(text = "Souhait : ${gamme.name}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Maille (mm) : ${gamme.meshSize}")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Boutons Retour / Valider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Retour")
                    }
                    Button(
                        onClick = {
                            viewModel.validateGammeChange { success, msg ->
                                // TODO: afficher Snackbar ou Dialog avec msg
                            }
                        },
                        enabled = currentGamme != null && desiredGamme != null
                    ) {
                        Text("Valider")
                    }
                }
            }

            // Bandeau bas : zone, intervention et date/heure
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Zone : $zone    Interv. : $intervention")
                    Text(text = now)
                }
            }
        }
    }
}
