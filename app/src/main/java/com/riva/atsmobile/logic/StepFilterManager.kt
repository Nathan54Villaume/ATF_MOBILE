package com.riva.atsmobile.logic

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StepFilterManager {
    private const val PREF_NAME = "step_filter_prefs"
    private const val KEY_EXCLUSIONS = "exclusion_map"

    private lateinit var prefs: SharedPreferences

    private val gson = Gson()

    // Par défaut : exclusions pour certaines transitions
    private val defaultMap = mapOf(
        "12-16" to listOf(30, 31, 72),
        "16-12" to listOf(30, 31, 72),
        "12-12" to listOf(29, 68, 69, 70, 71, 75),
        "16-16" to listOf(29, 68, 69, 70, 71, 75)
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_EXCLUSIONS)) {
            saveExclusions(defaultMap)
        }
    }

    fun getExcludedSteps(nbFilsActuel: Int?, nbFilsVise: Int?): List<Int> {
        if (nbFilsActuel == null || nbFilsVise == null) return emptyList()
        val key = "$nbFilsActuel-$nbFilsVise"
        return loadExclusions()[key] ?: emptyList()
    }

    fun loadExclusions(): Map<String, List<Int>> {
        val json = prefs.getString(KEY_EXCLUSIONS, null) ?: return defaultMap
        val type = object : TypeToken<Map<String, List<Int>>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveExclusions(map: Map<String, List<Int>>) {
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_EXCLUSIONS, json).apply()
    }
}
