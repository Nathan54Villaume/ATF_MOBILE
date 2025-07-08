package com.riva.atsmobile.model

import com.google.gson.annotations.SerializedName

/**
 * Représente un lien entre étapes pour un opérateur donné.
 */
data class EtapeRelation(
    @SerializedName("operateur")
    val operateur: String,

    @SerializedName("ids")
    val ids: List<Int>
)

data class Etape(
    @SerializedName("id_etape")
    val id_Etape: Int,

    @SerializedName("libelle_etape")
    val libelle_Etape: String,

    @SerializedName("affectation_etape")
    val affectation_Etape: String,

    @SerializedName("role_log")
    val role_Log: String,

    @SerializedName("phase_etape")
    val phase_Etape: String,

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

    @SerializedName("predecesseurs")
    val predecesseurs: List<EtapeRelation>,

    @SerializedName("successeurs")
    val successeurs: List<EtapeRelation>,

    @SerializedName("conditions_a_valider")
    val conditions_A_Valider: String? // Nouvelle colonne
)
