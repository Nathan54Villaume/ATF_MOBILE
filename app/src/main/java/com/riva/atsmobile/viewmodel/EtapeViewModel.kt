// file: app/src/main/java/com/riva/atsmobile/viewmodel/EtapeViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.model.*
import com.riva.atsmobile.network.ATSApiService
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EtapeViewModel : ViewModel() {

    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes.asStateFlow()

    private val _selectedEtape = MutableStateFlow<Etape?>(null)
    val selectedEtape: StateFlow<Etape?> = _selectedEtape.asStateFlow()

    /** Charge toutes les étapes */
    fun loadEtapes(context: Context) {
        viewModelScope.launch {
            runCatching {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.getEtapes()
            }.onSuccess { _etapes.value = it }
                .onFailure { it.printStackTrace() }
        }
    }

    /** Charge une étape par son ID */
    fun loadEtapeById(context: Context, id: Int) {
        viewModelScope.launch {
            runCatching {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.getEtapeById(id)
            }.onSuccess { _selectedEtape.value = it }
                .onFailure { it.printStackTrace() }
        }
    }

    /** Crée une étape */
    fun createEtape(context: Context, dto: EtapeCreateDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.createEtape(dto).isSuccessful
            }.getOrDefault(false)
            onResult(success)
        }
    }

    /** Met à jour une étape */
    fun updateEtape(context: Context, id: Int, dto: EtapeUpdateDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.updateEtape(id, dto).isSuccessful
            }.getOrDefault(false)
            onResult(success)
        }
    }

    /** Valide une étape : retourne TRUE si l'appel a réussi */
    suspend fun validerEtape(context: Context, dto: EtapeValidationDto): Boolean {
        val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
        return api.validerEtape(dto).isSuccessful
    }

    /** Dévalide une étape et retourne true si succès */
    suspend fun devaliderEtape(context: Context, dto: EtapeValidationDto): Boolean {
        val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
        return api.devaliderEtape(dto).isSuccessful
    }
}
