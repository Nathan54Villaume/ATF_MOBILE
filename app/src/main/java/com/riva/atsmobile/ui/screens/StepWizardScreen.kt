package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.ChangeoverViewModel
import com.riva.atsmobile.viewmodel.SelectionViewModel
import com.riva.atsmobile.domain.model.ProcessType
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepWizardScreen(
    selectionViewModel: SelectionViewModel,
    changeoverViewModel: ChangeoverViewModel,
    navController: NavController
) {
    // 1) Statut réseau
    val isConnected by selectionViewModel.isOnline.collectAsState()

    // 2) On récupère la liste des désignations (List<String>)
    val designationsList: List<String> = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get<List<String>>("selectedDesignations")
        ?: emptyList()

    // 3) Conversion en Set pour le VM
    val selectedDesignations = designationsList.toSet()

    // 4) Initialise le VM une seule fois
    LaunchedEffect(selectedDesignations) {
        changeoverViewModel.initWithSelectedGammes(selectedDesignations)
    }

    // 5) Collecte des états
    val operators by changeoverViewModel.operatorSteps.collectAsState()
    val zones     by changeoverViewModel.zoneOptions.collectAsState()
    val inters    by changeoverViewModel.interventionOptions.collectAsState()
    val selZone   by changeoverViewModel.selectedZone.collectAsState()
    val selInt    by changeoverViewModel.selectedIntervention.collectAsState()
    val selType   by changeoverViewModel.selectedType.collectAsState()

    // 6) Affichage
    BaseScreen(
        title            = if (selectedDesignations.isEmpty()) {
            "Changement de gamme"
        } else {
            "Changement de gamme : " + selectedDesignations.joinToString(", ")
        },
        navController    = navController,
        viewModel        = selectionViewModel,
        showBack         = true,
        showLogout       = false,
        connectionStatus = isConnected
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 6.1) Grille de cartes opérateur (prend tout l'espace restant)
            Row(
                modifier = Modifier
                    .weight(1f)         // ← here!
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                operators.forEachIndexed { idx, state ->
                    OperatorStepCard(
                        state     = state,
                        onPrev    = { changeoverViewModel.onPrevStep(idx) },
                        onNext    = { changeoverViewModel.onNextStep(idx) },
                        onFinish  = { changeoverViewModel.onFinishStep(idx) },
                        onComment = { comment ->
                            changeoverViewModel.onCommentChanged(idx, comment)
                        },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 6.2) Barre de sélection
            BottomSelectionBar(
                zones                  = zones,
                selectedZone           = selZone,
                onZoneSelected         = { changeoverViewModel.onZoneSelected(it) },
                interventions          = inters,
                selectedIntervention   = selInt,
                onInterventionSelected = { changeoverViewModel.onInterventionSelected(it) },
                types                  = ProcessType.values().toList(),
                selectedType           = selType,
                onTypeSelected         = { changeoverViewModel.onProcessTypeSelected(it) },
                currentDateTime        = LocalDateTime.now(),
                modifier               = Modifier.fillMaxWidth()
            )
        }
    }
}
