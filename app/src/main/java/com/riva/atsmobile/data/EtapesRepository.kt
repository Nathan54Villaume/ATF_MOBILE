// file: app/src/main/java/com/riva/atsmobile/data/EtapesRepository.kt
package com.riva.atsmobile.data

import com.google.gson.Gson
import com.riva.atsmobile.data.mappers.buildCreateDto
import com.riva.atsmobile.data.mappers.buildUpdateDto
import com.riva.atsmobile.model.*
import com.riva.atsmobile.network.ApiServerClient

class EtapesRepository(baseUrl: String) {

    private val api = ApiServerClient.create(baseUrl)

    suspend fun fetchAll() = api.getEtapes()
    suspend fun fetchById(id: Int) = api.getEtapeById(id)

    suspend fun create(dto: EtapeCreateDto) = api.createEtape(dto)
    suspend fun update(id: Int, dto: EtapeUpdateDto) = api.updateEtape(id, dto)

    suspend fun validate(dto: EtapeValidationDto) = api.validerEtape(dto)
    suspend fun unvalidate(dto: EtapeValidationDto) = api.devaliderEtape(dto)

    // —— Nouveaux helpers alignés avec tes modèles ——
    suspend fun createFromModel(model: Etape) =
        create(buildCreateDto(model, Gson()))

    suspend fun updateFromModel(model: Etape) =
        update(model.id_Etape, buildUpdateDto(model, Gson()))
}
