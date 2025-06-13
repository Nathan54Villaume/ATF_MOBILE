// /model/Gamme.kt
package com.riva.atsmobile.model

data class Gamme(
    val id: String,           // ou Int selon ton API
    val name: String,
    val meshSize: String,
    val wireDiameter: String,
    val chainCount: String
)
