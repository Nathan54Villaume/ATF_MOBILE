package com.riva.atsmobile.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.riva.atsmobile.model.Etape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiAutomateClient {

    private val client = OkHttpClient()
    private const val TAG = "API_AUTOMATE"
    private const val ENDPOINT_READ_MULTIPLE = "/api/automate/read-multiple"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /**
     * Lit plusieurs adresses regroupées par catégories via l'API
     *
     * @param dbMap Map regroupant les adresses à lire (clé = groupe logique, valeur = liste d'adresses DB)
     * @param baseUrl URL de base (ex: http://10.250.13.4:8080)
     * @return Map identique à dbMap, mais contenant les valeurs décodées.
     */
    suspend fun fetchGroupedValues(
        dbMap: Map<String, List<String>>,
        baseUrl: String
    ): Map<String, Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JsonArray().apply {
                dbMap.values.flatten().forEach { add(it) }
            }

            val requestBody = jsonArray.toString().toRequestBody(JSON_MEDIA)
            val fullUrl = baseUrl.trimEnd('/') + ENDPOINT_READ_MULTIPLE

            Log.d(TAG, "POST → $fullUrl\nPayload → $jsonArray")

            val request = Request.Builder()
                .url(fullUrl)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${response.code} → $rawBody")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Échec HTTP: code ${response.code}")
                    return@withContext emptyMap()
                }

                val parsed = JSONObject(rawBody)

                return@withContext dbMap.mapValues { (_, addresses) ->
                    addresses.associateWith { addr ->
                        val rawVal = parsed.opt(addr)
                        decodeAutomateValue(addr, rawVal)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur réseau ou parsing", e)
            emptyMap()
        }
    }

    /**
     * Décode une valeur renvoyée par l'automate selon le type attendu (bool, byte, short, float, etc.)
     */
    private fun decodeAutomateValue(addr: String, rawVal: Any?): Any {
        return when (rawVal) {
            is Boolean -> rawVal
            is Double -> {
                // On récupère le suffixe après le dernier point, ex. "DBD10", puis on isole les lettres "DBD"
                val fieldType = addr
                    .substringAfterLast('.')
                    .takeWhile { it.isLetter() }
                    .uppercase()

                when {
                    // CORRECTION : Caster directement en Float si c'est un DBD (Double ou Float de l'API)
                    fieldType == "DBD" -> rawVal.toFloat() // La valeur est déjà un Double, il suffit de la caster en Float
                    fieldType == "DBW" -> rawVal.toInt().toShort()
                    fieldType == "DBB" -> rawVal.toInt().toByte()
                    // si c'est un double entier, on cast en Int
                    rawVal % 1.0 == 0.0 -> rawVal.toInt()
                    else -> rawVal
                }
            }
            is Int -> rawVal
            is String -> if (rawVal.startsWith("Erreur")) "N/A" else rawVal
            null -> "N/A"
            else -> rawVal
        }
    }

    /**
     * Récupère la liste des étapes via l'API.
     *
     * @param baseUrl URL de base (ex: http://10.250.13.4:8080)
     * @return Liste d'étapes (ou vide en cas d'erreur)
     */
    suspend fun getEtapes(
        baseUrl: String
    ): List<Etape> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/api/etapes"
            val fullUrl = baseUrl.trimEnd('/') + endpoint

            Log.d(TAG, "GET → $fullUrl")

            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "HTTP ${response.code} → $body")

                if (!response.isSuccessful) return@withContext emptyList()

                val listType = object : TypeToken<List<Etape>>() {}.type
                return@withContext Gson().fromJson<List<Etape>>(body, listType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement des étapes", e)
            emptyList()
        }
    }
}