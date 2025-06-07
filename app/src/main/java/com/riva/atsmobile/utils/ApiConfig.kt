package com.riva.atsmobile.utils

import android.content.Context
import android.os.Build
import android.util.Log

object ApiConfig {
    private const val PREFS_NAME = "ats_prefs"
    private const val KEY_API_URL = "api_url"
    private const val DEFAULT_API_URL_EMULATOR = "http://10.0.2.2:5258"
    private const val DEFAULT_API_URL_SERVER = "http://10.250.13.4:8088"

    private var cachedUrl: String? = null

    private fun isRunningOnEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator")
    }

    fun getBaseUrl(context: Context): String {
        if (cachedUrl != null) return cachedUrl!!

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedUrl = prefs.getString(KEY_API_URL, null)

        val resolvedUrl = storedUrl ?: if (isRunningOnEmulator()) {
            DEFAULT_API_URL_EMULATOR
        } else {
            DEFAULT_API_URL_SERVER
        }

        Log.d("API", "✅ Base URL utilisée : $resolvedUrl")
        cachedUrl = resolvedUrl
        return resolvedUrl
    }

    fun setBaseUrl(context: Context, url: String) {
        cachedUrl = url
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_URL, url).apply()
    }
}
