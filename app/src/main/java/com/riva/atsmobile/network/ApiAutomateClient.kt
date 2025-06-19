package com.riva.atsmobile.network

import android.util.Log
import com.google.gson.JsonArray
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiAutomateClient {
    private val client = OkHttpClient()
    private const val ENDPOINT_PATH = "/api/automate/read-multiple"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * Envoie en une seule fois toutes les adresses contenues dans dbMap,
     * puis reconstitue une Map identique à celle passée en paramètre.
     *
     * @param dbMap Map où chaque clé regroupe une liste d’adresses à lire.
     * @param baseUrl URL de base (p. ex. issue de ApiConfig.getBaseUrl(context)).
     * @return Map<String, Map<String, Any>> avec les valeurs décodées.
     */
    suspend fun fetchGroupedValues(
        dbMap: Map<String, List<String>>,
        baseUrl: String
    ): Map<String, Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            // 1) Construction du JsonArray contenant toutes les adresses
            val jsonArray = JsonArray().apply {
                dbMap.values.flatten().forEach { add(it) }
            }
            val requestBody = jsonArray.toString().toRequestBody(JSON_MEDIA)

            Log.d("API", "POST vers $baseUrl$ENDPOINT_PATH → $requestBody")

            // 2) Construction et exécution de la requête HTTP
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + ENDPOINT_PATH)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                Log.d("API", "HTTP ${response.code} → $rawBody")

                if (!response.isSuccessful) {
                    Log.e("API", "fetchGroupedValues échoué, code ${response.code}")
                    return@withContext emptyMap()
                }

                val parsed = JSONObject(rawBody)

                // 3) Regroupement des résultats dans la même structure que dbMap
                return@withContext dbMap.mapValues { (_, adresses) ->
                    adresses.associateWith { addr ->
                        val rawVal = parsed.opt(addr)
                        when (rawVal) {
                            is Boolean -> rawVal
                            is Double -> {
                                // JSONObject renvoie les nombres en Double
                                when {
                                    addr.endsWith(".DBD", true) ->
                                        Float.fromBits(rawVal.toInt())
                                    addr.endsWith(".DBW", true) ->
                                        rawVal.toInt().toShort()
                                    addr.endsWith(".DBB", true) ->
                                        rawVal.toInt().toByte()
                                    else ->
                                        // Si c’est un entier déguisé en double, on cast en Int
                                        if (rawVal % 1.0 == 0.0) rawVal.toInt() else rawVal
                                }
                            }
                            is Int    -> rawVal
                            is String -> if (rawVal.startsWith("Erreur")) "N/A" else rawVal
                            null      -> "N/A"
                            else      -> rawVal
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API", "Erreur fetchGroupedValues", e)
            emptyMap()
        }
    }
}
