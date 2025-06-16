package com.riva.atsmobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.riva.atsmobile.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
        Log.d(TAG, "→ lancerVpnCisco(): début")
        val uri = "anyconnect://connect?host=$host&profile=$profile"
        val component = ComponentName(
            "com.cisco.anyconnect.vpn.android.avf",
            "com.cisco.anyconnect.ui.PrimaryActivity"
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setComponent(component)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val resolved = intent.resolveActivity(context.packageManager)
        if (resolved == null) {
            Log.e(TAG, "Cisco AnyConnect introuvable")
            Toast.makeText(context, "Cisco AnyConnect non installé", Toast.LENGTH_LONG).show()
            return
        }
        context.startActivity(intent)

        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if (isVpnConnected(context)) {
                    Log.i(TAG, "VPN actif détecté, tentative de retour à l'app")
                    bringAppToFront(context)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed(this, 500)
                }
            }
        }, 500)
    }

    private fun bringAppToFront(context: Context) {
        Log.i(TAG, "bringAppToFront(): redémarrage de MainActivity")
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            context.startActivity(this)
        } ?: run {
            Log.e(TAG, "Impossible de récupérer le launch intent de l'app")
        }
    }

    fun verifierConnexionEtEventuellementLancerVpn(
        context: Context,
        allowedSsids: List<String> = listOf("Riva_Usine", "Elmec_Factory")
    ) {
        Log.d(TAG, "→ verifierConnexionEtEventuellementLancerVpn(): début")
        when {
            isVpnConnected(context) -> Log.i(TAG, "Branch: déjà connecté au VPN")
            isConnectedToWifi(context) && isOnAllowedWifi(context, allowedSsids) -> Log.i(TAG, "Branch: Wi-Fi autorisé (${allowedSsids.joinToString()})")
            else -> {
                Log.w(TAG, "Branch: lancement VPN Cisco")
                lancerVpnCisco(context, host = "gate.elmec.com/rivagroup", profile = "Riva")
            }
        }
        Log.d(TAG, "← verifierConnexionEtEventuellementLancerVpn(): fin")
    }

    suspend fun isNetworkAvailable(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "${ApiConfig.getBaseUrl(context)}/api/ping"
                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1000
                    readTimeout = 1000
                    requestMethod = "GET"
                    connect()
                }
                connection.responseCode == HttpURLConnection.HTTP_OK
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception ping API", e)
                false
            }
        }
    }
}
