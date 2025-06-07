package com.riva.atsmobile.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object LocalAuthManager {

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "local_auth",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun saveUserInfo(
        context: Context,
        matricule: String,
        nom: String,
        role: String,
        password: String
    ) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putString("matricule", matricule)
            putString("nom", nom)
            putString("role", role)
            putString("passwordHash", hashPassword(password))
            apply()
        }
    }

    fun loadUserInfo(context: Context): UserInfo? {
        val prefs = getPrefs(context)
        val matricule = prefs.getString("matricule", null)
        val nom = prefs.getString("nom", null)
        val role = prefs.getString("role", null)
        val passwordHash = prefs.getString("passwordHash", null)

        return if (matricule != null && nom != null && role != null && passwordHash != null) {
            UserInfo(matricule, nom, role, passwordHash)
        } else null
    }

    fun isValidOffline(context: Context, matricule: String, password: String): Boolean {
        val prefs = getPrefs(context)
        val savedMatricule = prefs.getString("matricule", null)
        val savedPasswordHash = prefs.getString("passwordHash", null)
        return (matricule == savedMatricule && savedPasswordHash == hashPassword(password))
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    data class UserInfo(
        val matricule: String,
        val nom: String,
        val role: String,
        val passwordHash: String
    )
}
