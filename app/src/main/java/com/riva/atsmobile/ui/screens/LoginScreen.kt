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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.navigation.Routes
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.utils.isNetworkAvailable
import com.riva.atsmobile.utils.playSoundWithVibration
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: SelectionViewModel
) {
    // États locaux pour les champs utilisateur
    var matricule by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val scrollState  = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Action de connexion déclenchée par le bouton
    val doLogin = {
        message = "Connexion en cours..."
        scope.launch {
            if (isNetworkAvailable(context)) {
                viewModel.verifierConnexion(context, matricule, motDePasse)
                    .onSuccess { user ->
                        playSoundWithVibration(context, R.raw.logon)
                        viewModel.setMatricule(user.matricule)
                        viewModel.setNom(user.nom)
                        viewModel.setRole(user.role)
                        message = ""
                        // navigation immédiate vers Home
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    }
                    .onFailure {
                        message = "Connexion impossible : identifiants incorrects."
                    }
            } else {
                message = "Connexion impossible : réseau indisponible."
            }
        }
    }

    BaseScreen(
        title         = "Connexion",
        viewModel     = viewModel,
        navController = navController,
        showBack      = false,
        showLogout    = false
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            Image(
                painter            = painterResource(id = R.drawable.logo),
                contentDescription = "Logo ATS",
                modifier           = Modifier.size(120.dp),
                contentScale       = ContentScale.Fit
            )

            val champModifier = Modifier.widthIn(max = 300.dp)

            OutlinedTextField(
                value               = matricule,
                onValueChange       = { matricule = it },
                label               = { Text("Matricule") },
                singleLine          = true,
                modifier            = champModifier,
                keyboardOptions     = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions     = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            OutlinedTextField(
                value               = motDePasse,
                onValueChange       = { motDePasse = it },
                label               = { Text("Mot de passe") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine          = true,
                modifier            = champModifier,
                keyboardOptions     = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions     = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        doLogin()
                    }
                )
            )

            FilledTonalButton(
                onClick  = { doLogin() },
                enabled  = matricule.isNotBlank() && motDePasse.isNotBlank(),
                modifier = champModifier.height(50.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Se connecter")
            }

            if (message.isNotBlank()) {
                Text(
                    text  = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(onClick = { navController.navigate(Routes.Settings) }) {
                Text("⚙️ Options")
            }
        }
    }
}
