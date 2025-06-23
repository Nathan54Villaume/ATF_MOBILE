package com.riva.atsmobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ETAPES_CHANG_GAMMES")
data class EtapeEntity(
    @PrimaryKey val idEtape: Int,
    val libelleEtape: String,
    val affectationEtape: String?,
    val roleLog: String?,
    val phaseEtape: String?,
    val dureeEtape: Int?,
    val descriptionEtape: String?,
    val etatEtape: String?,
    val tempsReelEtape: Int?,
    val commentaireEtape1: String?
)
