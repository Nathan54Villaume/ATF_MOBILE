// file: app/src/main/java/com/riva/atsmobile/viewmodel/EtapeViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.model.*
import com.riva.atsmobile.network.ATSApiService
import com.riva.atsmobile.utils.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EtapeViewModel : ViewModel() {

    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes.asStateFlow()

    private val _selectedEtape = MutableStateFlow<Etape?>(null)
    val selectedEtape: StateFlow<Etape?> = _selectedEtape.asStateFlow()

    /** Charge toutes les étapes */
    fun loadEtapes(context: Context) {
        viewModelScope.launch {
            try {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                val response = api.getEtapes()
                if (response.isSuccessful) {
                    _etapes.value = response.body() ?: emptyList()
                } else {
                    // gérer l'erreur si besoin, ex: log ou toast
                    _etapes.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _etapes.value = emptyList()
            }
        }
    }

    /** Charge une étape par son ID */
    fun loadEtapeById(context: Context, id: Int) {
        viewModelScope.launch {
            try {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                val response = api.getEtapeById(id)
                if (response.isSuccessful) {
                    _selectedEtape.value = response.body()
                } else {
                    _selectedEtape.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _selectedEtape.value = null
            }
        }
    }

    /** Crée une étape */
    fun createEtape(context: Context, dto: EtapeCreateDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.createEtape(dto).isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            onResult(success)
        }
    }

    /** Met à jour une étape */
    fun updateEtape(context: Context, id: Int, dto: EtapeUpdateDto, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
                api.updateEtape(id, dto).isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            onResult(success)
        }
    }

    /** Valide une étape : retourne TRUE si l'appel a réussi */
    suspend fun validerEtape(context: Context, dto: EtapeValidationDto): Boolean {
        return try {
            val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
            api.validerEtape(dto).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Dévalide une étape et retourne true si succès */
    suspend fun devaliderEtape(context: Context, dto: EtapeValidationDto): Boolean {
        return try {
            val api = ATSApiService.create(ApiConfig.getBaseUrl(context))
            api.devaliderEtape(dto).isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
