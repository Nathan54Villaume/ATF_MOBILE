package com.riva.atsmobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

private const val TAG = "NetworkUtils"

// 1. Vérifie si le SSID actuel est dans la liste des SSID autorisés
@SuppressLint("MissingPermission")
fun isOnAllowedWifi(context: Context, allowedSsids: List<String>): Boolean {
    Log.d(TAG, "→ isOnAllowedWifi(): début")
    val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Vérification du permission ACCESS_FINE_LOCATION requise pour Android 8+
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
        return false
    }

    val info = wifiManager.connectionInfo
    var ssid = info.ssid
    // Sur certains appareils SSID est encadré de guillemets
    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
        ssid = ssid.substring(1, ssid.length - 1)
    }
    Log.d(TAG, "   • SSID actuel = $ssid")
    val allowed = allowedSsids.any { it == ssid }
    Log.d(TAG, "← isOnAllowedWifi() = $allowed")
    return allowed
}

fun isVpnConnected(context: Context): Boolean {
    Log.d(TAG, "→ isVpnConnected(): début de la vérification VPN")
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val connected = cm.allNetworks.any { network ->
        val hasVpn = cm.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        Log.d(TAG, "   • réseau=$network → hasVpn=$hasVpn")
        hasVpn
    }
    Log.d(TAG, "← isVpnConnected() = $connected")
    return connected
}

fun isConnectedToWifi(context: Context): Boolean {
    Log.d(TAG, "→ isConnectedToWifi(): début de la vérification Wi-Fi")
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    Log.d(TAG, "← isConnectedToWifi() = $hasWifi")
    return hasWifi
}

fun lancerVpnCisco(context: Context, host: String, profile: String? = null) {
    val uri = profile
        ?.let { "anyconnect://connect/?name=$it" }
        ?: "anyconnect://connect/?host=$host"
    Log.d(TAG, "→ lancerVpnCisco(): uri = $uri")

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uri)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        Log.i(TAG, "Démarrage Cisco AnyConnect")
        context.startActivity(intent)
    } else {
        Log.e(TAG, "Cisco AnyConnect non installé")
        Toast.makeText(
            context,
            "Cisco AnyConnect n'est pas installé",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun verifierConnexionEtEventuellementLancerVpn(
    context: Context,
    allowedSsids: List<String> = listOf("Riva_Usine", "Elmec_Factory")
) {
    Log.d(TAG, "→ verifierConnexionEtEventuellementLancerVpn(): début")
    when {
        isVpnConnected(context) -> {
            Log.i(TAG, "Branch: déjà connecté au VPN")
        }
        isConnectedToWifi(context) && isOnAllowedWifi(context, allowedSsids) -> {
            Log.i(TAG, "Branch: Wi-Fi autorisé (${allowedSsids.joinToString()})")
        }
        else -> {
            Log.w(TAG, "Branch: ni VPN ni Wi-Fi autorisé → lancement VPN Cisco")
            lancerVpnCisco(
                context,
                host = "gate.elmec.com/rivagroup",
                profile = "Riva"
            )
        }
    }
    Log.d(TAG, "← verifierConnexionEtEventuellementLancerVpn(): fin")
}
