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
                Log.d("API", "Response brut → $bodyStr")

                val parsed = JSONObject(bodyStr)

                val result = dbMap.mapValues { (_, addresses) ->
                    addresses.associateWith { addr ->
                        val raw = parsed.opt(addr)
                        val interpreted: Any = when {
                            raw is Int && addr.contains(".DBD") -> Float.fromBits(raw)
                            raw is Int && addr.contains(".DBW") -> raw.toShort()
                            raw is Int && addr.contains(".DBB") -> raw.toByte()
                            raw is Int -> raw
                            raw is Boolean -> raw
                            raw is String && raw.startsWith("Erreur") -> "N/A"
                            else -> raw ?: "N/A"
                        }
                        interpreted
                    }
                }

                Log.d("API", "✔ Résultat décodé final → $result")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e("API", "fetchGroupedValues error", e)
            return@withContext emptyMap()
        }
    }
}
