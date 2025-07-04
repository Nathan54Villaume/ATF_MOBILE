// file: app/src/main/java/com/riva/atsmobile/model/EtapeCreateDto.kt
package com.riva.atsmobile.model

import com.google.gson.annotations.SerializedName

data class EtapeCreateDto(
    @SerializedName("libelle_etape")
    val libelle_Etape: String?,

    @SerializedName("affectation_etape")
    val affectation_Etape: String?,

    @SerializedName("role_log")
    val role_Log: String?,

    @SerializedName("phase_etape")
    val phase_Etape: String?,

    @SerializedName("duree_etape")
    val duree_Etape: Int?,

    @SerializedName("description_etape")
    val description_Etape: String?,

    @SerializedName("etat_etape")
    val etat_Etape: String?,

    @SerializedName("temps_reel_etape")
    val temps_Reel_Etape: Int?,

    @SerializedName("commentaire_etape_1")
    val commentaire_Etape_1: String?,

    @SerializedName("predecesseur_etape")
    val predecesseur_etape: String?,

    @SerializedName("successeur_etape")
    val successeur_etape: String?
)
