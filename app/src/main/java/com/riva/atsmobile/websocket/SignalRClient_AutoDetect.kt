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
import kotlinx.coroutines.*

class SignalRClientAutoDetect(
    private val context: Context,
    private val matricule: String
) {
    private var connection: HubConnection? = null

    /** Scope unique pour tous les coroutines internes */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null
    var onReceiveGammesError: ((String) -> Unit)? = null

    private val gson = Gson()
    private val resolvedUrl: String by lazy {
        ApiConfig.getBaseUrl(context)
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws"
    }

    /**
     * Démarre ou redémarre la connexion SignalR.
     */
    fun connect() {
        // Si déjà connecté, on ne recrée pas tout
        if (connection?.connectionState == HubConnectionState.CONNECTED) return

        // (Re)création du HubConnection
        connection = HubConnectionBuilder.create(resolvedUrl).build()

        // En cas de fermeture, on retente après 2s
        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Fermeture connexion${error?.message?.let { ": $it" } ?: ""}")
            scope.launch {
                delay(2_000)
                connect()
            }
        }

        // Enregistrement des handlers
        connection?.apply {
            on("ReceiveGammes", { payload: String ->
                Log.d("SignalR-RAW", payload)
                val type = object : TypeToken<List<Gamme>>() {}.type
                val list: List<Gamme> = gson.fromJson(payload, type)
                // Notification sur le thread Main
                CoroutineScope(Dispatchers.Main).launch {
                    onReceiveGammes?.invoke(list)
                }
            }, String::class.java)

            on("ReceiveGammesError", { err: String ->
                Log.e("SignalR", "❌ ReceiveGammesError: $err")
                CoroutineScope(Dispatchers.Main).launch {
                    onReceiveGammesError?.invoke(err)
                }
            }, String::class.java)
        }

        // Démarrage de la connexion
        scope.launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")

                // 1) on envoie le login
                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé : $matricule")
            } catch (e: Exception) {
                Log.e("SignalR", "❌ Échec connexion : ${e.message}", e)
                delay(2_000)
                connect()
            }
        }
    }

    /** Arrêt manuel de la connexion */
    fun disconnect() {
        scope.launch {
            connection?.stop()
            connection = null
            Log.d("SignalR", "🔌 Déconnecté manuellement")
        }
    }

    /**
     * Envoie directement GetLatestGammes.
     * Appeler **après** avoir appelé `connect()`.
     * Si la connexion n'est pas encore active, on réessaie automatiquement après 500 ms.
     */
    fun invokeGetLatestGammes(min: Double, max: Double) {
        scope.launch {
            // attente fine si nécessaire
            while (connection?.connectionState != HubConnectionState.CONNECTED) {
                delay(500)
            }
            Log.d("SignalR", "📤 Invoke GetLatestGammes($min, $max)")
            connection?.send("GetLatestGammes", min, max)
        }
    }

    /**
     * Confort : connecte puis, dès que le login est envoyé, appelle GetLatestGammes.
     */
    fun connectAndFetchGammes(min: Double, max: Double) {
        connect()
        // on lance le fetch de façon asynchrone
        invokeGetLatestGammes(min, max)
    }
}
