// file: app/src/main/java/com/riva/atsmobile/data/EtapesRepository.kt
package com.riva.atsmobile.data // Déclare le package où se trouve ce fichier. C'est le package 'data', ce qui est logique pour un repository.

import com.riva.atsmobile.model.* // Importe toutes les classes de modèles (data classes) définies dans le package 'model'.
import com.riva.atsmobile.network.ApiServerClient // Importe le client API spécifique au serveur (celui qui utilise Retrofit pour les opérations REST).

/**
 * Repository pour la gestion des données des Étapes.
 *
 * Cette classe agit comme une couche d'abstraction pour l'accès aux données.
 * Son rôle principal est de centraliser la logique de récupération et de modification
 * des données liées aux "Étapes", en cachant les détails de la source de données
 * (ici, une API REST via ApiServerClient).
 *
 * Cela permet à d'autres parties de l'application (comme les ViewModels) de demander
 * des données sans se soucier de savoir si elles viennent du réseau, d'une base de données locale,
 * ou d'une autre source.
 */
class EtapesRepository(baseUrl: String) { // Déclare la classe EtapesRepository. Elle prend un 'baseUrl' en paramètre pour savoir à quelle API se connecter.

    // Initialise une instance du client API pour le serveur.
    // C'est via cet objet 'api' que toutes les requêtes réseau pour les étapes seront effectuées.
    private val api = ApiServerClient.create(baseUrl)

    /**
     * Récupère toutes les étapes disponibles depuis l'API.
     * C'est une fonction suspendue, ce qui signifie qu'elle est asynchrone et doit être appelée depuis une coroutine.
     * Elle délègue l'appel à la méthode 'getEtapes()' de l'interface 'ApiServerClient'.
     */
    suspend fun fetchAll() = api.getEtapes()

    /**
     * Récupère une étape spécifique par son identifiant unique (ID) depuis l'API.
     * Délègue l'appel à 'getEtapeById(id)' de 'ApiServerClient'.
     * @param id L'identifiant de l'étape à récupérer.
     */
    suspend fun fetchById(id: Int) = api.getEtapeById(id)

    /**
     * Crée une nouvelle étape sur le serveur en envoyant un objet 'EtapeCreateDto'.
     * Délègue l'appel à 'createEtape(dto)' de 'ApiServerClient'.
     * @param dto L'objet de transfert de données (Data Transfer Object) contenant les informations pour créer l'étape.
     */
    suspend fun create(dto: EtapeCreateDto) = api.createEtape(dto)

    /**
     * Met à jour une étape existante sur le serveur.
     * Délègue l'appel à 'updateEtape(id, dto)' de 'ApiServerClient'.
     * @param id L'identifiant de l'étape à mettre à jour.
     * @param dto L'objet de transfert de données contenant les informations mises à jour de l'étape.
     */
    suspend fun update(id: Int, dto: EtapeUpdateDto) = api.updateEtape(id, dto)

    /**
     * Valide une étape sur le serveur.
     * Délègue l'appel à 'validerEtape(dto)' de 'ApiServerClient'.
     * @param dto L'objet de transfert de données contenant les informations nécessaires à la validation.
     */
    suspend fun validate(dto: EtapeValidationDto) = api.validerEtape(dto)

    /**
     * Dévalide une étape sur le serveur.
     * Délègue l'appel à 'devaliderEtape(dto)' de 'ApiServerClient'.
     * @param dto L'objet de transfert de données contenant les informations nécessaires à la dévalidation.
     */
    suspend fun unvalidate(dto: EtapeValidationDto) = api.devaliderEtape(dto)
}