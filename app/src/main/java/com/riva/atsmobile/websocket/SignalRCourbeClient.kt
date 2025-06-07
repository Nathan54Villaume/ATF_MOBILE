package com.riva.atsmobile.websocket

import android.util.Log
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder

class SignalRCourbeClient(
    private val onDataReceived: (String, Float) -> Unit
) {
    private lateinit var hubConnection: HubConnection

    fun connect() {
        hubConnection = HubConnectionBuilder.create("http://10.250.13.121:8088/ws")   // TABLETTE
            .build()

        hubConnection.on("ReceiveVitesse", { data ->
            try {
                val timestamp = data["timestamp"] as String
                val valeur = (data["valeur"] as Double).toFloat()
                onDataReceived(timestamp, valeur)
            } catch (e: Exception) {
                Log.e("SignalRCourbe", "Erreur parsing: ${e.message}")
            }
        }, Map::class.java)

        hubConnection.start()
            .doOnComplete { Log.d("SignalRCourbe", "✅ Connecté") }
            .doOnError { e -> Log.e("SignalRCourbe", "❌ Erreur : ${e.message}") }
            .subscribe()
    }

    fun stop() {
        if (::hubConnection.isInitialized && hubConnection.connectionState.name == "CONNECTED") {
            hubConnection.stop()
        }
    }
}
