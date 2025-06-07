package com.riva.atsmobile.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun DevModeBanner(viewModel: SelectionViewModel) {
    val role = viewModel.role.collectAsState().value

    if (role == "ADMIN") {
        Popup(
            alignment = Alignment.TopStart,
            properties = PopupProperties(focusable = false)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Mode développeur actif",
                    tint = Color.White
                )
            }
        }
    }
}
