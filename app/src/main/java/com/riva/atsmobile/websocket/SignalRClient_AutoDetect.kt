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

    var onConnected: (() -> Unit)? = null
    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null
    var onReceiveGammesError: ((String) -> Unit)? = null

    private val gson = Gson()
    private val resolvedUrl: String by lazy {
        ApiConfig.getBaseUrl(context)
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws"
    }

    fun connect() {
        if (connection?.connectionState == HubConnectionState.CONNECTED) return

        connection = HubConnectionBuilder.create(resolvedUrl).build()

        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Connexion fermée${error?.message?.let { ": $it" } ?: ""}")
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000)
                connect()
            }
        }

        connection?.apply {
            on("ReceiveGammes", { payload: String ->
                Log.d("SignalR-RAW", "RAW JSON payload: $payload")
                val type = object : TypeToken<List<Gamme>>() {}.type
                val list: List<Gamme> = gson.fromJson(payload, type)
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")
                // Après avoir effectivement ouvert la connexion
                // on envoie d'abord le login, puis on notifie
                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé : $matricule")

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

    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }

    fun invokeGetLatestGammes(min: Double, max: Double) {
        val conn = connection
        if (conn?.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "📤 Invoke GetLatestGammes($min, $max)")
            conn.send("GetLatestGammes", min, max)
        } else {
            onConnected = { invokeGetLatestGammes(min, max) }
        }
    }

    fun connectAndFetchGammes(min: Double, max: Double) {
        // onConnected ne lancera pas avant le Login
        onConnected = { invokeGetLatestGammes(min, max) }
        connect()
    }
}
