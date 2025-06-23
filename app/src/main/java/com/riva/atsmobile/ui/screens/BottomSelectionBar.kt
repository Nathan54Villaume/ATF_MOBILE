package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riva.atsmobile.domain.model.ProcessType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BottomSelectionBar(
    zones: List<String>,
    selectedZone: String,
    onZoneSelected: (String) -> Unit,
    interventions: List<String>,
    selectedIntervention: String,
    onInterventionSelected: (String) -> Unit,
    types: List<ProcessType>,
    selectedType: ProcessType,
    onTypeSelected: (ProcessType) -> Unit,
    currentDateTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zone de travail
        Text("Zone :", style = MaterialTheme.typography.bodySmall)
        DropdownSelector(
            options = zones,
            selected = selectedZone,
            onSelect = onZoneSelected
        )

        // Intervention
        Text("Intervention :", style = MaterialTheme.typography.bodySmall)
        DropdownSelector(
            options = interventions,
            selected = selectedIntervention,
            onSelect = onInterventionSelected
        )

        // Type de process
        Text("Type :", style = MaterialTheme.typography.bodySmall)
        DropdownSelector(
            options = types.map { it.name },
            selected = selectedType.name
        ) { value ->
            onTypeSelected(ProcessType.valueOf(value))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Date/Heure
        Text(
            text = currentDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            style = MaterialTheme.typography.bodySmall
        )
    }
}


