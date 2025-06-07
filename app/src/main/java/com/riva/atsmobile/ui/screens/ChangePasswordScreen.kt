package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riva.atsmobile.api.changePassword
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(viewModel: SelectionViewModel, navController: NavController) {
    var ancien by remember { mutableStateOf("") }
    var nouveau by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val matriculeState = viewModel.matricule.collectAsState().value
    val devMode = viewModel.devModeEnabled.collectAsState().value
    val isConnected by viewModel.isOnline.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Matricule utilisé : réel ou "DEV"
    val matriculeEffectif = remember(matriculeState, devMode) {
        if (matriculeState.isNotBlank()) matriculeState else if (devMode) "DEV" else ""
    }

    BaseScreen(
        title = "Changement du mot de passe",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Matricule : $matriculeEffectif", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))

                PasswordField("Ancien mot de passe", ancien) { ancien = it }
                Spacer(modifier = Modifier.height(12.dp))

                PasswordField("Nouveau mot de passe", nouveau) { nouveau = it }
                Spacer(modifier = Modifier.height(12.dp))

                PasswordField("Confirmer le mot de passe", confirmation) { confirmation = it }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (nouveau != confirmation) {
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Les mots de passe ne correspondent pas.")
                            }
                            return@Button
                        }

                        scope.launch {
                            if (matriculeEffectif.isBlank()) {
                                snackbarHostState.showSnackbar("❌ Connexion requise pour changer le mot de passe.")
                                return@launch
                            }

                            val result = changePassword(context, matriculeEffectif, ancien, nouveau)
                            result.onSuccess {
                                snackbarHostState.showSnackbar("✅ $it")
                                navController.popBackStack()
                            }.onFailure {
                                snackbarHostState.showSnackbar("❌ ${it.message ?: "Erreur inconnue."}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                ) {
                    Text("Valider")
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 20) onValueChange(it) },
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(52.dp)
    )
}
