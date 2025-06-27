package com.riva.atsmobile.model

import com.google.gson.annotations.SerializedName

data class Gamme(
    @SerializedName("Designation") val designation: String,
    @SerializedName("Code_Treillis") val codeTreillis: String,
    @SerializedName("Code_Produit") val codeProduit: String,
    @SerializedName("CP_Elingues") val cpElingues: String,
    @SerializedName("Nuance") val nuance: String,
    @SerializedName("DIAM_Chaine") val diamChaine: Double,
    @SerializedName("diam_Trame") val diamTrame: Double,
    @SerializedName("Diam_Chaine_Trame") val diamChaineTrame: String,
    @SerializedName("Dimension") val dimension: String,
    @SerializedName("EspFil_Chaine_Trame") val espFilChaineTrame: String,
    @SerializedName("Norme") val norme: String,
    @SerializedName("Colissage") val colissage: String,
    @SerializedName("Masse_panneau") val massePanneau: Double,
    @SerializedName("Masse_paquet") val massePaquet: Double,
    @SerializedName("Horo_maj") val horoMaj: String,
    @SerializedName("Valid") val valid: Int
) {
    val nbFilChaine: Int?
        get() {
            val normalized = designation.trim().uppercase()

            return when {
                normalized in setOf("PAF C", "PAF R", "PAF V", "PAF 10", "ST 15 C") -> 12
                normalized in setOf("ST 20", "ST 25", "ST 25 C") -> 16
                else -> null
            }
        }
}
