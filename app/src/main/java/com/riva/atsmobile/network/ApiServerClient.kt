// file: app/src/main/java/com/riva/atsmobile/network/ApiServerClient.kt
package com.riva.atsmobile.network

import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.EtapeCreateDto
import com.riva.atsmobile.model.EtapeUpdateDto
import com.riva.atsmobile.model.EtapeValidationDto
import com.riva.atsmobile.model.Gamme
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiServerClient {
    @GET("api/Etapes")
    suspend fun getEtapes(): Response<List<Etape>>

    @GET("api/Etapes/{id}")
    suspend fun getEtapeById(@Path("id") id: Int): Response<Etape>

    @POST("api/Etapes")
    suspend fun createEtape(@Body dto: EtapeCreateDto): Response<Void>

    @PUT("api/Etapes/{id}")
    suspend fun updateEtape(
        @Path("id") id: Int,
        @Body dto: EtapeUpdateDto
    ): Response<Void>

    /**
     * Valide une étape pour un opérateur.
     * Le DTO contient maintenant : id_etape, commentaire, description, tempsReel et role.
     */
    @POST("api/Etapes/valider")
    suspend fun validerEtape(
        @Body dto: EtapeValidationDto
    ): Response<Void>

    /**
     * Annule la validation d’une étape pour un opérateur.
     */
    @POST("api/Etapes/devalider")
    suspend fun devaliderEtape(
        @Body dto: EtapeValidationDto
    ): Response<Void>

    /**
     * Réinitialise la session (toutes les validations) côté serveur.
     */
    @POST("api/Etapes/reset-session")
    suspend fun resetSession(): Response<Void>

    @GET("api/Gammes")
    suspend fun getGammes(): Response<List<Gamme>>

    companion object {
        fun create(baseUrl: String): ApiServerClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiServerClient::class.java)
        }
    }
}
