package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riva.atsmobile.domain.model.OperatorStepState

@Composable
fun OperatorStepCard(
    state: OperatorStepState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. En-tête (nom opérateur, étape, durée)
            Text(text = state.operatorName, style = MaterialTheme.typography.titleMedium)
            Text(text = "Étape ${state.currentStep}/${state.totalSteps}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Durée estimée : ${state.estimatedDuration} min", style = MaterialTheme.typography.bodySmall)

            Divider()

            // 2. Titre et description
            Text(text = state.stepTitle, style = MaterialTheme.typography.titleSmall)
            Text(text = state.stepDescription, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            // 3. Commentaire
            OutlinedTextField(
                value = state.comment,
                onValueChange = onComment,
                label = { Text("Commentaire") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Chrono et progression
            Text(text = "${state.elapsedMinutes} min ${state.elapsedSeconds} sec", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = state.progressPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(Modifier.weight(1f))

            // 5. Navigation
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentStep > 1) {
                    Button(onClick = onPrev) { Text("Précédent") }
                } else {
                    Spacer(Modifier.width(100.dp))
                }

                if (state.currentStep < state.totalSteps) {
                    Button(onClick = onNext) { Text("Suivant") }
                } else {
                    Button(onClick = onFinish) { Text("Terminer étape") }
                }
            }
        }
    }
}
