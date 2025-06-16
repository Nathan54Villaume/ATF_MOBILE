package com.riva.atsmobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.riva.atsmobile.MainActivity
import com.riva.atsmobile.vpn.MyVpnService

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    @SuppressLint("MissingPermission")
    fun isOnAllowedWifi(context: Context, allowedSsids: List<String>): Boolean {
        Log.d(TAG, "→ isOnAllowedWifi(): début")
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
            return false
        }

        var ssid = wifiManager.connectionInfo.ssid
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        Log.d(TAG, "   • SSID actuel = $ssid")
        val allowed = allowedSsids.contains(ssid)
        Log.d(TAG, "← isOnAllowedWifi() = $allowed")
        return allowed
    }

    fun isVpnConnected(context: Context): Boolean {
        Log.d(TAG, "→ isVpnConnected(): début de la vérification VPN")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
        Log.d(TAG, "← isVpnConnected() = $connected")
        return connected
    }

    fun isConnectedToWifi(context: Context): Boolean {
        Log.d(TAG, "→ isConnectedToWifi(): début de la vérification Wi-Fi")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val result = cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        Log.d(TAG, "← isConnectedToWifi() = $result")
        return result
    }

    /** Lancement du VPN intégré via VpnService.prepare() */
    fun startAlwaysOnVpn(context: Context) {
        val intent = VpnService.prepare(context)
        if (intent != null && context is MainActivity) {
            context.requestVpnPermission.launch(intent)
        } else {
            // permission déjà accordée ou hors MainActivity
            Intent(context, MyVpnService::class.java).also {
                context.startForegroundService(it)
            }
        }
    }

    /** Vérifie SSID/VPN et démarre le VPN si besoin */
    fun verifierConnexionEtEventuellementLancerVpn(
        context: Context,
        allowedSsids: List<String> = listOf("Riva_Usine", "Elmec_Factory")
    ) {
        when {
            isVpnConnected(context) ->
                Log.i(TAG, "Déjà connecté au VPN")
            isConnectedToWifi(context) && isOnAllowedWifi(context, allowedSsids) ->
                Log.i(TAG, "Wi-Fi autorisé, pas de VPN nécessaire")
            else -> {
                Log.i(TAG, "Démarrage VPN intégré Always-On")
                startAlwaysOnVpn(context)
            }
        }
    }
}
