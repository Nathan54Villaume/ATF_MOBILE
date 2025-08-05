// file: app/src/main/java/com/riva/atsmobile/utils/SessionManager.kt
package com.riva.atsmobile.utils

import android.content.Context
import com.google.gson.Gson
import com.riva.atsmobile.model.Gamme

/**
 * Utility object to manage saving, loading and clearing user session data locally,
 * including the current step index for resuming progress.
 */
object SessionManager {
    private const val PREFS_NAME = "ats_session"
    private const val KEY_CURRENT = "current_gamme"
    private const val KEY_DESIRED = "desired_gamme"
    private const val KEY_ZONE = "zone"
    private const val KEY_INTERVENTION = "intervention"
    private const val KEY_STEP_INDEX = "step_index"

    private val gson = Gson()

    /**
     * Data class to represent a saved session, including the last step index.
     */
    data class SessionData(
        val current: Gamme,
        val desired: Gamme,
        val zone: String,
        val intervention: String,
        val stepIndex: Int
    )

    /**
     * Save the full session, serializing gammes as JSON and storing the step index.
     * Now accepts nullable Gamme? for compatibility with view-model calls.
     */
    fun saveSession(
        context: Context,
        current: Gamme?,
        desired: Gamme?,
        zone: String,
        intervention: String,
        stepIndex: Int
    ) {
        // Si l’une des gammes est nulle, on ne sauvegarde rien
        if (current == null || desired == null) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
              // if current or desired is null, clear the entry (or skip, as you prefer)
             .putString(KEY_CURRENT, current?.let { gson.toJson(it) })
            .putString(KEY_DESIRED, desired?.let { gson.toJson(it) })
            .putString(KEY_ZONE, zone)
            .putString(KEY_INTERVENTION, intervention)
            .putInt(KEY_STEP_INDEX, stepIndex)
            .apply()
    }

    /**
     * Overload pour sauvegarder d’un coup un SessionData.
     */
    fun saveSession(
        context: Context,
        session: SessionData
    ) {
        saveSession(
            context,
            session.current,
            session.desired,
            session.zone,
            session.intervention,
            session.stepIndex
        )
    }

    /**
     * Load the saved session; returns null if any required field is missing.
     */
    fun loadSession(context: Context): SessionData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentJson = prefs.getString(KEY_CURRENT, null) ?: return null
        val desiredJson = prefs.getString(KEY_DESIRED, null) ?: return null
        val zone = prefs.getString(KEY_ZONE, null) ?: return null
        val intervention = prefs.getString(KEY_INTERVENTION, null) ?: return null
        val stepIndex = prefs.getInt(KEY_STEP_INDEX, 0)
        return try {
            val current = gson.fromJson(currentJson, Gamme::class.java)
            val desired = gson.fromJson(desiredJson, Gamme::class.java)
            SessionData(current, desired, zone, intervention, stepIndex)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear all saved session data.
     */
    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
