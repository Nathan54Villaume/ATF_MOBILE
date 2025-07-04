// file: model/Etape.kt
package com.riva.atsmobile.model

data class Etape(
    val id_Etape: Int,
    val libelle_Etape: String,
    val affectation_Etape: String,
    val role_Log: String,
    val phase_Etape: String,
    val duree_Etape: Int?,
    val description_Etape: String?,
    val etat_Etape: String?,
    val temps_Reel_Etape: Int?,
    val commentaire_Etape_1: String?,

    // On reprend simplement les deux chaînes brutes
    @SerializedName("predecesseur_etape")
    val predecesseur_Etape: String?,

    @SerializedName("successeur_etape")
    val successeur_Etape: String?
)

