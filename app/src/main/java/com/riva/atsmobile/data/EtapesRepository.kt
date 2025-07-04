// file: app/src/main/java/com/riva/atsmobile/data/EtapesRepository.kt
package com.riva.atsmobile.data

import com.riva.atsmobile.model.*
import com.riva.atsmobile.network.ATSApiService

class EtapesRepository(baseUrl: String) {
    private val api = ATSApiService.create(baseUrl)

    suspend fun fetchAll()                 = api.getEtapes()
    suspend fun fetchById(id: Int)         = api.getEtapeById(id)
    suspend fun create(dto: EtapeCreateDto)     = api.createEtape(dto)
    suspend fun update(id: Int, dto: EtapeUpdateDto) = api.updateEtape(id, dto)
    suspend fun validate(dto: EtapeValidationDto)    = api.validerEtape(dto)
    suspend fun unvalidate(dto: EtapeValidationDto)  = api.devaliderEtape(dto)
}
