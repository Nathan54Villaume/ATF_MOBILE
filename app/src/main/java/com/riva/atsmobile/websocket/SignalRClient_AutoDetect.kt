package com.riva.atsmobile.websocket

import android.content.Context
import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null
    var onMessage: ((String) -> Unit)? = null

    private val resolvedUrl: String by lazy {
        val baseUrl = ApiConfig.getBaseUrl(context) // ✅ corrigé ici
        val url = baseUrl.replace("http://", "ws://").trimEnd('/') + "/ws"
        Log.d("SignalR", "✅ URL WebSocket utilisée : $url")
        url
    }

    fun connect(matricule: String = "N1234") {
        if (connection != null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection = HubConnectionBuilder.create(resolvedUrl).build()

                connection?.on("NouvelleNotification", { message: String ->
                    Log.d("SignalR", "📨 Reçu : $message")
                    onMessage?.invoke(message)
                }, String::class.java)

                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")

                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé : $matricule")

            } catch (e: Exception) {
                Log.e("SignalR", "❌ Erreur connexion : ${e.message}")
            }
        }
    }

    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }
}
