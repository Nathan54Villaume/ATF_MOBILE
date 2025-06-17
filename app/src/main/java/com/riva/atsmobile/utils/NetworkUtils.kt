package com.riva.atsmobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Utilitaires réseau pour vérifier la connexion Wi‑Fi et VPN.
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"

    @SuppressLint("MissingPermission")
    fun isOnAllowedWifi(context: Context, allowedSsids: List<String>): Boolean {
        Log.d(TAG, "→ isOnAllowedWifi(): début")
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
            return false
        }

        val ssidRaw = wifiManager.connectionInfo.ssid
        val ssid = ssidRaw.trim('"')
        Log.d(TAG, "   • SSID actuel = $ssid")
        val allowed = ssid in allowedSsids
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
        Log.d(TAG, "→ isConnectedToWifi(): début de la vérification Wi‑Fi")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val result = cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        Log.d(TAG, "← isConnectedToWifi() = $result")
        return result
    }
}
