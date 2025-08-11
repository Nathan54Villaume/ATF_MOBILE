// file: app/src/main/java/com/riva/atsmobile/viewmodel/EtapeViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
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

class EtapeViewModel : ViewModel() {

    private val _etapes = MutableStateFlow<List<Etape>>(emptyList())
    val etapes: StateFlow<List<Etape>> = _etapes.asStateFlow()

    /* ----------------- Normalisation des rôles ----------------- */

    private fun normalizeRoleKey(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.replace(" ", "_")
            .replace("-", "_")
        s = s.replace(Regex("^operateur_t[_\\- ]?([0-9])$"), "operateur_t$1")
        s = s.replace(Regex("^mecanicien[_\\- ]?([0-9])$"), "mecanicien_$1")
        return s
    }

    private fun normalizeEtape(e: Etape): Etape {
        val normAffect = e.affectation_Etape
            .split(';', ',')
            .map { normalizeRoleKey(it) }
            .filter { it.isNotEmpty() }
            .joinToString(";")

        val normMap: Map<String, String> =
            (e.etatParRole ?: emptyMap())
                .mapKeys { (k, _) -> normalizeRoleKey(k) }

        return e.copy(
            affectation_Etape = normAffect,
            etatParRole = normMap
        )
    }

    /* ----------------- Chargement ----------------- */

    fun loadEtapes(context: Context) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            try {
                val response = repository.fetchAll()
                if (response.isSuccessful) {
                    val allRaw = response.body() ?: emptyList()
                    val all = allRaw.map { normalizeEtape(it) }
                    _etapes.value = all

                    // DEBUG : vérifie les clés/résultats pour id 23
                    all.firstOrNull { it.id_Etape == 23 }?.let { e ->
                        Log.d("EtapesVM", "ID=23 affect=${e.affectation_Etape} etatParRole=${e.etatParRole}")
                    }
                } else {
                    _etapes.value = emptyList()
                    Log.w("EtapesVM", "fetchAll failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                _etapes.value = emptyList()
                Log.e("EtapesVM", "fetchAll exception", e)
                Toast.makeText(context, "Erreur réseau : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ----------------- Validation / Dévalidation ----------------- */

    fun validerEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        operateur: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        val op = normalizeRoleKey(operateur)

        viewModelScope.launch {
            try {
                val dto = EtapeValidationDto(
                    id_Etape = id_Etape,
                    commentaire = commentaire,
                    description = description,
                    tempsReel = tempsReel,
                    operateur = op,
                    etatParRole = mapOf(op to "VALIDE")
                )
                val response = repository.validate(dto)
                val success = response.isSuccessful
                if (success) loadEtapes(context)
                onComplete(success, if (!success) response.errorBody()?.string() else null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }
    }

    fun devaliderEtape(
        context: Context,
        id_Etape: Int,
        commentaire: String,
        description: String,
        tempsReel: Int,
        operateur: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        val op = normalizeRoleKey(operateur)

        viewModelScope.launch {
            try {
                val dto = EtapeValidationDto(
                    id_Etape = id_Etape,
                    commentaire = commentaire,
                    description = description,
                    tempsReel = tempsReel,
                    operateur = op,
                    etatParRole = mapOf(op to "EN_ATTENTE") // IMPORTANT: underscore
                )
                val response = repository.unvalidate(dto)
                val success = response.isSuccessful
                if (success) loadEtapes(context)
                onComplete(success, if (!success) response.errorBody()?.string() else null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }
    }

    /* ----------------- Reset session ----------------- */

    // EtapeViewModel.kt
    fun resetSession(context: Context) {
        viewModelScope.launch {
            try {
                // On remet l'état local à zéro puis on recharge depuis l'API
                _etapes.value = emptyList()
                loadEtapes(context)
            } catch (e: Exception) {
                Log.e("EtapesVM", "resetSession local error", e)
            }
        }
    }


    /* ----------------- CRUD (si utilisés ailleurs) ----------------- */

    fun createEtape(context: Context, dto: EtapeCreateDto, onComplete: (Boolean) -> Unit) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            try {
                val res = repository.create(dto)
                onComplete(res.isSuccessful)
                if (res.isSuccessful) loadEtapes(context)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    fun updateEtape(context: Context, id: Int, dto: EtapeUpdateDto, onComplete: (Boolean) -> Unit) {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val repository = EtapesRepository(baseUrl)
        viewModelScope.launch {
            try {
                val res = repository.update(id, dto)
                onComplete(res.isSuccessful)
                if (res.isSuccessful) loadEtapes(context)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }
}
