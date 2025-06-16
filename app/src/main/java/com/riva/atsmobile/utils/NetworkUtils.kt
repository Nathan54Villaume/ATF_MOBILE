package com.riva.atsmobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

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

        // Utilisation de var pour pouvoir réassigner
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
        val hasWifi = cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        Log.d(TAG, "← isConnectedToWifi() = $hasWifi")
        return hasWifi
    }

    fun lancerVpnCisco(context: Context, host: String, profile: String) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 1) On s'abonne aux événements VPN
        val vpnRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()
        val vpnCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                cm.unregisterNetworkCallback(this)
                // 2) On ramène notre appli au premier plan
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        context.startActivity(this)
                    }
            }
        }
        cm.registerNetworkCallback(vpnRequest, vpnCallback)

        // 3) On lance Cisco AnyConnect
        val uri = "anyconnect://connect?host=$host&profile=$profile"
        Log.d(TAG, "→ lancerVpnCisco(): uri = $uri")

        val component = ComponentName(
            "com.cisco.anyconnect.vpn.android.avf",
            "com.cisco.anyconnect.ui.PrimaryActivity"
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setComponent(component)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolved = intent.resolveActivity(context.packageManager)
        Log.d(TAG, "   • resolveActivity = $resolved")
        if (resolved != null) {
            Log.i(TAG, "Démarrage Cisco AnyConnect")
            context.startActivity(intent)
        } else {
            Log.e(TAG, "Impossible de résoudre l’Intent pour Cisco AnyConnect")
            Toast.makeText(
                context,
                "Cisco AnyConnect introuvable. Vérifiez l’installation.",
                Toast.LENGTH_LONG
            ).show()
            cm.unregisterNetworkCallback(vpnCallback)
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
                lancerVpnCisco(context, host = "gate.elmec.com/rivagroup", profile = "Riva")
            }
        }
        Log.d(TAG, "← verifierConnexionEtEventuellementLancerVpn(): fin")
    }
}
