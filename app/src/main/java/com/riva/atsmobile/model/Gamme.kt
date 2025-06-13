package com.riva.atsmobile.model

import java.time.LocalDateTime

/**
 * Modèle Kotlin correspondant au DTO TreillisDto renvoyé par l'API.
 */
data class Gamme(
    val designation: String,
    val codeTreillis: String,
    val codeProduit: String,
    val cpElingues: String,
    val nuance: String,
    val diamChaine: Double,
    val diamTrame: Double,
    val aboutAvAr: String,
    val aboutAdAg: String,
    val diamChaineTrame: String,
    val dimension: String,
    val espFilChaineTrame: Double,
    val norme: String,
    val colissage: String,
    val massePanneau: Double,
    val massePaquet: Double,
    val horoMaj: LocalDateTime,
    val valid: Boolean
)
