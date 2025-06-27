// file: model/Etape.kt
package com.riva.atsmobile.model

data class Etape(
    val id_etape: Int,
    val libelle_etape: String,
    val affectation_etape: String,
    val role_log: String,
    val phase_etape: String,
    val duree_etape: Int?,
    val description_etape: String?,
    val etat_etape: String?,
    val temps_reel_etape: Int?,
    val commentaire_etape_1: String?
)
