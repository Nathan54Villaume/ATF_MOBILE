package com.riva.atsmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel

@Composable
fun TypeOperationParamScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val gammes by viewModel.gammes.collectAsState()
    val selectedGammes = remember { mutableStateListOf<String>() }

    BaseScreen(
        title = "Paramétrage Gammes",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Sélectionnez les gammes visibles dans la sélection", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(gammes) { gamme ->
                    val isChecked = selectedGammes.contains(gamme.codeTreillis)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) selectedGammes.add(gamme.codeTreillis)
                                else selectedGammes.remove(gamme.codeTreillis)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(gamme.designation)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    // TODO : sauvegarder cette liste si besoin
                    navController.popBackStack()
                }) {
                    Text("Enregistrer")
                }
            }
        }
    }
}
