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

class SignalRClientAutoDetect(private val context: Context) {

    private var connection: HubConnection? = null

    /** Handler générique pour les notifications simples */
    var onMessage: ((String) -> Unit)? = null

    /** Spécifique pour la liste de gammes reçue */
    var onReceiveGammes: ((List<Gamme>) -> Unit)? = null

    /** En cas d’erreur serveur sur GetLatestGammes */
    var onReceiveGammesError: ((String) -> Unit)? = null

    /** Callback invoqué une fois la connexion établie et le login envoyé */
    var onConnected: (() -> Unit)? = null

    private val gson = Gson()

    private val resolvedUrl: String by lazy {
        val baseUrl = ApiConfig.getBaseUrl(context)
        baseUrl.replace("http://", "ws://").trimEnd('/') + "/ws"
    }

    /**
     * Établit la connexion au Hub, configure les handlers,
     * démarre la connexion, envoie le login, puis appelle onConnected().
     */
    fun connect(matricule: String = "N1234") {
        if (connection != null && connection?.connectionState == HubConnectionState.CONNECTED) return

        connection = HubConnectionBuilder
            .create(resolvedUrl)
            // Pour activer la reconnexion automatique, décommentez si supporté :
            // .withAutomaticReconnect()
            .build()

        // Gestion de la fermeture
        connection?.onClosed { error ->
            Log.d("SignalR", "🔌 Connexion fermée${error?.message?.let { ": $it" } ?: ""}")
        }

        // Notification générique
        connection?.on("NouvelleNotification", { message: String ->
            Log.d("SignalR", "📨 Reçu NouvelleNotification : $message")
            onMessage?.invoke(message)
        }, String::class.java)

        // Liste de gammes
        connection?.on("ReceiveGammes", { payload: String ->
            Log.d("SignalR", "📨 Reçu ReceiveGammes (${payload.length} chars)")
            val type = object : TypeToken<List<Gamme>>() {}.type
            val list: List<Gamme> = gson.fromJson(payload, type)
            onReceiveGammes?.invoke(list)
        }, String::class.java)

        // Erreur sur GetLatestGammes
        connection?.on("ReceiveGammesError", { errorMsg: String ->
            Log.e("SignalR", "❌ ReceiveGammesError : $errorMsg")
            onReceiveGammesError?.invoke(errorMsg)
        }, String::class.java)

        // Démarrage de la connexion (bloquant)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                connection?.start()?.blockingAwait()
                Log.d("SignalR", "✅ Connecté à $resolvedUrl")

                // Envoi du login
                connection?.send("Login", matricule)
                Log.d("SignalR", "📤 Login envoyé : $matricule")

                // Maintenant que tout est prêt, on notifie l'appelant
                onConnected?.invoke()
            } catch (e: Exception) {
                Log.e("SignalR", "❌ Erreur connexion : ${e.message}", e)
            }
        }
    }

    /** Déconnecte proprement */
    fun disconnect() {
        connection?.stop()
        connection = null
        Log.d("SignalR", "🔌 Déconnecté manuellement")
    }

    /** Envoie simplement la requête GetLatestGammes – à appeler après onConnected */
    fun invokeGetLatestGammes(minDiam: Double, maxDiam: Double) {
        connection?.let {
            Log.d("SignalR", "📤 Invoke GetLatestGammes($minDiam, $maxDiam)")
            it.send("GetLatestGammes", minDiam, maxDiam)
        }
    }

    /**
     * Confort : connecte, logue, puis demande directement les gammes dans l'ordre sécurisé.
     */
    fun connectAndFetchGammes(matricule: String, minDiam: Double, maxDiam: Double) {
        onConnected = {
            invokeGetLatestGammes(minDiam, maxDiam)
        }
        connect(matricule)
    }
}
