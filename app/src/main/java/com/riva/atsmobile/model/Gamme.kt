package com.riva.atsmobile.model

import java.time.LocalDateTime

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
    // Diamètre Chaîne/Trame combiné (ex. "4x5 mm"), traité comme String
    val diamChaineTrame: String,
    val dimension: String,
    val espFilChaineTrame: String,
    val norme: String,
    val colissage: String,
    val massePanneau: Double,
    val massePaquet: Double,
    val horoMaj: LocalDateTime,
    val valid: Boolean
)
