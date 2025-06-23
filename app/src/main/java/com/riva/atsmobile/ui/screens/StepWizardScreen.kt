package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
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
    // Statut réseau
    val isConnected by selectionViewModel.isOnline.collectAsState()

    // Récupérer les gammes sélectionnées transmises depuis ChangementGammeScreen
    val saved: Set<String> = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get("selectedGammes")
        ?: emptySet()

    // Initialise le VM une seule fois avec ces gammes
    LaunchedEffect(saved) {
        changeoverViewModel.initWithSelectedGammes(saved)
    }

    // Flux d’état composables
    val operators by changeoverViewModel.operatorSteps.collectAsState()
    val zones     by changeoverViewModel.zoneOptions.collectAsState()
    val inters    by changeoverViewModel.interventionOptions.collectAsState()
    val selZone   by changeoverViewModel.selectedZone.collectAsState()
    val selInt    by changeoverViewModel.selectedIntervention.collectAsState()
    val selType   by changeoverViewModel.selectedType.collectAsState()

    BaseScreen(
        title            = "Changement de gamme",
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
            // 1) Grille de cartes opérateur
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                operators.forEachIndexed { idx, state ->
                    OperatorStepCard(
                        state     = state,
                        onPrev    = { changeoverViewModel.onPrevStep(idx) },
                        onNext    = { changeoverViewModel.onNextStep(idx) },
                        onFinish  = { changeoverViewModel.onFinishStep(idx) },
                        onComment = { comment -> changeoverViewModel.onCommentChanged(idx, comment) },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2) Barre de sélection
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
