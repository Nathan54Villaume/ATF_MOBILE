package com.riva.atsmobile.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ServerStatusBanner(modifier: Modifier = Modifier, pingUrl: String = "http://10.250.13.121:5258/api/ping") {
    var isServerOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            isServerOnline = try {
                val connection = URL(pingUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "GET"
                connection.connect()
                connection.responseCode == 200
            } catch (e: Exception) {
                false
            }
            delay(3000)
        }
    }

    if (!isServerOnline) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Red)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📴 Hors ligne — Serveur injoignable",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}
