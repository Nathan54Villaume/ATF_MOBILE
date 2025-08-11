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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EtapeViewModel : ViewModel() {

    // --- Toutes les étapes brutes ---
    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes.asStateFlow()

    // --- Flux dédiés par opérateur ---
    private val _etapesSoudeuse = MutableStateFlow<List<Etape>>(emptyList())
    val etapesSoudeuse: StateFlow<List<Etape>> = _etapesSoudeuse.asStateFlow()

    private val _etapesTref1 = MutableStateFlow<List<Etape>>(emptyList())
    val etapesTref1: StateFlow<List<Etape>> = _etapesTref1.asStateFlow()

    private val _etapesTref2 = MutableStateFlow<List<Etape>>(emptyList())
    val etapesTref2: StateFlow<List<Etape>> = _etapesTref2.asStateFlow()

    /**
     * Charge toutes les étapes depuis le back, et répartit / trie
     * pour chaque opérateur.
     */
    fun loadEtapes(context: Context) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val response = repository.fetchAll()
            if (response.isSuccessful) {
                val all = response.body() ?: emptyList()
                _etapes.value = all

                // Filtrer et trier topologiquement pour chaque opérateur
                _etapesSoudeuse.value = all
                    .filter { it.affectation_Etape.contains("operateur_soudeuse") }
                    .topoSortForOperator("operateur_soudeuse")
                _etapesTref1.value    = all
                    .filter { it.affectation_Etape.contains("operateur_t1") }
                    .topoSortForOperator("operateur_t1")
                _etapesTref2.value    = all
                    .filter { it.affectation_Etape.contains("operateur_t2") }
                    .topoSortForOperator("operateur_t2")
            } else {
                _etapes.value = emptyList()
                _etapesSoudeuse.value = emptyList()
                _etapesTref1.value    = emptyList()
                _etapesTref2.value    = emptyList()
            }
        }
    }

    /**
     * Valide une étape pour un opérateur donné.
     */
    fun validerEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        operateur: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val baseUrl    = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val dto = EtapeValidationDto(
                id_Etape      = id_Etape,
                commentaire   = commentaire,
                description   = description,
                tempsReel     = tempsReel,
                operateur     = operateur,
                etatParRole   = mapOf(operateur to "VALIDE")
            )
            val response = repository.validate(dto)
            val success  = response.isSuccessful
            val errorMsg = if (!success) response.errorBody()?.string() else null
            if (success) loadEtapes(context)
            onComplete(success, errorMsg)
        }
    }

    /**
     * Annule la validation d’une étape pour un opérateur donné.
     */
    fun devaliderEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        operateur: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val baseUrl    = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            val dto = EtapeValidationDto(
                id_Etape      = id_Etape,
                commentaire   = commentaire,
                description   = description,
                tempsReel     = tempsReel,
                operateur     = operateur,
                etatParRole   = mapOf(operateur to "EN ATTENTE")
            )
            val response = repository.unvalidate(dto)
            val success  = response.isSuccessful
            val errorMsg = if (!success) response.errorBody()?.string() else null
            if (success) loadEtapes(context)
            onComplete(success, errorMsg)
        }
    }

    /**
     * Réinitialise toutes les validations côté serveur.
     */
    fun resetSession(context: Context) {
        viewModelScope.launch {
            try {
                val api      = ApiServerClient.create(ApiConfig.getBaseUrl(context))
                val response = api.resetSession()
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Erreur API : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur réseau : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Création / mise à jour d'étapes (inchangées).
     */
    fun createEtape(context: Context, dto: EtapeCreateDto, onComplete: (Boolean) -> Unit) { /* ... */ }
    fun updateEtape(context: Context, id: Int, dto: EtapeUpdateDto, onComplete: (Boolean) -> Unit) { /* ... */ }
}

/**
 * Extension privée pour trier topologiquement les étapes
 * d’après leurs prédécesseurs *spécifiques* à un opérateur.
 */
private fun List<Etape>.topoSortForOperator(operateur: String): List<Etape> {
    val byId     = associateBy { it.id_Etape }
    val predMap  = mutableMapOf<Int, MutableSet<Int>>()

    // Construire la map id→ensemble de prédecesseurs pour cet opérateur
    for (e in this) {
        val preds = e.predecesseurs
            .filter { it.operateur == operateur }
            .flatMap { it.ids }
            .filter { it != 0 }
        predMap[e.id_Etape] = preds.toMutableSet()
    }

    // Kahn's algorithm
    val queue  = ArrayDeque(predMap.filterValues { it.isEmpty() }.keys)
    val result = mutableListOf<Etape>()
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        byId[id]?.let { result += it }
        for ((otherId, preds) in predMap) {
            if (preds.remove(id) && preds.isEmpty()) {
                queue += otherId
            }
        }
    }

    // Ajouter en fin les éventuels restants (pour casser les boucles)
    val doneIds = result.map { it.id_Etape }.toSet()
    val remaining = this.map { it.id_Etape }.toSet() - doneIds
    result += remaining.mapNotNull { byId[it] }

    return result
}
