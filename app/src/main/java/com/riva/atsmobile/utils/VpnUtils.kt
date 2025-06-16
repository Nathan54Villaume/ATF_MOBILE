package com.riva.atsmobile.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast

fun isVpnConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.allNetworks.any {
        cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

fun lancerVpnCisco(context: Context, host: String, profile: String? = null) {
    val uri = if (profile != null)
        "anyconnect://connect/?name=$profile"
    else
        "anyconnect://connect/?host=$host"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uri)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Cisco AnyConnect n'est pas installé", Toast.LENGTH_LONG).show()
    }
}

fun verifierConnexionEtEventuellementLancerVpn(context: Context) {
    when {
        isVpnConnected(context) -> {
            // Déjà connecté au VPN
        }
        isConnectedToWifi(context) -> {
            // Connecté au réseau local
        }
        else -> {
            // Aucun réseau local ni VPN actif → déclencher la connexion VPN Cisco
            lancerVpnCisco(context, host = "vpn.sam.local", profile = "Riva")
        }
    }
}
