package com.riva.atsmobile.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ApiAutomateClient {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://10.250.13.4:8088/api/automate/read-multiple"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchGroupedValues(
        dbMap: Map<String, List<String>>
    ): Map<String, Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val allAddresses = dbMap.values.flatten()
            val payload = json.encodeToString(
                ListSerializer(String.serializer()),
                allAddresses
            )
            Log.d("API", "Payload → $payload")

            val request = Request.Builder()
                .url(BASE_URL)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                Log.d("API", "Response → $bodyStr")

                val parsed = json.parseToJsonElement(bodyStr).jsonObject

                return@withContext dbMap.mapValues { (_, addresses) ->
                    addresses.associateWith { addr ->
                        val primitive = parsed[addr]?.jsonPrimitive
                        if (primitive != null && !primitive.content.startsWith("Erreur")) {
                            when {
                                primitive.booleanOrNull != null -> primitive.boolean
                                primitive.doubleOrNull != null -> primitive.double
                                primitive.intOrNull != null -> primitive.int
                                else -> primitive.content
                            }
                        } else {
                            "N/A"
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
