package com.riva.atsmobile.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.model.LoginResponse
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.utils.LocalAuthManager
import com.riva.atsmobile.utils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SelectionViewModel : ViewModel() {

    // --- Données utilisateur / session ---
    private val _matricule = MutableStateFlow("")
    val matricule: StateFlow<String> = _matricule.asStateFlow()

    private val _nom = MutableStateFlow("")
    val nom: StateFlow<String> = _nom.asStateFlow()

    private val _role = MutableStateFlow("")
    val role: StateFlow<String> = _role.asStateFlow()

    private val _annee = MutableStateFlow(2025)
    val annee: StateFlow<Int> = _annee.asStateFlow()

    private val _mois = MutableStateFlow(5)
    val mois: StateFlow<Int> = _mois.asStateFlow()

    private val _devModeEnabled = MutableStateFlow(false)
    val devModeEnabled: StateFlow<Boolean> = _devModeEnabled.asStateFlow()

    // --- Statut réseau ---
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private var isNetworkLoopStarted = false

    // === Données de l’écran “Type d’opération” ===
    private val _gammes = MutableStateFlow<List<Gamme>>(emptyList())
    val gammes: StateFlow<List<Gamme>> = _gammes.asStateFlow()

    private val _currentGamme = MutableStateFlow<Gamme?>(null)
    val currentGamme: StateFlow<Gamme?> = _currentGamme.asStateFlow()

    private val _desiredGamme = MutableStateFlow<Gamme?>(null)
    val desiredGamme: StateFlow<Gamme?> = _desiredGamme.asStateFlow()

    private val _zoneDeTravail = MutableStateFlow("")
    val zoneDeTravail: StateFlow<String> = _zoneDeTravail.asStateFlow()

    private val _intervention = MutableStateFlow("")
    val intervention: StateFlow<String> = _intervention.asStateFlow()

    private val _gammesSelectionnees = MutableStateFlow<Set<String>>(emptySet())
    val gammesSelectionnees: StateFlow<Set<String>> = _gammesSelectionnees.asStateFlow()

    fun setGammesSelectionnees(codes: Set<String>) {
        _gammesSelectionnees.value = codes
    }

    /** Charge les gammes depuis l’API */
    fun chargerGammesDepuisApi(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = ApiConfig.getBaseUrl(context)
                val url = "$baseUrl/api/gammes"

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val listType = object : TypeToken<List<Gamme>>() {}.type
                        val gammes = Gson().fromJson<List<Gamme>>(body, listType)
                        _gammes.value = gammes
                        gammes.apply {
                            _gammes.value = this
                            if (_gammesSelectionnees.value.isEmpty()) {
                                _gammesSelectionnees.value = this.map { it.codeTreillis }.toSet()
                            }
                        }

                    } else {
                        Log.e("GAMMES", "Réponse non valide : $body")
                    }
                }
            } catch (e: Exception) {
                Log.e("GAMMES", "Erreur lors du chargement des gammes", e)
            }
        }
    }

    fun selectCurrentGamme(g: Gamme) {
        _currentGamme.value = g
        if (_desiredGamme.value == g) {
            _desiredGamme.value = null
        }
    }

    fun selectDesiredGamme(g: Gamme) {
        if (_currentGamme.value != g) {
            _desiredGamme.value = g
        }
    }

    fun setZoneDeTravail(zone: String) {
        _zoneDeTravail.value = zone
    }

    fun setIntervention(inter: String) {
        _intervention.value = inter
    }

    fun validateGammeChange(onResult: (success: Boolean, msg: String) -> Unit) {
        viewModelScope.launch {
            try {
                // TODO : Remplacer cette partie par un POST réel si besoin
                onResult(true, "Validation OK")
            } catch (e: Exception) {
                Log.e("SelectionViewModel", "Erreur validation gamme", e)
                onResult(false, e.message ?: "Erreur inconnue")
            }
        }
    }

    // --- Fonctions générales ---
    fun setMatricule(value: String) { _matricule.value = value }
    fun setNom(value: String) { _nom.value = value }
    fun setRole(value: String) { _role.value = value }
    fun setAnnee(value: Int) { _annee.value = value }
    fun setMois(value: Int) { _mois.value = value }
    fun activateDevMode() { _devModeEnabled.value = true }
    fun setDevMode(enabled: Boolean) { _devModeEnabled.value = enabled }

    fun updateOnlineStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                isNetworkAvailable(context)
            } catch (_: Exception) {
                false
            }
            _isOnline.value = result
        }
    }

    fun InitNetworkObserverIfNeeded(context: Context, intervalMillis: Long = 5000L) {
        if (isNetworkLoopStarted) return
        isNetworkLoopStarted = true
        viewModelScope.launch {
            while (true) {
                updateOnlineStatus(context)
                kotlinx.coroutines.delay(intervalMillis)
            }
        }
    }

    fun reset() {
        setMatricule("")
        setNom("")
        setRole("")
        setAnnee(2025)
        setMois(1)
        _devModeEnabled.value = false
    }

    fun chargerSessionLocale(context: Context) {
        val userInfo = LocalAuthManager.loadUserInfo(context)
        if (userInfo != null) {
            setMatricule(userInfo.matricule)
            setNom(userInfo.nom)
            setRole(userInfo.role)
        }
    }

    suspend fun verifierConnexion(
        context: Context,
        matricule: String,
        motDePasse: String
    ): Result<LoginResponse> {
        val baseUrl = ApiConfig.getBaseUrl(context)
        Log.d("API_URL", "Base URL utilisée pour la connexion : $baseUrl")
        val url = "$baseUrl/api/auth/login"

        val json = """
            {
                "matricule": "$matricule",
                "motDePasse": "$motDePasse"
            }
        """.trimIndent()

        val client = OkHttpClient()
        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val user = Gson().fromJson(body, LoginResponse::class.java)
                        LocalAuthManager.saveUserInfo(
                            context,
                            user.matricule,
                            user.nom,
                            user.role,
                            motDePasse
                        )
                        Result.success(user)
                    } else {
                        Result.failure(Exception(body ?: "Erreur inconnue"))
                    }
                }
            } catch (e: Exception) {
                Log.e("SelectionViewModel", "Erreur lors de la requête réseau", e)
                Result.failure(e)
            }
        }
    }
}
