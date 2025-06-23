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

    /** Initialize with the selected gamme codes */
    fun initWithSelectedGammes(selected: Set<String>) {
        viewModelScope.launch {
            val loaded = repo.getAllSteps()
                .filter { selected.contains(it.libelleEtape) }
            allEntities.value = loaded
            buildStateFromEntities(loaded)
        }
    }

    /** Build UI state from loaded entities */
    private fun buildStateFromEntities(entities: List<EtapeEntity>) {
        // Populate dropdown options
        _zoneOptions.value = entities.mapNotNull { it.affectationEtape }.distinct()
        _interventionOptions.value = entities.mapNotNull { it.phaseEtape }.distinct()

        // Default selections
        _selectedZone.value = _zoneOptions.value.firstOrNull().orEmpty()
        _selectedIntervention.value = _interventionOptions.value.firstOrNull().orEmpty()

        // Initial operator step states
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

    /** Move to next step for operator at index */
    fun onNextStep(index: Int) {
        viewModelScope.launch {
            val list = _operatorSteps.value.toMutableList()
            val state = list[index]
            if (state.currentStep < state.totalSteps) {
                val ops = allEntities.value.filter { it.affectationEtape == state.operatorName }
                val next = ops.getOrNull(state.currentStep)
                next?.let {
                    val updated = state.copy(
                        currentStep       = state.currentStep + 1,
                        stepTitle         = it.libelleEtape,
                        stepDescription   = it.descriptionEtape.orEmpty(),
                        estimatedDuration = it.dureeEtape ?: 0,
                        elapsedMinutes    = it.tempsReelEtape ?: 0,
                        elapsedSeconds    = 0,
                        progressPercent   = (state.currentStep * 100 / state.totalSteps),
                        comment           = it.commentaireEtape1.orEmpty()
                    )
                    list[index] = updated
                    _operatorSteps.value = list
                    repo.updateStep(it.copy(
                        tempsReelEtape   = updated.elapsedMinutes,
                        commentaireEtape1 = updated.comment
                    ))
                }
            }
        }
    }

    /** Move to previous step */
    fun onPrevStep(index: Int) {
        viewModelScope.launch {
            val list = _operatorSteps.value.toMutableList()
            val state = list[index]
            if (state.currentStep > 1) {
                val ops  = allEntities.value.filter { it.affectationEtape == state.operatorName }
                val prev = ops.getOrNull(state.currentStep - 2)
                prev?.let {
                    val updated = state.copy(
                        currentStep       = state.currentStep - 1,
                        stepTitle         = it.libelleEtape,
                        stepDescription   = it.descriptionEtape.orEmpty(),
                        estimatedDuration = it.dureeEtape ?: 0,
                        elapsedMinutes    = it.tempsReelEtape ?: 0,
                        elapsedSeconds    = 0,
                        progressPercent   = ((state.currentStep - 2) * 100 / state.totalSteps),
                        comment           = it.commentaireEtape1.orEmpty()
                    )
                    list[index] = updated
                    _operatorSteps.value = list
                    repo.updateStep(it.copy(
                        tempsReelEtape   = updated.elapsedMinutes,
                        commentaireEtape1 = updated.comment
                    ))
                }
            }
        }
    }

    /** Mark step finished */
    fun onFinishStep(index: Int) {
        viewModelScope.launch {
            val list = _operatorSteps.value.toMutableList()
            val state = list[index]
            val updated = state.copy(progressPercent = 100)
            list[index] = updated
            _operatorSteps.value = list
            allEntities.value
                .filter { it.affectationEtape == state.operatorName }
                .lastOrNull()
                ?.let { repo.updateStep(it.copy(
                    tempsReelEtape   = updated.elapsedMinutes,
                    commentaireEtape1 = updated.comment
                )) }
        }
    }

    /** Update comment */
    fun onCommentChanged(index: Int, comment: String) {
        viewModelScope.launch {
            val list = _operatorSteps.value.toMutableList()
            val state = list[index]
            val updated = state.copy(comment = comment)
            list[index] = updated
            _operatorSteps.value = list
            allEntities.value
                .filter { it.affectationEtape == state.operatorName }
                .getOrNull(state.currentStep - 1)
                ?.let { repo.updateStep(it.copy(commentaireEtape1 = comment)) }
        }
    }

    /** Selection callbacks */
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