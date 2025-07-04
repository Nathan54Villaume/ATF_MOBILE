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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SelectionViewModel : ViewModel() {

    private val _matricule = MutableStateFlow("")
    val matricule = _matricule.asStateFlow()

    private val _nom = MutableStateFlow("")
    val nom = _nom.asStateFlow()

    private val _role = MutableStateFlow("")
    val role = _role.asStateFlow()

    private val _annee = MutableStateFlow(2025)
    val annee = _annee.asStateFlow()

    private val _mois = MutableStateFlow(5)
    val mois = _mois.asStateFlow()

    private val _devModeEnabled = MutableStateFlow(false)
    val devModeEnabled = _devModeEnabled.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    private var isNetworkLoopStarted = false

    private val _gammes = MutableStateFlow<List<Gamme>>(emptyList())
    val gammes = _gammes.asStateFlow()

    private val _currentGamme = MutableStateFlow<Gamme?>(null)
    val currentGamme = _currentGamme.asStateFlow()

    private val _desiredGamme = MutableStateFlow<Gamme?>(null)
    val desiredGamme = _desiredGamme.asStateFlow()

    private val _gammesSelectionnees = MutableStateFlow<Set<String>>(emptySet())
    val gammesSelectionnees = _gammesSelectionnees.asStateFlow()

    private val _zoneDeTravail = MutableStateFlow("")
    val zoneDeTravail = _zoneDeTravail.asStateFlow()

    private val _intervention = MutableStateFlow("")
    val intervention = _intervention.asStateFlow()

    private val _groupedValues = MutableStateFlow<Map<String, Map<String, Any>>>(emptyMap())
    val groupedValues = _groupedValues.asStateFlow()

    private val _nbFilsActuel = MutableStateFlow<Int?>(null)
    val nbFilsActuelFlow = _nbFilsActuel.asStateFlow()

    private val _nbFilsVise = MutableStateFlow<Int?>(null)
    val nbFilsViseFlow = _nbFilsVise.asStateFlow()

    var memoireGammeActuelle: Gamme? = null
    var memoireGammeVisee: Gamme? = null

    fun setGammesSelectionnees(codes: Set<String>) {
        _gammesSelectionnees.value = codes
    }

    fun testFetchGroupedValues(context: Context) {
        viewModelScope.launch {
            val demoMap = mapOf("g1" to listOf("addr1", "addr2"), "g2" to listOf("addr3"))
            val baseUrl = ApiConfig.getBaseUrl(context)
            val result = com.riva.atsmobile.network.ApiAutomateClient.fetchGroupedValues(demoMap, baseUrl)
            _groupedValues.value = result
        }
    }

    fun chargerGammesDepuisApi(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = ApiConfig.getBaseUrl(context)
                val request = Request.Builder()
                    .url("$baseUrl/api/gammes")
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()

                OkHttpClient().newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val listType = object : TypeToken<List<Gamme>>() {}.type
                        val gammesLoaded = Gson().fromJson<List<Gamme>>(body, listType)
                        _gammes.value = gammesLoaded

                        if (_gammesSelectionnees.value.isEmpty()) {
                            val defaultDesignations = setOf(
                                "PAF R", "PAF C", "PAF V", "PAF 10",
                                "ST 15 C", "ST 20", "ST 25", "ST 25 C"
                            ).map { it.trim().uppercase() }.toSet()

                            _gammesSelectionnees.value = gammesLoaded
                                .filter { it.designation.trim().uppercase() in defaultDesignations }
                                .map { it.codeTreillis }
                                .toSet()
                        } else {

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

    fun setZoneDeTravail(zone: String) {
        _zoneDeTravail.value = zone
    }

    fun setIntervention(inter: String) {
        _intervention.value = inter
    }

    fun validateGammeChange(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(true, "Validation OK")
            } catch (e: Exception) {
                Log.e("SelectionViewModel", "Erreur validation gamme", e)
                onResult(false, e.message ?: "Erreur inconnue")
            }
        }
    }

    fun setMatricule(value: String) = run { _matricule.value = value }
    fun setNom(value: String) = run { _nom.value = value }
    fun setRole(value: String) = run { _role.value = value }
    fun setAnnee(value: Int) = run { _annee.value = value }
    fun setMois(value: Int) = run { _mois.value = value }
    fun setDevMode(enabled: Boolean) = run { _devModeEnabled.value = enabled }
    fun activateDevMode() = setDevMode(true)

    fun updateOnlineStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isOnline.value = try {
                isNetworkAvailable(context)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun InitNetworkObserverIfNeeded(context: Context, intervalMillis: Long = 5000L) {
        if (isNetworkLoopStarted) return
        isNetworkLoopStarted = true
        viewModelScope.launch {
            while (true) {
                updateOnlineStatus(context)
                delay(intervalMillis)
            }
        }
    }

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
        memoireGammeVisee = null
        _nbFilsActuel.value = null
        _nbFilsVise.value = null
    }

    fun chargerSessionLocale(context: Context) {
        LocalAuthManager.loadUserInfo(context)?.let {
            setMatricule(it.matricule)
            setNom(it.nom)
            setRole(it.role)
        }
    }

    suspend fun verifierConnexion(
        context: Context,
        matricule: String,
        motDePasse: String
    ): Result<LoginResponse> {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val url = "$baseUrl/api/auth/login"
        val json = """{"matricule":"$matricule","motDePasse":"$motDePasse"}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())

        return withContext(Dispatchers.IO) {
            try {
                OkHttpClient().newCall(
                    Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json")
                        .build()
                ).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val user = Gson().fromJson(body, LoginResponse::class.java)
                        LocalAuthManager.saveUserInfo(context, user.matricule, user.nom, user.role, motDePasse)
                        Result.success(user)
                    } else {
                        Result.failure(Exception(body ?: "Erreur inconnue"))
                    }
                }
            } catch (e: Exception) {
                Log.e("SelectionViewModel", "Erreur connexion", e)
                Result.failure(e)
            }
        }
    }
}
