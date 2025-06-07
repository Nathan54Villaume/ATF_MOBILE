package com.riva.atsmobile.navigation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsScreen(navController: NavController, viewModel: SelectionViewModel) {
    val context = LocalContext.current
    var apiUrl by remember { mutableStateOf(ApiConfig.getBaseUrl(context)) }
    val isConnected = viewModel.isOnline.collectAsState().value
    val devModeEnabled by viewModel.devModeEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    BaseScreen(
        title = "🧪 Outils développeur",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🔧 Configuration API",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("URL de l'API") },
                modifier = Modifier.fillMaxWidth(0.9f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { apiUrl = "http://10.0.2.2:5258" }) {
                    Text("Émulateur", fontSize = 12.sp)
                }
                Button(onClick = { apiUrl = "http://10.250.13.4:8088" }) {
                    Text("Serveur", fontSize = 12.sp)
                }
                Button(onClick = { apiUrl = "http://10.250.13.121:5058" }) {
                    Text("Usine", fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    ApiConfig.setBaseUrl(context, apiUrl)
                    Toast.makeText(context, "✅ URL API enregistrée", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Enregistrer", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val testUrl = URL("$apiUrl/api/ping")
                            val conn = testUrl.openConnection() as HttpURLConnection
                            conn.requestMethod = "GET"
                            conn.connectTimeout = 3000
                            conn.readTimeout = 3000
                            conn.connect()

                            val code = conn.responseCode
                            val msg = if (code == 200) "API disponible (code 200)" else "Code : $code"
                            withContext(Dispatchers.Main) {
                                sendApiNotification(context, code == 200, msg)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                sendApiNotification(context, false, "Erreur : ${e.message}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Tester connexion API", fontSize = 14.sp)
            }

            if (devModeEnabled) {
                OutlinedButton(
                    onClick = {
                        viewModel.setDevMode(false)
                        Toast.makeText(context, "🧪 Mode développeur désactivé", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text("Quitter le mode développeur", fontSize = 14.sp)
                }
            }
        }
    }
}

fun sendApiNotification(context: Context, success: Boolean, message: String) {
    val channelId = "API_TEST_CHANNEL"
    val notificationId = 1001

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Tests API"
        val descriptionText = "Notifications liées aux tests de connexion API"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(if (success) "✅ API OK" else "❌ Échec API")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
    }
}
