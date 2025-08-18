// file: app/src/main/java/com/riva/atsmobile/data/mappers/EtapeDtoMappers.kt
package com.riva.atsmobile.data.mappers
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.riva.atsmobile.model.Etape
import com.riva.atsmobile.model.EtapeCreateDto
import com.riva.atsmobile.model.EtapeRelation
import com.riva.atsmobile.model.EtapeUpdateDto

private val listTypeEtapeRelation = object : TypeToken<List<EtapeRelation>>() {}.type

private fun List<EtapeRelation>?.toJsonOrNull(gson: Gson): String? =
    this?.takeIf { it.isNotEmpty() }?.let { gson.toJson(it, listTypeEtapeRelation) }

fun buildCreateDto(model: Etape, gson: Gson = Gson()): EtapeCreateDto =
    EtapeCreateDto(
        libelle_Etape        = model.libelle_Etape,
        affectation_Etape    = model.affectation_Etape,
        role_Log             = model.role_Log,
        phase_Etape          = model.phase_Etape,
        duree_Etape          = model.duree_Etape,
        description_Etape    = model.description_Etape,
        etatParRole          = model.etatParRole,              // Map<String,String>
        temps_Reel_Etape     = model.temps_Reel_Etape,
        commentaire_Etape_1  = model.commentaire_Etape_1,
        predecesseur_etape   = model.predecesseurs.toJsonOrNull(gson),
        successeur_etape     = model.successeurs.toJsonOrNull(gson),
        conditions_A_Valider = model.conditions_A_Valider
    )

fun buildUpdateDto(model: Etape, gson: Gson = Gson()): EtapeUpdateDto =
    EtapeUpdateDto(
        libelle_Etape        = model.libelle_Etape,
        affectation_Etape    = model.affectation_Etape,
        role_Log             = model.role_Log,
        phase_Etape          = model.phase_Etape,
        duree_Etape          = model.duree_Etape,
        description_Etape    = model.description_Etape,
        etatParRole          = model.etatParRole,
        temps_Reel_Etape     = model.temps_Reel_Etape,
        commentaire_Etape_1  = model.commentaire_Etape_1,
        predecesseur_etape   = model.predecesseurs.toJsonOrNull(gson),
        successeur_etape     = model.successeurs.toJsonOrNull(gson),
        conditions_A_Valider = model.conditions_A_Valider
    )
