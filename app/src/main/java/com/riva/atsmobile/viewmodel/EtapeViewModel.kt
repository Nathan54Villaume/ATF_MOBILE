// file: app/src/main/java/com/riva/atsmobile/viewmodel/EtapeViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.data.EtapesRepository
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.EtapeCreateDto
import com.riva.atsmobile.model.EtapeUpdateDto
import com.riva.atsmobile.model.EtapeValidationDto
import com.riva.atsmobile.network.ApiServerClient
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

    /** Valide une étape et recharge la liste */
    fun validerEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        onComplete: (Boolean, String?) -> Unit // Changed lambda signature for consistency with prior errors
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val dto = EtapeValidationDto(id_Etape, commentaire, description, tempsReel)
            val response = repository.validate(dto)
            val success = response.isSuccessful
            val message = if (!success) response.errorBody()?.string() else null
            if (success) loadEtapes(context)
            onComplete(success, message)
        }
    }

    /** Dévalide une étape et recharge la liste */
    fun devaliderEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        onComplete: (Boolean, String?) -> Unit // Changed lambda signature for consistency with prior errors
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val dto = EtapeValidationDto(id_Etape, commentaire, description, tempsReel)
            val response = repository.unvalidate(dto)
            val success = response.isSuccessful
            val message = if (!success) response.errorBody()?.string() else null
            if (success) loadEtapes(context)
            onComplete(success, message)
        }
    }
    fun resetSession(context: Context) {
        viewModelScope.launch {
            try {
                val api = ApiServerClient.create("http://10.250.13.4:8088/") // ou ton URL API
                val response = api.resetSession()
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Erreur API : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
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

/** Extension pour trier topologiquement une liste d'étapes. */
private fun List<Etape>.topoSortForOperator(operateur: String): List<Etape> {
    val byId = associateBy { it.id_Etape }
    val predMap = mutableMapOf<Int, MutableSet<Int>>()
    for (e in this) {
        val preds = e.predecesseurs
            .filter { it.operateur == operateur }
            .flatMap { it.ids }
        predMap[e.id_Etape] = preds.toMutableSet()
    }
    val ready = ArrayDeque(predMap.filterValues { it.isEmpty() }.keys)
    val result = mutableListOf<Etape>()
    while (ready.isNotEmpty()) {
        val id = ready.removeFirst()
        byId[id]?.let { result += it }
        for ((otherId, preds) in predMap) {
            if (preds.remove(id) && preds.isEmpty()) ready += otherId
        }
    }
    val remaining = this.map { it.id_Etape }.toSet() - result.map { it.id_Etape }.toSet()
    result += remaining.mapNotNull { byId[it] }
    return result
}