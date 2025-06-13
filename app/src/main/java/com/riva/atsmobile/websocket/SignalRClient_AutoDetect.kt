package com.riva.atsmobile.websocket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Client SignalR pour se connecter au backend WebSocket,
 * recevoir les notifications et la liste des gammes.
 */
class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null

    /** Notification générique du serveur */
    var onMessage: ((String) -> Unit)? = null

    /** Callback pour la liste de gammes reçue */
    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null

    /** Callback pour les erreurs lors de la récupération des gammes */
    var onReceiveGammesError: ((String) -> Unit)? = null

    /** Invoked après envoi du login et prêt à envoyer les appels métier */
    var onConnected: (() -> Unit)? = null

    private val gson = Gson()

    // URL WebSocket basée sur l'API HTTP
    private val resolvedUrl: String by lazy {
        val baseUrl = ApiConfig.getBaseUrl(context)
        baseUrl.replace("http://", "ws://").trimEnd('/') + "/ws"
    }

    /**
     * Établit la connexion au Hub, configure les handlers,
     * démarre la connexion, envoie le login, puis onConnected().
     */
    fun connect(matricule: String = "N1234") {
        // Ne créer qu'une fois si déjà connecté
        if (connection != null && connection?.connectionState == HubConnectionState.CONNECTED) return

        connection = HubConnectionBuilder
            .create(resolvedUrl)
            .build()

        // Log de fermeture
        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Connexion fermée${error?.message?.let { ": $it" } ?: ""}")
        }

        // Handlers
        connection?.apply {
            on("NouvelleNotification", { msg: String ->
                Log.d("SignalR", "📨 Notification: $msg")
                onMessage?.invoke(msg)
            }, String::class.java)

            on("ReceiveGammes", { payload: String ->
                Log.d("SignalR", "📨 Gammes reçues (${payload.length} chars)")
                val type = object : TypeToken<List<Gamme>>() {}.type
                onReceiveGammes?.invoke(gson.fromJson(payload, type))
            }, String::class.java)

            on("ReceiveGammesError", { err: String ->
                Log.e("SignalR", "❌ Erreur gammes: $err")
                onReceiveGammesError?.invoke(err)
            }, String::class.java)
        }

        // Démarrage asynchrone
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")

                // Envoi du login
                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé: $matricule")

                // Callback ready
                onConnected?.invoke()
            } catch (e: Exception) {
                Log.e("SignalR", "❌ Erreur de connexion: ${e.message}", e)
            }
        }
    }

    /** Coupe la connexion du Hub */
    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }

    /** Envoie la requête GetLatestGammes(min, max) */
    fun invokeGetLatestGammes(minDiam: Double, maxDiam: Double) {
        connection?.let {
            Log.d("SignalR", "📤 Appel GetLatestGammes($minDiam, $maxDiam)")
            it.send("GetLatestGammes", minDiam, maxDiam)
        }
    }

    /** Connecte puis appelle directement InvokeGetLatestGammes */
    fun connectAndFetchGammes(matricule: String, min: Double, max: Double) {
        onConnected = {
            invokeGetLatestGammes(min, max)
        }
        connect(matricule)
    }
}
