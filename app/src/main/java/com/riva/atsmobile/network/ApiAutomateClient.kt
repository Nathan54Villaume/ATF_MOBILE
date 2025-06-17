package com.riva.atsmobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ApiAutomateClient {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://10.250.13.4:8088/api/automate/read-multiple"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchGroupedValues(dbMap: Map<String, List<String>>): Map<String, Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val allAddresses = dbMap.values.flatten()
            val requestBody = json.encodeToString(ListSerializer(String.serializer()), allAddresses)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyMap()
            val parsed = json.decodeFromString<JsonObject>(body)

            dbMap.mapValues { (_, addresses) ->
                addresses.associateWith { addr ->
                    val primitive = parsed[addr]?.jsonPrimitive
                    when {
                        primitive == null -> "N/A"
                        primitive.booleanOrNull != null -> primitive.booleanOrNull ?: false
                        primitive.doubleOrNull != null -> primitive.doubleOrNull ?: 0.0
                        primitive.intOrNull != null -> primitive.intOrNull ?: 0
                        else -> primitive.content
                    }
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
