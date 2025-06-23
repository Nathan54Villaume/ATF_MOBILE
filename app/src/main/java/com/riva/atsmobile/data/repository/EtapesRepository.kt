package com.riva.atsmobile.data.repository

import com.riva.atsmobile.data.local.EtapesDao
import com.riva.atsmobile.data.local.EtapeEntity

class EtapesRepository(
    private val dao: EtapesDao
) {

    /** Récupère toutes les étapes depuis la base */
    suspend fun getAllSteps(): List<EtapeEntity> =
        dao.getAll()

    /** Récupère une étape par son ID */
    suspend fun getStepById(id: Int): EtapeEntity? =
        dao.getById(id)

    /** Insère ou remplace une liste d’étapes */
    suspend fun insertAll(steps: List<EtapeEntity>) =
        dao.insertAll(steps)

    /** Met à jour une étape existante */
    suspend fun updateStep(step: EtapeEntity) =
        dao.update(step)

    /** Supprime une étape */
    suspend fun deleteStep(step: EtapeEntity) =
        dao.delete(step)
}
