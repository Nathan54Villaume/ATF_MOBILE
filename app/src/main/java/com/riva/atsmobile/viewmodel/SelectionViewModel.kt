// file: app/src/main/java/com/riva/atsmobile/viewmodel/SelectionViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.model.LoginResponse
import com.riva.atsmobile.network.ApiServerClient
import com.riva.atsmobile.utils.ApiConfig
import com.riva.atsmobile.utils.LocalAuthManager
import com.riva.atsmobile.utils.SessionManager
import com.riva.atsmobile.utils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SelectionViewModel : ViewModel() {

    // --- User info ---
    private val _matricule = MutableStateFlow("")
    val matricule: StateFlow<String> = _matricule.asStateFlow()

    private val _nom = MutableStateFlow("")
    val nom: StateFlow<String> = _nom.asStateFlow()

    private val _role = MutableStateFlow("")
    val role: StateFlow<String> = _role.asStateFlow()

    // true si rôle == "ADMIN"
    val isAdmin: StateFlow<Boolean> = _role
        .map { it.equals("ADMIN", ignoreCase = true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- Date contextuelles ---
    private val _annee = MutableStateFlow(2025)
    val annee: StateFlow<Int> = _annee.asStateFlow()

    private val _mois = MutableStateFlow(1)
    val mois: StateFlow<Int> = _mois.asStateFlow()

    // --- Dev mode toggle ---
    private val _devModeEnabled = MutableStateFlow(false)
    val devModeEnabled: StateFlow<Boolean> = _devModeEnabled.asStateFlow()

    // --- Connectivity observer ---
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    private var networkObserverStarted = false

    // --- Gammes & sélection ---
    private val _gammes = MutableStateFlow<List<Gamme>>(emptyList())
    val gammes: StateFlow<List<Gamme>> = _gammes.asStateFlow()

    private val _currentGamme = MutableStateFlow<Gamme?>(null)
    val currentGamme: StateFlow<Gamme?> = _currentGamme.asStateFlow()

    private val _desiredGamme = MutableStateFlow<Gamme?>(null)
    val desiredGamme: StateFlow<Gamme?> = _desiredGamme.asStateFlow()

    private val _gammesSelectionnees = MutableStateFlow<Set<String>>(emptySet())
    val gammesSelectionnees: StateFlow<Set<String>> = _gammesSelectionnees.asStateFlow()

    // --- Zone & intervention ---
    private val _zoneDeTravail = MutableStateFlow("")
    val zoneDeTravail: StateFlow<String> = _zoneDeTravail.asStateFlow()

    private val _intervention = MutableStateFlow("")
    val intervention: StateFlow<String> = _intervention.asStateFlow()

    // --- Comptage fils ---
    private val _nbFilsActuel = MutableStateFlow<Int?>(null)
    val nbFilsActuelFlow: StateFlow<Int?> = _nbFilsActuel.asStateFlow()

    private val _nbFilsVise = MutableStateFlow<Int?>(null)
    val nbFilsViseFlow: StateFlow<Int?> = _nbFilsVise.asStateFlow()

    // --- Grouped values (exemple) ---
    private val _groupedValues = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val groupedValues: StateFlow<Map<String, Map<String, Any>>> = _groupedValues.asStateFlow()

    // Mémoire temporaire (pour affichage)
    var memoireGammeActuelle: Gamme? = null
    var memoireGammeVisee: Gamme? = null

    // ————————————————————————————————————————————
    // 1) Mutateurs simples
    // ————————————————————————————————————————————
    fun setMatricule(value: String)            { _matricule.value = value }
    fun setNom(value: String)                  { _nom.value = value }
    fun setRole(value: String)                 { _role.value = value }
    fun setAnnee(value: Int)                   { _annee.value = value }
    fun setMois(value: Int)                    { _mois.value = value }
    fun setDevMode(enabled: Boolean)           { _devModeEnabled.value = enabled }
    fun activateDevMode()                      { setDevMode(true) }

    fun setGammesSelectionnees(codes: Set<String>) { _gammesSelectionnees.value = codes }
    fun setZoneDeTravail(z: String)                { _zoneDeTravail.value = z }
    fun setIntervention(i: String)                 { _intervention.value = i }

    // ————————————————————————————————————————————
    // 2) Chargement / sélection de gammes
    // ————————————————————————————————————————————
    fun chargerGammesDepuisApi(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = ApiConfig.getBaseUrl(context)
                val resp = OkHttpClient().newCall(
                    Request.Builder()
                        .url("$baseUrl/api/gammes")
                        .get()
                        .addHeader("Accept", "application/json")
                        .build()
                ).execute()
                val body = resp.body?.string()
                if (resp.isSuccessful && body != null) {
                    val listType = object : TypeToken<List<Gamme>>() {}.type
                    val loaded = Gson().fromJson<List<Gamme>>(body, listType)
                    _gammes.value = loaded

                    if (_gammesSelectionnees.value.isEmpty()) {
                        val defaults = setOf(
                            "PAF R","PAF C","PAF V","PAF 10",
                            "ST 15 C","ST 20","ST 25","ST 25 C"
                        ).map(String::uppercase).toSet()
                        _gammesSelectionnees.value =
                            loaded.filter { it.designation.uppercase() in defaults }
                                .map { it.codeTreillis }
                                .toSet()
                    }
                } else {
                    Log.e("SelectionVM","Chargement gammes KO : $body")
                }
            } catch(e: Exception) {
                Log.e("SelectionVM","Erreur chargement gammes", e)
            }
        }
    }

    fun selectCurrentGamme(g: Gamme) {
        _currentGamme.value = g
        memoireGammeActuelle = g
        _nbFilsActuel.value = g.nbFilChaine
        if (_desiredGamme.value == g) _desiredGamme.value = null
    }

    fun selectDesiredGamme(g: Gamme) {
        if (_currentGamme.value != g) {
            _desiredGamme.value = g
            memoireGammeVisee = g
            _nbFilsVise.value = g.nbFilChaine
        }
    }

    fun testFetchGroupedValues(context: Context) {
        viewModelScope.launch {
            val demo = mapOf("g1" to listOf("a","b"), "g2" to listOf("c"))
            val baseUrl = ApiConfig.getBaseUrl(context)
            val result = com.riva.atsmobile.network.ApiAutomateClient.fetchGroupedValues(demo, baseUrl)
            _groupedValues.value = result
        }
    }

    // ————————————————————————————————————————————
    // 3) Persistance locale de la session
    // ————————————————————————————————————————————
    /** Charge uniquement les infos utilisateur stockées */
    fun chargerSessionLocale(context: Context) {
        LocalAuthManager.loadUserInfo(context)?.let {
            setMatricule(it.matricule)
            setNom(it.nom)
            setRole(it.role)
        }
    }

    /** Sauvegarde minimale (stub) */
    fun sauvegarderSessionLocalement(context: Context) {
        // SessionManager.saveSession(context, …)
    }

    /** Recharge l’utilisateur + gammes + zone + intervention */
    fun chargerSessionEnCours(context: Context) {
        // 1) utilisateur
        LocalAuthManager.loadUserInfo(context)?.let {
            setMatricule(it.matricule)
            setNom(it.nom)
            setRole(it.role)
        }
        // 2) métadonnées
        SessionManager.loadSession(context)?.let { s ->
            _currentGamme.value    = s.current
            _desiredGamme.value    = s.desired
            memoireGammeActuelle   = s.current
            memoireGammeVisee      = s.desired
            _zoneDeTravail.value   = s.zone
            _intervention.value    = s.intervention
            _nbFilsActuel.value    = s.current.nbFilChaine
            _nbFilsVise.value      = s.desired.nbFilChaine
        }
    }

    /** Vide tout l’état pour le logout */
    fun reset() {
        setMatricule("")
        setNom("")
        setRole("")
        setAnnee(2025)
        setMois(1)
        setDevMode(false)
        _currentGamme.value = null
        _desiredGamme.value = null
        memoireGammeActuelle = null
        memoireGammeVisee    = null
        _nbFilsActuel.value = null
        _nbFilsVise.value   = null
    }

    // ————————————————————————————————————————————
    // 4) Réseau / état online
    // ————————————————————————————————————————————
    fun updateOnlineStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOnline.value = try {
                isNetworkAvailable(context)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun initNetworkObserverIfNeeded(context: Context, intervalMillis: Long = 5_000L) {
        if (networkObserverStarted) return
        networkObserverStarted = true
        viewModelScope.launch {
            while (true) {
                updateOnlineStatus(context)
                delay(intervalMillis)
            }
        }
    }

    // ————————————————————————————————————————————
    // 5) API d’authentification & session serveur
    // ————————————————————————————————————————————
    suspend fun verifierConnexion(
        context: Context,
        matricule: String,
        motDePasse: String
    ): Result<LoginResponse> {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val url     = "$baseUrl/api/auth/login"
        val json    = """{"matricule":"$matricule","motDePasse":"$motDePasse"}"""
        val body    = json.toRequestBody("application/json".toMediaType())

        return withContext(Dispatchers.IO) {
            try {
                OkHttpClient().newCall(
                    Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Accept","application/json")
                        .addHeader("Content-Type","application/json")
                        .build()
                ).execute().use { resp ->
                    val respBody = resp.body?.string()
                    if (resp.isSuccessful && respBody != null) {
                        val user = Gson().fromJson(respBody, LoginResponse::class.java)
                        LocalAuthManager.saveUserInfo(
                            context,
                            user.matricule, user.nom, user.role, motDePasse
                        )
                        Result.success(user)
                    } else {
                        Result.failure(Exception(respBody ?: "Erreur inconnue"))
                    }
                }
            } catch (e: Exception) {
                Log.e("SelectionVM","Login error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun demarrerNouvelleSession(): Boolean {
        return try {
            ApiServerClient
                .create("http://10.250.13.4:8088/")
                .resetSession()
                .isSuccessful
        } catch (e: Exception) {
            Log.e("SelectionVM","Session reset error", e)
            false
        }
    }
}
