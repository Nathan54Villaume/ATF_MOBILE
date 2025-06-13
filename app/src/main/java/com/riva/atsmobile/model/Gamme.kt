package com.riva.atsmobile.model

/**
 * Data class representing the TreillisDto returned by the API.
 */
data class Gamme(
    val designation: String,
    val codeTreillis: String,
    val codeProduit: String,
    val cpElingues: String,
    val nuance: String,
    val diamChaine: Double,
    val diamTrame: Double,
    /** Diamètre Chaîne/Trame combiné (ex. "4x5 mm") */
    val diamChaineTrame: String,
    /** Maille (ex. "10x10 mm") */
    val dimension: String,
    /** Espacement fil-chaîne/trame en mm */
    val espFilChaineTrame: String,
    val norme: String,
    val colissage: String,
    val massePanneau: Double,
    val massePaquet: Double,
    /** Horodatage au format ISO8601, parsé côté UI si besoin */
    val horoMaj: String,
    val valid: Boolean
)
