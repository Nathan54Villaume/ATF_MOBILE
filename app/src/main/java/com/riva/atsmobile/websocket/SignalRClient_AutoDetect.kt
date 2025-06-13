package com.riva.atsmobile.websocket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null

    /** Handler générique pour les notifications simples */
    var onMessage: ((String) -> Unit)? = null

    /** Spécifique pour la liste de gammes reçue */
    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null

    /** En cas d’erreur serveur sur GetLatestGammes */
    var onReceiveGammesError: ((String) -> Unit)? = null

    private val gson = Gson()

    private val resolvedUrl: String by lazy {
        val baseUrl = ApiConfig.getBaseUrl(context)
        baseUrl.replace("http://", "ws://").trimEnd('/') + "/ws"
    }

    fun connect(matricule: String = "N1234") {
        if (connection != null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection = HubConnectionBuilder.create(resolvedUrl).build()

                // Notification générique
                connection?.on("NouvelleNotification", { message: String ->
                    Log.d("SignalR", "📨 Reçu NouvelleNotification : $message")
                    onMessage?.invoke(message)
                }, String::class.java)

                // Liste de gammes
                connection?.on("ReceiveGammes", { payload: String ->
                    Log.d("SignalR", "📨 Reçu ReceiveGammes")
                    val type = object : TypeToken<List<Gamme>>() {}.type
                    val list: List<Gamme> = gson.fromJson(payload, type)
                    onReceiveGammes?.invoke(list)
                }, String::class.java)

                // Erreur sur GetLatestGammes
                connection?.on("ReceiveGammesError", { errorMsg: String ->
                    Log.e("SignalR", "❌ ReceiveGammesError : $errorMsg")
                    onReceiveGammesError?.invoke(errorMsg)
                }, String::class.java)

                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")

                // Login sur le hub
                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé : $matricule")

            } catch (e: Exception) {
                Log.e("SignalR", "❌ Erreur connexion : ${e.message}", e)
            }
        }
    }

    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }

    /** Demande au serveur la liste des gammes filtrée */
    fun invokeGetLatestGammes(minDiam: Double, maxDiam: Double) {
        connection?.let {
            it.send("GetLatestGammes", minDiam, maxDiam)
            Log.d("SignalR", "📤 Invoke GetLatestGammes($minDiam, $maxDiam)")
        }
    }
}
