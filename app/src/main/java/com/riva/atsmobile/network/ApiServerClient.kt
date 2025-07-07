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

    @POST("api/Etapes/valider")
    suspend fun validerEtape(
        @Body dto: EtapeValidationDto
    ): Response<Void>

    @POST("api/Etapes/devalider")
    suspend fun devaliderEtape(
        @Body dto: EtapeValidationDto
    ): Response<Void>

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
