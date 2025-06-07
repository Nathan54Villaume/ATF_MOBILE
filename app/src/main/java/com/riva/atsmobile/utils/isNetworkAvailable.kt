package com.riva.atsmobile.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun isNetworkAvailable(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val urlString = "${ApiConfig.getBaseUrl(context)}/api/ping"
            Log.d("DEBUG", "🌐 Test URL = $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            connection.requestMethod = "GET"
            connection.connect()

            val success = connection.responseCode == 200
            Log.d("DEBUG", "✅ API response = ${connection.responseCode}")
            success
        } catch (e: Exception) {
            Log.e("DEBUG", "❌ Exception ping", e)
            false
        }
    }
}
