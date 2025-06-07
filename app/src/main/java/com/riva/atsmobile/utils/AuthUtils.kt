package com.riva.atsmobile.utils

import android.content.Context
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.viewmodel.SelectionViewModel

fun handleOfflineFallback(
    context: Context,
    matricule: String,
    motDePasse: String,
    viewModel: SelectionViewModel,
    navController: NavController,
    onError: (String) -> Unit
) {
    if (LocalAuthManager.isValidOffline(context, matricule, motDePasse)) {
        playSoundWithVibration(context, R.raw.logon)
        val userInfo = LocalAuthManager.loadUserInfo(context)
        if (userInfo != null) {
            viewModel.setMatricule(userInfo.matricule)
            viewModel.setNom(userInfo.nom)
            viewModel.setRole(userInfo.role)
        } else {
            viewModel.setMatricule(matricule)
            viewModel.setNom("Mode hors ligne")
            viewModel.setRole("OPERATEUR")
        }
        onError("")
        navController.navigate("home") {
            popUpTo("login") { inclusive = true }
        }
    } else {
        onError("Connexion impossible. Aucune donnée locale trouvée.")
    }
}
