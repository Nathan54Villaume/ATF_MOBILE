package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.utils.handleOfflineFallback
import com.riva.atsmobile.utils.isNetworkAvailable
import com.riva.atsmobile.utils.playSoundWithVibration
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, viewModel: SelectionViewModel) {
    var matricule by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // 1) définir l'action de connexion en un seul bloc
    val doLogin: () -> Unit = {
        message = "Connexion en cours..."
        scope.launch {
            val reseauDispo = isNetworkAvailable(context)
            if (reseauDispo) {
                val result = viewModel.verifierConnexion(context, matricule, motDePasse)
                result.onSuccess { user ->
                    playSoundWithVibration(context, R.raw.logon)
                    viewModel.setMatricule(user.matricule)
                    viewModel.setNom(user.nom)
                    viewModel.setRole(user.role)
                    message = ""
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }.onFailure {
                    handleOfflineFallback(context, matricule, motDePasse, viewModel, navController) {
                        message = it
                    }
                }
            } else {
                handleOfflineFallback(context, matricule, motDePasse, viewModel, navController) {
                    message = it
                }
            }
        }
    }

    val isLoggedIn = viewModel.nom.collectAsState().value.isNotBlank()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    BaseScreen(
        title = "Connexion",
        viewModel = viewModel,
        navController = navController,
        showBack = false,
        showLogout = false
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo ATS",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            val champModifier = Modifier.widthIn(max = 300.dp)

            OutlinedTextField(
                value = matricule,
                onValueChange = { matricule = it },
                label = { Text("Matricule") },
                modifier = champModifier,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                )
            )

            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it },
                label = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = champModifier,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        doLogin()
                    }
                )
            )

            FilledTonalButton(
                onClick = { doLogin() },
                enabled = matricule.isNotBlank() && motDePasse.isNotBlank(),
                modifier = champModifier.height(50.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Se connecter")
            }

            if (message.isNotBlank()) {
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            TextButton(
                onClick = { navController.navigate("settings") }
            ) {
                Text("⚙️ Options")
            }
        }
    }
}
