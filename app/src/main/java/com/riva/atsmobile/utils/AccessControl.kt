package com.riva.atsmobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.riva.atsmobile.viewmodel.SelectionViewModel

/**
 * Retourne true si l'utilisateur est en mode développeur ou a le rôle requis.
 */
@Composable
fun hasAccess(viewModel: SelectionViewModel, requiredRole: String? = null): Boolean {
    val role = viewModel.role.collectAsState().value
    val dev = viewModel.devModeEnabled.collectAsState().value

    return dev || (requiredRole == null || role == requiredRole)
}
