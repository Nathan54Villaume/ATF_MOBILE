// file: app/src/main/java/com/riva/atsmobile/viewmodel/SelectionViewModel.kt
package com.riva.atsmobile.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.riva.atsmobile.model.*
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
import retrofit2.Response

class SelectionViewModel : ViewModel() {

    private val TAG = "SelectionVM"

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

    // --- Dates contextuelles ---
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
            val baseUrl = ApiConfig.getBaseUrl(context)
            val url = "$baseUrl/api/gammes"
            try {
                Log.d(TAG, "GET $url")
                val resp = OkHttpClient().newCall(
                    Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Accept", "application/json")
                        .build()
                ).execute()
                val body = resp.body?.string()
                Log.d(TAG, "GET /gammes -> code=${resp.code} success=${resp.isSuccessful} len=${body?.length ?: 0}")
                if (resp.isSuccessful && body != null) {
                    val listType = object : TypeToken<List<Gamme>>() {}.type
                    val loaded = Gson().fromJson<List<Gamme>>(body, listType)
                    Log.d(TAG, "gammes chargées: ${loaded.size}")
                    _gammes.value = loaded

                    if (_gammesSelectionnees.value.isEmpty()) {
                        val defaults = setOf("PAF R","PAF C","PAF V","PAF 10","ST 15 C","ST 20","ST 25","ST 25 C")
                            .map(String::uppercase).toSet()
                        _gammesSelectionnees.value =
                            loaded.filter { it.designation.uppercase() in defaults }
                                .map { it.codeTreillis }
                                .toSet()
                        Log.d(TAG, "gammes sélection par défaut: ${_gammesSelectionnees.value.size}")
                    }
                } else {
                    Log.e(TAG,"/gammes KO: ${body?.take(500)}")
                }
            } catch(e: Exception) {
                Log.e(TAG,"Erreur chargement gammes ($url)", e)
            }
        }
    }

    fun selectCurrentGamme(g: Gamme) {
        Log.d(TAG, "selectCurrentGamme code=${g.codeTreillis} nbFils=${g.nbFilChaine}")
        _currentGamme.value = g
        memoireGammeActuelle = g
        _nbFilsActuel.value = g.nbFilChaine
        if (_desiredGamme.value == g) _desiredGamme.value = null
    }

    fun selectDesiredGamme(g: Gamme) {
        if (_currentGamme.value != g) {
            Log.d(TAG, "selectDesiredGamme code=${g.codeTreillis} nbFils=${g.nbFilChaine}")
            _desiredGamme.value = g
            memoireGammeVisee = g
            _nbFilsVise.value = g.nbFilChaine
        }
    }

    fun testFetchGroupedValues(context: Context) {
        viewModelScope.launch {
            val demo = mapOf("g1" to listOf("a","b"), "g2" to listOf("c"))
            val baseUrl = ApiConfig.getBaseUrl(context)
            Log.d(TAG,"fetchGroupedValues -> $baseUrl")
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
            Log.d(TAG,"session locale chargée: ${it.matricule}/${it.role}")
            setMatricule(it.matricule)
            setNom(it.nom)
            setRole(it.role)
        } ?: Log.d(TAG,"aucune session locale")
    }

    /** Sauvegarde minimale (stub) */
    fun sauvegarderSessionLocalement(context: Context) {
        // SessionManager.saveSession(context, …
    }

    /** Recharge l’utilisateur + gammes + zone + intervention */
    fun chargerSessionEnCours(context: Context) {
        SessionManager.loadSession(context)?.let { s ->
            Log.d(TAG,"chargerSessionEnCours OK: ${s.current.codeTreillis} -> ${s.desired.codeTreillis}")
            _currentGamme.value    = s.current
            _desiredGamme.value    = s.desired
            memoireGammeActuelle   = s.current
            memoireGammeVisee      = s.desired
            _zoneDeTravail.value   = s.zone
            _intervention.value    = s.intervention
            _nbFilsActuel.value    = s.current.nbFilChaine
            _nbFilsVise.value      = s.desired.nbFilChaine
        } ?: Log.d(TAG,"pas de session en cours à recharger")
    }

    /** Vide tout l’état pour le logout */
    fun reset() {
        Log.d(TAG,"reset all state")
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
            val online = try { isNetworkAvailable(context) } catch (_: Exception) { false }
            if (_isOnline.value != online) Log.d(TAG, "online=$online")
            _isOnline.value = online
        }
    }

    fun initNetworkObserverIfNeeded(context: Context, intervalMillis: Long = 5_000L) {
        if (networkObserverStarted) return
        networkObserverStarted = true
        Log.d(TAG,"network observer started (every ${intervalMillis}ms)")
        viewModelScope.launch {
            while (true) {
                updateOnlineStatus(context)
                delay(intervalMillis)
            }
        }
    }

    // ————————————————————————————————————————————
    // 5) API d’authentification & session serveur (LOGS AJOUTÉS)
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
                Log.d(TAG, "POST $url (matricule=$matricule)")
                OkHttpClient().newCall(
                    Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Accept","application/json")
                        .addHeader("Content-Type","application/json")
                        .build()
                ).execute().use { resp ->
                    val respBody = resp.body?.string()
                    Log.d(TAG, "login -> code=${resp.code} success=${resp.isSuccessful} len=${respBody?.length ?: 0}")
                    if (resp.isSuccessful && respBody != null) {
                        val user = Gson().fromJson(respBody, LoginResponse::class.java)
                        LocalAuthManager.saveUserInfo(
                            context,
                            user.matricule, user.nom, user.role, motDePasse
                        )
                        Result.success(user)
                    } else {
                        Log.e(TAG, "login erreur: ${respBody?.take(800)}")
                        Result.failure(Exception(respBody ?: "Erreur inconnue"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG,"login exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun demarrerNouvelleSession(context: Context): Boolean {
        val baseUrl = ApiConfig.getBaseUrl(context)
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "resetSession -> baseUrl=$baseUrl")
                val api = ApiServerClient.create(baseUrl)
                val r: Response<Void> = api.resetSession()
                val err = try { r.errorBody()?.string() } catch (_: Exception) { null }
                Log.d(
                    TAG,
                    "resetSession resp: code=${r.code()} success=${r.isSuccessful} " +
                            "headers=${r.headers().size} err=${err?.take(800)}"
                )
                r.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "resetSession exception", e)
                false
            }
        }
    }

    // ————————————————————————————————————————————
    // 6) Création / Mise à jour d'une Étape (corrigé + LOGS)
    // ————————————————————————————————————————————

    /** Extrait le **premier id > 1** ; sinon "" */
    private fun List<EtapeRelation>?.firstValidIdOrEmpty(): String {
        if (this == null) return ""
        val id = this.asSequence().flatMap { it.ids.asSequence() }.firstOrNull { it > 1 }
        return id?.toString() ?: ""
    }

    private fun buildCreateDtoFromModel(model: Etape): EtapeCreateDto {
        val pred = model.predecesseurs.firstValidIdOrEmpty()
        val succ = model.successeurs.firstValidIdOrEmpty()
        Log.d(TAG, "buildCreateDto pred='$pred' succ='$succ' id=${model.id_Etape}")
        return EtapeCreateDto(
            libelle_Etape        = model.libelle_Etape,
            affectation_Etape    = model.affectation_Etape,
            role_Log             = model.role_Log,
            phase_Etape          = model.phase_Etape,
            duree_Etape          = model.duree_Etape,
            description_Etape    = model.description_Etape,
            etatParRole          = model.etatParRole,
            temps_Reel_Etape     = model.temps_Reel_Etape,
            commentaire_Etape_1  = model.commentaire_Etape_1,
            predecesseur_etape   = pred,
            successeur_etape     = succ,
            conditions_A_Valider = model.conditions_A_Valider
        )
    }

    private fun buildUpdateDtoFromModel(model: Etape): EtapeUpdateDto {
        val pred = model.predecesseurs.firstValidIdOrEmpty()
        val succ = model.successeurs.firstValidIdOrEmpty()
        Log.d(TAG, "buildUpdateDto pred='$pred' succ='$succ' id=${model.id_Etape}")
        return EtapeUpdateDto(
            libelle_Etape        = model.libelle_Etape,
            affectation_Etape    = model.affectation_Etape,
            role_Log             = model.role_Log,
            phase_Etape          = model.phase_Etape,
            duree_Etape          = model.duree_Etape,
            description_Etape    = model.description_Etape,
            etatParRole          = model.etatParRole,
            temps_Reel_Etape     = model.temps_Reel_Etape,
            commentaire_Etape_1  = model.commentaire_Etape_1,
            predecesseur_etape   = pred,
            successeur_etape     = succ,
            conditions_A_Valider = model.conditions_A_Valider
        )
    }

    suspend fun createEtapeFromModel(context: Context, model: Etape): Response<Void> {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val api = ApiServerClient.create(baseUrl)
        val dto = buildCreateDtoFromModel(model)
        Log.d(TAG, "POST createEtape -> baseUrl=$baseUrl lib='${dto.libelle_Etape}' pred='${dto.predecesseur_etape}' succ='${dto.successeur_etape}'")
        return withContext(Dispatchers.IO) {
            val r = api.createEtape(dto)
            logResponse("createEtape", r)
            r
        }
    }

    suspend fun updateEtapeFromModel(context: Context, model: Etape): Response<Void> {
        val baseUrl = ApiConfig.getBaseUrl(context)
        val api = ApiServerClient.create(baseUrl)
        val dto = buildUpdateDtoFromModel(model)
        Log.d(TAG, "PUT updateEtape(${model.id_Etape}) -> pred='${dto.predecesseur_etape}' succ='${dto.successeur_etape}'")
        return withContext(Dispatchers.IO) {
            val r = api.updateEtape(model.id_Etape, dto)
            logResponse("updateEtape", r)
            r
        }
    }

    // ————————————————————————————————————————————
    // Helpers logging retrofit
    // ————————————————————————————————————————————
    private fun <T> logResponse(label: String, r: Response<T>) {
        val err = try { r.errorBody()?.string() } catch (_: Exception) { null }
        Log.d(TAG, "$label resp: code=${r.code()} success=${r.isSuccessful} headers=${r.headers().size} err=${err?.take(800)}")
    }
}
