package com.riva.atsmobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import com.riva.atsmobile.BuildConfig
import com.riva.atsmobile.R
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.viewmodel.EtapeViewModel

import com.riva.atsmobile.ui.screens.TypeOperationParamScreen
import com.riva.atsmobile.ui.screens.ExclusionsParamSection

@Composable
fun ParametresScreen(
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    etapeViewModel: EtapeViewModel
) {
    // Collecte le rôle de l'utilisateur
    val role by selectionViewModel.role.collectAsState()

    // Définit la liste complète des onglets
    val allTabs = listOf("Général", "Gammes", "Exclusions")

    // Filtre les onglets en fonction du rôle
    val filteredTabs = remember(role) {
        allTabs.filter { tab ->
            when (tab) {
                "Général" -> true // L'onglet Général est toujours visible
                "Gammes" -> role == "ADMIN" // L'onglet Gammes n'est visible que pour les ADMIN
                "Exclusions" -> role == "ADMIN" // L'onglet Exclusions n'est visible que pour les ADMIN
                else -> false
            }
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    // S'assurer que l'onglet sélectionné est toujours valide après filtrage
    // Si l'onglet sélectionné précédemment n'est plus dans filteredTabs (par exemple, si l'utilisateur change de rôle),
    // on réinitialise à l'onglet "Général" (index 0 de filteredTabs).
    LaunchedEffect(filteredTabs) {
        if (selectedTab >= filteredTabs.size) {
            selectedTab = 0
        }
        // Ou, si on veut être plus précis, on peut vérifier si l'élément courant existe encore.
        // val currentSelectedTitle = allTabs[selectedTab]
        // if (!filteredTabs.contains(currentSelectedTitle)) {
        //     selectedTab = filteredTabs.indexOf("Général").coerceAtLeast(0)
        // }
    }


    // États section "Général"
    val nom by selectionViewModel.nom.collectAsState()
    val matricule by selectionViewModel.matricule.collectAsState()
    val devMode by selectionViewModel.devModeEnabled.collectAsState()
    val isConnected by selectionViewModel.isOnline.collectAsState()
    val estConnecte = nom.isNotBlank() && matricule.isNotBlank() && role.isNotBlank()
    val dateSansSecondes = remember(BuildConfig.BUILD_TIME) { BuildConfig.BUILD_TIME.take(16) }
    var tapCount by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BaseScreen(
        title = "Paramètres",
        navController = navController,
        viewModel = selectionViewModel,
        showBack = true,
        connectionStatus = isConnected
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                // Utilise filteredTabs ici pour afficher seulement les onglets autorisés
                filteredTabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Affiche le contenu de l'onglet sélectionné
            // Note: On utilise filteredTabs[selectedTab] pour obtenir le titre de l'onglet actuel
            when (filteredTabs[selectedTab]) {
                "Général" -> GeneralSection(
                    nom = nom,
                    matricule = matricule,
                    role = role,
                    estConnecte = estConnecte,
                    dateSansSecondes = dateSansSecondes,
                    devMode = devMode,
                    tapCount = tapCount,
                    onTap = { tapCount++ },
                    onActivateDev = { selectionViewModel.activateDevMode() },
                    onLogoutClick = { showDialog = true },
                    onChangePwd = { navController.navigate("change_password") },
                    navController = navController,
                    selectionViewModel = selectionViewModel,
                    context = context
                )
                "Gammes" -> TypeOperationParamScreen(
                    viewModel = selectionViewModel,
                    navController = navController
                )
                "Exclusions" -> ExclusionsParamSection(
                    selectionViewModel = selectionViewModel,
                    etapeViewModel = etapeViewModel
                )
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
                            selectionViewModel.reset()
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
private fun GeneralSection(
    nom: String,
    matricule: String,
    role: String,
    estConnecte: Boolean,
    dateSansSecondes: String,
    devMode: Boolean,
    tapCount: Int,
    onTap: () -> Unit,
    onActivateDev: () -> Unit,
    onLogoutClick: () -> Unit,
    onChangePwd: () -> Unit,
    navController: NavController,
    selectionViewModel: SelectionViewModel,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_parametres),
            contentDescription = "Logo",
            modifier = Modifier.size(160.dp)
        )
        Spacer(Modifier.height(24.dp))
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
                .padding(vertical = 8.dp)
                .clickable {
                    onTap()
                    if (tapCount >= 7 && !devMode) {
                        onActivateDev()
                        Toast.makeText(context, "🧪 Mode développeur activé", Toast.LENGTH_SHORT).show()
                    }
                }
        )
        if (devMode) {
            TextButton(onClick = { navController.navigate("devtools") }) {
                Text("🧪 Outils développeur")
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Se déconnecter")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onChangePwd,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
        ) {
            Text("Changer le mot de passe")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
    }
}