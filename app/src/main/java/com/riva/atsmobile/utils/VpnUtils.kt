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

    // Vérification de la permission ACCESS_FINE_LOCATION
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        Log.e(TAG, "Permission ACCESS_FINE_LOCATION non accordée")
        return false
    }

    val info = wifiManager.connectionInfo
    var ssid = info.ssid
    // Supprime les guillemets éventuels
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

fun lancerVpnCisco(context: Context, host: String, profile: String) {
    val uri = "anyconnect://connect?host=$host&profile=$profile"
    Log.d(TAG, "→ lancerVpnCisco(): uri = $uri")

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        // Ciblage explicite du package Cisco AnyConnect
        setPackage("com.cisco.anyconnect.vpn.android.avf")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val resolved = intent.resolveActivity(context.packageManager)
    Log.d(TAG, "   • resolveActivity = $resolved")
    if (resolved != null) {
        Log.i(TAG, "Démarrage Cisco AnyConnect")
        context.startActivity(intent)
    } else {
        Log.e(TAG, "Cisco AnyConnect non résolu par resolveActivity")
        Toast.makeText(
            context,
            "Cisco AnyConnect introuvable. Veuillez l’installer depuis le Play Store.",
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
