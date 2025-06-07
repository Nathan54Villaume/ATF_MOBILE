package com.riva.atsmobile.model

data class LoginRequest(
    val matricule: String,
    val motDePasse: String
)

data class LoginResponse(
    val matricule: String,
    val nom: String,
    val role: String
)
