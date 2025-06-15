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
    @SerializedName("Valid") val valid: Boolean
)
