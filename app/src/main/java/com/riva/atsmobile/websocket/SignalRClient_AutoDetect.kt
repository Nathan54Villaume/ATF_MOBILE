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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Client SignalR pour se connecter au backend WebSocket
 * et récupérer la liste des gammes.
 * Gère manuellement la reconnexion en cas de fermeture.
 */
class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null

    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null
    var onReceiveGammesError: ((String) -> Unit)? = null

    private val gson = Gson()
    private val resolvedUrl: String by lazy {
        ApiConfig.getBaseUrl(context)
            .replace("http://", "ws://").trimEnd('/') + "/ws"
    }

    /**
     * Démarre la connexion ou relance si fermée.
     */
    fun connect() {
        if (connection != null && connection?.connectionState == HubConnectionState.CONNECTED) return

        connection = HubConnectionBuilder
            .create(resolvedUrl)
            .build()

        // Sur fermeture, on planifie une reconnexion
        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Connexion fermée${error?.message?.let { ": $it" } ?: ""}")
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000)
                connect()
            }
        }

        // Handlers
        connection?.apply {
            on("ReceiveGammes", { payload: String ->
                Log.d("SignalR", "📨 Gammes reçues (${payload.length})")
                val type = object : TypeToken<List<Gamme>>() {}.type
                onReceiveGammes?.invoke(gson.fromJson(payload, type))
            }, String::class.java)

            on("ReceiveGammesError", { err: String ->
                Log.e("SignalR", "❌ Erreur gammes: $err")
                onReceiveGammesError?.invoke(err)
            }, String::class.java)
        }

        // Lancement asynchrone
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")
            } catch (e: Exception) {
                Log.e("SignalR", "❌ Connexion échouée: ${e.message}", e)
                // en cas d'erreur, replanifier reconnexion
                delay(2000)
                connect()
            }
        }
    }

    /** Arrêt manuel de la connexion */
    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }

    /** Envoie GetLatestGammes */
    fun invokeGetLatestGammes(min: Double, max: Double) {
        connection?.send("GetLatestGammes", min, max)?.also {
            Log.d("SignalR", "📤 GetLatestGammes($min, $max)")
        }
    }

    /** Connecte et fetch directement les gammes */
    fun connectAndFetchGammes(min: Double, max: Double) {
        connect()
        onReceiveGammes?.let {
            // attendre connexion puis appeler
            invokeGetLatestGammes(min, max)
        }
    }
}
