package com.riva.atsmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riva.atsmobile.data.local.EtapeEntity
import com.riva.atsmobile.data.repository.EtapesRepository
import com.riva.atsmobile.domain.model.OperatorStepState
import com.riva.atsmobile.domain.model.ProcessType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChangeoverViewModel(
    private val repo: EtapesRepository
) : ViewModel() {

    private val allEntities = MutableStateFlow<List<EtapeEntity>>(emptyList())
    private val _operatorSteps = MutableStateFlow<List<OperatorStepState>>(emptyList())
    val operatorSteps: StateFlow<List<OperatorStepState>> = _operatorSteps.asStateFlow()

    private val _zoneOptions = MutableStateFlow<List<String>>(emptyList())
    val zoneOptions: StateFlow<List<String>> = _zoneOptions.asStateFlow()

    private val _interventionOptions = MutableStateFlow<List<String>>(emptyList())
    val interventionOptions: StateFlow<List<String>> = _interventionOptions.asStateFlow()

    private val _selectedZone = MutableStateFlow("")
    val selectedZone: StateFlow<String> = _selectedZone.asStateFlow()

    private val _selectedIntervention = MutableStateFlow("")
    val selectedIntervention: StateFlow<String> = _selectedIntervention.asStateFlow()

    private val _selectedType = MutableStateFlow(ProcessType.MINEUR)
    val selectedType: StateFlow<ProcessType> = _selectedType.asStateFlow()

    init {
        loadSteps()
    }

    private fun loadSteps() {
        viewModelScope.launch {
            val entities = repo.getAllSteps()
            allEntities.value = entities

            // Options distinctes
            _zoneOptions.value = entities.mapNotNull { it.affectationEtape }.distinct()
            _interventionOptions.value = entities.mapNotNull { it.phaseEtape }.distinct()

            // Sélections par défaut
            _selectedZone.value = _zoneOptions.value.firstOrNull().orEmpty()
            _selectedIntervention.value = _interventionOptions.value.firstOrNull().orEmpty()

            // Initialiser états opérateurs
            val grouped = entities.groupBy { it.affectationEtape.orEmpty() }
            _operatorSteps.value = grouped.map { (operator, list) ->
                val first = list.first()
                OperatorStepState(
                    operatorName      = operator,
                    currentStep       = 1,
                    totalSteps        = list.size,
                    stepTitle         = first.libelleEtape,
                    stepDescription   = first.descriptionEtape.orEmpty(),
                    estimatedDuration = first.dureeEtape ?: 0,
                    elapsedMinutes    = first.tempsReelEtape ?: 0,
                    elapsedSeconds    = 0,
                    progressPercent   = 0,
                    comment           = first.commentaireEtape1.orEmpty(),
                    zone              = _selectedZone.value,
                    intervention      = _selectedIntervention.value,
                    processType       = _selectedType.value
                )
            }
        }
    }

    fun onNextStep(index: Int) {
        viewModelScope.launch {
            val currentList = _operatorSteps.value.toMutableList()
            val state = currentList[index]
            if (state.currentStep < state.totalSteps) {
                val operatorEntities = allEntities.value.filter { it.affectationEtape == state.operatorName }
                val nextEntity = operatorEntities.getOrNull(state.currentStep)
                if (nextEntity != null) {
                    val updated = state.copy(
                        currentStep       = state.currentStep + 1,
                        stepTitle         = nextEntity.libelleEtape,
                        stepDescription   = nextEntity.descriptionEtape.orEmpty(),
                        estimatedDuration = nextEntity.dureeEtape ?: 0,
                        elapsedMinutes    = nextEntity.tempsReelEtape ?: 0,
                        elapsedSeconds    = 0,
                        progressPercent   = ((state.currentStep) * 100 / state.totalSteps),
                        comment           = nextEntity.commentaireEtape1.orEmpty()
                    )
                    currentList[index] = updated
                    _operatorSteps.value = currentList
                    repo.updateStep(
                        nextEntity.copy(
                            tempsReelEtape   = updated.elapsedMinutes,
                            commentaireEtape1 = updated.comment
                        )
                    )
                }
            }
        }
    }

    fun onPrevStep(index: Int) {
        viewModelScope.launch {
            val currentList = _operatorSteps.value.toMutableList()
            val state = currentList[index]
            if (state.currentStep > 1) {
                val operatorEntities = allEntities.value.filter { it.affectationEtape == state.operatorName }
                val prevEntity = operatorEntities.getOrNull(state.currentStep - 2)
                if (prevEntity != null) {
                    val updated = state.copy(
                        currentStep       = state.currentStep - 1,
                        stepTitle         = prevEntity.libelleEtape,
                        stepDescription   = prevEntity.descriptionEtape.orEmpty(),
                        estimatedDuration = prevEntity.dureeEtape ?: 0,
                        elapsedMinutes    = prevEntity.tempsReelEtape ?: 0,
                        elapsedSeconds    = 0,
                        progressPercent   = ((state.currentStep - 2) * 100 / state.totalSteps),
                        comment           = prevEntity.commentaireEtape1.orEmpty()
                    )
                    currentList[index] = updated
                    _operatorSteps.value = currentList
                    repo.updateStep(
                        prevEntity.copy(
                            tempsReelEtape   = updated.elapsedMinutes,
                            commentaireEtape1 = updated.comment
                        )
                    )
                }
            }
        }
    }

    fun onFinishStep(index: Int) {
        viewModelScope.launch {
            val state = _operatorSteps.value[index]
            // Marquer comme terminé : progression à 100%
            val updated = state.copy(progressPercent = 100)
            _operatorSteps.value = _operatorSteps.value.toMutableList().also { it[index] = updated }
            // Persister
            val entity = allEntities.value
                .filter { it.affectationEtape == state.operatorName }
                .lastOrNull()
            entity?.let {
                repo.updateStep(
                    it.copy(
                        tempsReelEtape   = updated.elapsedMinutes,
                        commentaireEtape1 = updated.comment
                    )
                )
            }
        }
    }

    fun onCommentChanged(index: Int, comment: String) {
        viewModelScope.launch {
            val currentList = _operatorSteps.value.toMutableList()
            val state = currentList[index]
            val updated = state.copy(comment = comment)
            currentList[index] = updated
            _operatorSteps.value = currentList
            val entity = allEntities.value
                .filter { it.affectationEtape == state.operatorName }
                .getOrNull(state.currentStep - 1)
            entity?.let {
                repo.updateStep(
                    it.copy(commentaireEtape1 = comment)
                )
            }
        }
    }

    fun onZoneSelected(zone: String) {
        viewModelScope.launch {
            _selectedZone.value = zone
            _operatorSteps.value = _operatorSteps.value.map { it.copy(zone = zone) }
        }
    }

    fun onInterventionSelected(intervention: String) {
        viewModelScope.launch {
            _selectedIntervention.value = intervention
            _operatorSteps.value = _operatorSteps.value.map { it.copy(intervention = intervention) }
        }
    }

    fun onProcessTypeSelected(type: ProcessType) {
        viewModelScope.launch {
            _selectedType.value = type
            _operatorSteps.value = _operatorSteps.value.map { it.copy(processType = type) }
        }
    }
}
