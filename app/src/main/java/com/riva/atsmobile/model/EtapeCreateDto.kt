// file: app/src/main/java/com/riva/atsmobile/model/EtapeCreateDto.kt
package com.riva.atsmobile.model

data class EtapeCreateDto(
    val libelle_Etape: String?,
    val affectation_Etape: String?,
    val role_Log: String?,
    val phase_Etape: String?,
    val duree_Etape: Int?,
    val description_Etape: String?,
    val etat_Etape: String?,
    val temps_Reel_Etape: Int?,
    val commentaire_Etape_1: String?,
    val predecesseur_etape: String?,   // reste String?
    val successeur_etape: String?      // reste String?
)
