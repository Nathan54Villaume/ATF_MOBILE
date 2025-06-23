package com.riva.atsmobile.domain.model

data class OperatorStepState(
    val operatorName: String,      // ex. "Opérateur T1"
    val currentStep: Int,          // numéro de l’étape en cours
    val totalSteps: Int,           // nombre total d’étapes
    val stepTitle: String,         // libellé de l’étape
    val stepDescription: String,   // description détaillée
    val estimatedDuration: Int,    // durée estimée en minutes
    val elapsedMinutes: Int,       // temps réel écoulé (min)
    val elapsedSeconds: Int,       // temps réel écoulé (sec)
    val progressPercent: Int,      // avancement 0–100%
    val comment: String,           // commentaire saisi
    val zone: String,              // zone de travail sélectionnée
    val intervention: String,      // type d’intervention sélectionné
    val processType: ProcessType        // mineur, majeur ou complet
)
