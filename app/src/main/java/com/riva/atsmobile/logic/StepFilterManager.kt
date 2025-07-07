package com.riva.atsmobile.logic // Déclare le package de ce fichier. Il est dans 'logic', ce qui est approprié pour la gestion des règles métier.

import android.content.Context // Importe la classe Context, nécessaire pour accéder aux SharedPreferences.
import android.content.SharedPreferences // Importe SharedPreferences pour stocker et récupérer les données de manière persistante.
import com.google.gson.Gson // Importe Gson pour la sérialisation et désérialisation d'objets Java/Kotlin en JSON et vice-versa.
import com.google.gson.reflect.TypeToken // Utilisé par Gson pour gérer la désérialisation de types génériques complexes (comme Map<String, List<Int>>).

/**
 * Objet Singleton qui gère les règles d'exclusion d'étapes.
 *
 * Ce manager permet de définir et de récupérer une liste d'étapes qui doivent être exclues
 * (non affichées ou non exécutées) en fonction de la transition entre un nombre de fils actuel
 * et un nombre de fils visé sur une machine.
 * Les règles d'exclusion sont stockées de manière persistante via SharedPreferences.
 */
object StepFilterManager { // Déclare un objet singleton. 'object' signifie qu'il n'y aura qu'une seule instance de cette classe dans toute l'application.

    // Nom du fichier de préférences partagées où les données seront stockées.
    private const val PREF_NAME = "step_filter_prefs"
    // Clé sous laquelle la carte des exclusions sera stockée dans les SharedPreferences.
    private const val KEY_EXCLUSIONS = "exclusion_map"

    // Variable pour stocker l'instance de SharedPreferences. Elle doit être initialisée via la fonction 'init'.
    private lateinit var prefs: SharedPreferences

    // Instance de Gson utilisée pour convertir les Maps en chaînes JSON et vice-versa.
    private val gson = Gson()

    // Carte par défaut des exclusions.
    // Chaque clé représente une transition de nombre de fils ("nbFilsActuel-nbFilsVise").
    // La valeur associée est une liste d'IDs d'étapes à exclure pour cette transition.
    private val defaultMap = mapOf(
        "12-16" to listOf(30, 31, 72), // Exclut les étapes 30, 31, 72 lors du passage de 12 à 16 fils.
        "16-12" to listOf(30, 31, 72), // Exclut les mêmes étapes lors du passage de 16 à 12 fils.
        "12-12" to listOf(29, 68, 69, 70, 71, 75), // Exclut ces étapes si le nombre de fils reste à 12.
        "16-16" to listOf(29, 68, 69, 70, 71, 75)  // Exclut ces étapes si le nombre de fils reste à 16.
    )

    /**
     * Initialise le StepFilterManager.
     * Cette fonction doit être appelée une fois au démarrage de l'application (par exemple, dans l'Application class).
     * Elle configure les SharedPreferences et initialise les exclusions avec la carte par défaut si elles n'existent pas déjà.
     * @param context Le contexte de l'application, nécessaire pour obtenir SharedPreferences.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Vérifie si la clé des exclusions existe déjà dans les préférences.
        if (!prefs.contains(KEY_EXCLUSIONS)) {
            // Si non, sauvegarde la carte par défaut pour la première utilisation.
            saveExclusions(defaultMap)
        }
    }

    /**
     * Récupère la liste des étapes à exclure pour une transition donnée de nombre de fils.
     * @param nbFilsActuel Le nombre de fils actuel. Peut être null.
     * @param nbFilsVise Le nombre de fils visé. Peut être null.
     * @return Une liste d'entiers représentant les IDs des étapes exclues. Retourne une liste vide si les nombres de fils sont null ou si aucune exclusion n'est définie pour cette transition.
     */
    fun getExcludedSteps(nbFilsActuel: Int?, nbFilsVise: Int?): List<Int> {
        // Si un des nombres de fils est null, aucune exclusion n'est appliquée.
        if (nbFilsActuel == null || nbFilsVise == null) return emptyList()
        // Construit la clé de la transition au format "actuel-visé".
        val key = "$nbFilsActuel-$nbFilsVise"
        // Charge toutes les exclusions, puis tente de récupérer la liste pour la clé spécifique.
        // Si la clé n'existe pas, retourne une liste vide.
        return loadExclusions()[key] ?: emptyList()
    }

    /**
     * Charge la carte complète des règles d'exclusion depuis les SharedPreferences.
     * @return Une Map où les clés sont les transitions de fils (String) et les valeurs sont des listes d'IDs d'étapes exclues (List<Int>).
     * Retourne la 'defaultMap' si aucune donnée n'est trouvée dans les préférences.
     */
    fun loadExclusions(): Map<String, List<Int>> {
        // Tente de récupérer la chaîne JSON des exclusions. Si null, utilise la carte par défaut.
        val json = prefs.getString(KEY_EXCLUSIONS, null) ?: return defaultMap
        // Définit le type générique pour Gson pour qu'il puisse désérialiser la Map correctement.
        val type = object : TypeToken<Map<String, List<Int>>>() {}.type
        // Désérialise la chaîne JSON en Map et la retourne.
        return gson.fromJson(json, type)
    }

    /**
     * Sauvegarde une nouvelle carte de règles d'exclusion dans les SharedPreferences.
     * La Map est convertie en chaîne JSON avant d'être stockée.
     * @param map La Map de règles d'exclusion à sauvegarder.
     */
    fun saveExclusions(map: Map<String, List<Int>>) {
        // Convertit la Map en chaîne JSON.
        val json = gson.toJson(map)
        // Ouvre un éditeur pour SharedPreferences, met la chaîne JSON, et applique les changements.
        prefs.edit().putString(KEY_EXCLUSIONS, json).apply()
    }
}