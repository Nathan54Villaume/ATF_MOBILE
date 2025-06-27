// file: network/ATSApiService.kt
package com.riva.atsmobile.network

import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.Gamme
import retrofit2.http.GET
import retrofit2.http.Query

interface ATSApiService {
    @GET("api/etapes")
    suspend fun getEtapes(): List<Etape>


    @GET("api/gammes")
    suspend fun getGammes(): List<Gamme>

    // Tu peux en ajouter ici facilement :
    // @POST("api/changement") suspend fun envoyerChangement(...): Response
}
