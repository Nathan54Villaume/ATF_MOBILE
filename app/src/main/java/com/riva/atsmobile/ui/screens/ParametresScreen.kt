package com.riva.atsmobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.BuildConfig
import com.riva.atsmobile.R
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParametresScreen(navController: NavController, viewModel: SelectionViewModel) {
    val context = LocalContext.current
    val nom by viewModel.nom.collectAsState()
    val matricule by viewModel.matricule.collectAsState()
    val role by viewModel.role.collectAsState()
    val devMode by viewModel.devModeEnabled.collectAsState()
    val isConnected by viewModel.isOnline.collectAsState()

    val estConnecte = nom.isNotBlank() && matricule.isNotBlank() && role.isNotBlank()
    val dateSansSecondes = remember(BuildConfig.BUILD_TIME) { BuildConfig.BUILD_TIME.take(16) }

    var tapCount by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BaseScreen(
        title = "Paramètres",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        connectionStatus = isConnected
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo_parametres),
                    contentDescription = "Logo de l'application",
                    modifier = Modifier.size(192.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (estConnecte) {
                    InfoItem("Utilisateur", "$nom ($matricule)")
                    InfoItem("Rôle", role)
                } else {
                    InfoItem("Utilisateur", "Invité")
                }

                InfoItem("Dernière mise à jour", dateSansSecondes)

                Text(
                    text = "Version : ${BuildConfig.BUILD_VERSION}",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            tapCount++
                            if (tapCount >= 7 && !devMode) {
                                viewModel.activateDevMode()
                                Toast.makeText(context, "🧪 Mode développeur activé", Toast.LENGTH_SHORT).show()
                                tapCount = 0
                            }
                        }
                )

                if (devMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.navigate("devtools") }) {
                        Text("🧪 Outils développeur")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (estConnecte) {
                    // Bouton de déconnexion
                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Déconnexion",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Se déconnecter", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nouveau bouton : Accès à l'écran de changement de mot de passe
                    Button(
                        onClick = { navController.navigate("change_password") },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    ) {
                        Text("Changer le mot de passe")
                    }

                    if (role == "ADMIN") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate("param_exclusions") },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(48.dp)
                        ) {
                            Text("⚙️ Gérer exclusions d’étapes")
                        }
                    }

                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirmation") },
                text = { Text("Voulez-vous vraiment vous déconnecter ?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch {
                            delay(300)
                            viewModel.reset()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }) {
                        Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}
