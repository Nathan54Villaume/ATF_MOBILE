package com.riva.atsmobile.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log

private const val TAG = "MyVpnService"
private const val CHANNEL_ID = "VPN_SERVICE_CHANNEL"

class MyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildForegroundNotification()
        // Démarre le service en foreground
        startForeground(1, notification)
        Log.i(TAG, "MyVpnService créé")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Démarrage du VPN intégré Always-On")
        establishVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        teardownVpn()
        Log.i(TAG, "MyVpnService détruit")
        super.onDestroy()
    }

    private fun establishVpn() {
        val builder = Builder()
            .setSession("ATF Mobile VPN")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)

        vpnInterface?.close()
        vpnInterface = builder.establish()
        if (vpnInterface != null) {
            Log.i(TAG, "Interface VPN établie: $vpnInterface")
        } else {
            Log.e(TAG, "Échec de l'établissement du VPN")
        }
    }

    private fun teardownVpn() {
        vpnInterface?.close()
        vpnInterface = null
        Log.i(TAG, "Interface VPN fermée")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ATF Mobile VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Service VPN intégré Always-On" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("ATF Mobile VPN actif")
            .setContentText("Votre connexion passe via le VPN intégré.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        // notification persistante
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        return notification
    }
}
