package com.riva.atsmobile.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiAutomateClient {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://10.250.13.4:8088/api/automate/read-multiple"

    suspend fun fetchGroupedValues(
        dbMap: Map<String, List<String>>
    ): Map<String, Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val allAddresses = dbMap.values.flatten()
            val jsonArray = allAddresses.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
            Log.d("API", "Payload → $jsonArray")

            val request = Request.Builder()
                .url(BASE_URL)
                .post(jsonArray.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                Log.d("API", "Response → $bodyStr")

                val parsed = JSONObject(bodyStr)

                return@withContext dbMap.mapValues { (_, addresses) ->
                    addresses.associateWith { addr ->
                        val value = parsed.opt(addr)
                        if (value is String && value.startsWith("Erreur")) {
                            "N/A"
                        } else {
                            value ?: "N/A"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API", "fetchGroupedValues error", e)
            return@withContext emptyMap()
        }
    }
}