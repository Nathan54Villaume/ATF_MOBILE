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

class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null

    /** Callback quand la connexion devient active */
    var onConnected: (() -> Unit)? = null

    /** Reçoit la liste de gammes */
    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null

    /** Reçoit une erreur lors de la récupération des gammes */
    var onReceiveGammesError: ((String) -> Unit)? = null

    private val gson = Gson()
    private val resolvedUrl: String by lazy {
        ApiConfig.getBaseUrl(context)
            .replace("http://", "ws://").trimEnd('/') + "/ws"
    }

    /**
     * Démarre ou redémarre la connexion SignalR.
     */
    fun connect() {
        if (connection?.connectionState == HubConnectionState.CONNECTED) return

        connection = HubConnectionBuilder.create(resolvedUrl).build()

        // En cas de fermeture, on retente la connexion après 2s
        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Connexion fermée${error?.message?.let { ": $it" } ?: ""}")
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000)
                connect()
            }
        }

        // Handlers de réception
        connection?.apply {
            on("ReceiveGammes", { payload: String ->
                // 1) Log du JSON brut
                Log.d("SignalR-RAW", "RAW JSON payload: $payload")

                // 2) Parsing
                val type = object : TypeToken<List<Gamme>>() {}.type
                val list: List<Gamme> = gson.fromJson(payload, type)

                // 3) Invocation sur le Main thread
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

        // Démarrage asynchrone
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")
                // Callback onConnected sur le Main thread
                withContext(Dispatchers.Main) {
                    onConnected?.invoke()
                }
            } catch (e: Exception) {
                Log.e("SignalR", "❌ Connexion échouée: ${e.message}", e)
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

    /**
     * Envoie GetLatestGammes au hub.
     * Si la connexion n’est pas encore active, différer jusqu’à onConnected.
     */
    fun invokeGetLatestGammes(min: Double, max: Double) {
        val conn = connection
        if (conn?.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "📤 Invoke GetLatestGammes($min, $max)")
            conn.send("GetLatestGammes", min, max)
        } else {
            // Différer l’appel
            onConnected = {
                Log.d("SignalR", "📤 (post-connect) Invoke GetLatestGammes($min, $max)")
                connection?.send("GetLatestGammes", min, max)
            }
        }
    }

    /**
     * Confort : connecte + fetch.
     * Se base sur onConnected pour déclencher le fetch.
     */
    fun connectAndFetchGammes(min: Double, max: Double) {
        onConnected = { invokeGetLatestGammes(min, max) }
        connect()
    }
}
