package com.riva.atsmobile.api

import android.content.Context
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun changePassword(
    context: Context,
    matricule: String,
    ancien: String,
    nouveau: String
): Result<String> {
    val baseUrl = ApiConfig.getBaseUrl(context)
    val url = "$baseUrl/api/auth/change-password"

    val json = """
        {
            "matricule": "$matricule",
            "ancienMotDePasse": "$ancien",
            "nouveauMotDePasse": "$nouveau"
        }
    """.trimIndent()

    val client = OkHttpClient()
    val requestBody = json.toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
        .build()

    return withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    Result.success("✅ Mot de passe modifié avec succès.")
                } else {
                    Result.failure(Exception(body ?: "❌ Erreur inconnue."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
