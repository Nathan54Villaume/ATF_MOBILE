package com.riva.atsmobile.utils

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ConnectionStatusBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            isConnected = isNetworkAvailable(context)
            Log.d("DEBUG", "🔄 isNetworkAvailable = $isConnected")
            delay(5000)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ✅ Indicateur temporaire pour debug (affiche le statut même sans erreur)
        Text(
            text = if (isConnected) "✅ CONNECTÉ" else "❌ HORS LIGNE",
            color = if (isConnected) Color.Green else Color.Red,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(4.dp),
            style = MaterialTheme.typography.labelSmall
        )

        if (!isConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Hors ligne", tint = Color.Yellow)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hors ligne — Serveur injoignable",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
