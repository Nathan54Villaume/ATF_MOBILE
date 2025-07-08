package com.riva.atsmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.data.EtapesRepository
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.EtapeCreateDto
import com.riva.atsmobile.model.EtapeUpdateDto
import com.riva.atsmobile.model.EtapeValidationDto
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel gérant les étapes et leurs validations
.
 */
class EtapeViewModel : ViewModel() {

    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes.asStateFlow()

    /** Charge toutes les étapes depuis le backend */
    fun loadEtapes(context: Context) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val response = repository.fetchAll()
            _etapes.value = if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    /**
     * Vérifie si toutes les étapes pré-requises pour la validation d'une étape donnée sont validées.
     * Les IDs des étapes pré-requises sont contenus dans la chaîne conditions_A_Valider,
     * séparés par le délimiteur "!".
     */
    private fun checkValidationPreconditions(etape: Etape): String? {
        val conditions = etape.conditions_A_Valider
        if (conditions.isNullOrBlank()) {
            return null // Pas de conditions, donc c'est OK
        }

        val requiredEtapeIds = conditions.split("!").mapNotNull { it.trim().toIntOrNull() }

        for (requiredId in requiredEtapeIds) {
            // Ne pas vérifier l'étape elle-même si elle est listée par erreur dans ses propres conditions
            if (requiredId == etape.id_Etape) continue

            val requiredEtape = _etapes.value.find { it.id_Etape == requiredId }
            if (requiredEtape == null) {
                return "L'étape de condition préalable (ID: $requiredId) n'existe pas."
            }
            if (requiredEtape.etat_Etape != "VALIDE") {
                return "L'étape préalable '${requiredEtape.libelle_Etape}' (ID: $requiredId) n'est pas validée."
            }
        }
        return null // Toutes les conditions sont remplies
    }

    /** Valide une étape et recharge la liste */
    fun validerEtape(
        context: Context,
        etapeToValidate: Etape, // Ajout du paramètre pour l'étape à valider
        dto: EtapeValidationDto,
        onComplete: (Boolean, String?) -> Unit // Modification du callback
    ) {
        // Vérification des pré-conditions
        val preconditionError = checkValidationPreconditions(etapeToValidate)
        if (preconditionError != null) {
            onComplete(false, preconditionError)
            return
        }

        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val response = repository.validate(dto)
            val success = response.isSuccessful
            if (success) {
                loadEtapes(context) // Recharger toutes les étapes si succès
                onComplete(true, null)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur de validation de l'étape."
                onComplete(false, errorMessage)
            }
        }
    }

    /** Dévalide une étape et recharge la liste */
    fun devaliderEtape(
        context: Context,
        dto: EtapeValidationDto,
        onComplete: (Boolean, String?) -> Unit // Modification du callback
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val response = repository.unvalidate(dto)
            val success = response.isSuccessful
            if (success) {
                loadEtapes(context) // Recharger toutes les étapes si succès
                onComplete(true, null)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur de dévalidation de l'étape."
                onComplete(false, errorMessage)
            }
        }
    }

    /** Création d'une nouvelle étape */
    fun createEtape(
        context: Context,
        dto: EtapeCreateDto,
        onComplete: (Boolean) -> Unit
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val success = repository.create(dto).isSuccessful
            if (success) loadEtapes(context)
            onComplete(success)
        }
    }

    /** Mise à jour d'une étape existante */
    fun updateEtape(
        context: Context,
        id: Int,
        dto: EtapeUpdateDto,
        onComplete: (Boolean) -> Unit
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val success = repository.update(id, dto).isSuccessful
            if (success) loadEtapes(context)
            onComplete(success)
        }
    }
}