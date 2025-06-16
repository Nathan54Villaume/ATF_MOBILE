package com.riva.atsmobile.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast

private const val TAG = "NetworkUtils"

fun isVpnConnected(context: Context): Boolean {
    Log.d(TAG, "→ isVpnConnected(): début de la vérification VPN")
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val connected = cm.allNetworks.any { network ->
        val caps = cm.getNetworkCapabilities(network)
        val hasVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        Log.d(TAG, "   • réseau=$network → hasVpn=$hasVpn")
        hasVpn
    }
    Log.d(TAG, "← isVpnConnected() = $connected")
    return connected
}

fun isConnectedToWifi(context: Context): Boolean {
    Log.d(TAG, "→ isConnectedToWifi(): début de la vérification Wi-Fi")
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork
    Log.d(TAG, "   • activeNetwork = $active")
    val caps = cm.getNetworkCapabilities(active)
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

    val resolved = intent.resolveActivity(context.packageManager)
    Log.d(TAG, "   • resolveActivity = $resolved")
    if (resolved != null) {
        Log.i(TAG, "Démarrage Cisco AnyConnect")
        context.startActivity(intent)
    } else {
        Log.e(TAG, "Cisco AnyConnect non installé, affichage Toast")
        Toast.makeText(
            context,
            "Cisco AnyConnect n'est pas installé",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun verifierConnexionEtEventuellementLancerVpn(context: Context) {
    Log.d(TAG, "→ verifierConnexionEtEventuellementLancerVpn(): début")
    when {
        isVpnConnected(context) -> {
            Log.i(TAG, "Branch: déjà connecté au VPN")
        }
        isConnectedToWifi(context) -> {
            Log.i(TAG, "Branch: connecté en Wi-Fi (réseau local)")
        }
        else -> {
            Log.w(TAG, "Branch: pas de réseau local ni VPN → lancement VPN Cisco")
            lancerVpnCisco(
                context,
                host = "gate.elmec.com/rivagroup",
                profile = "Riva"
            )
        }
    }
    Log.d(TAG, "← verifierConnexionEtEventuellementLancerVpn(): fin")
}
