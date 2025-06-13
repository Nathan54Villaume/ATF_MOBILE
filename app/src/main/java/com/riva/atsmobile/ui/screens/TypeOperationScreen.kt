package com.riva.atsmobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riva.atsmobile.model.Gamme
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeOperationScreen(
    viewModel: SelectionViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.InitNetworkObserverIfNeeded(context)
        // TODO: replace with real fetch
        viewModel.setGammes(
            listOf(
                Gamme("1","PAF R","5","1.0","10"),
                Gamme("2","PAF C","6","1.2","12"),
                Gamme("3","PAF N","8","1.5","15"),
                Gamme("4","ST 15C","10","2.0","20"),
                Gamme("5","ST 20","12","2.5","25"),
                Gamme("6","ST 25CS","15","3.0","30")
            )
        )
    }

    val isConnected by viewModel.isOnline.collectAsState()
    val gammes by viewModel.gammes.collectAsState()
    val current by viewModel.currentGamme.collectAsState()
    val desired by viewModel.desiredGamme.collectAsState()
    val zone by viewModel.zoneDeTravail.collectAsState()
    val intervention by viewModel.intervention.collectAsState()

    BaseScreen(
        title = "Type d’opération",
        navController = navController,
        viewModel = viewModel,
        showBack = true,
        showLogout = false,
        connectionStatus = isConnected
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Section Gammes
            Text(
                "Sélectionnez vos gammes",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GammeGrid(
                title = "Gamme actuelle",
                gammes = gammes,
                selected = current,
                onSelect = { viewModel.selectCurrentGamme(it) }
            )

            Spacer(Modifier.height(12.dp))

            GammeGrid(
                title = "Gamme souhaitée",
                gammes = gammes,
                selected = desired,
                onSelect = { viewModel.selectDesiredGamme(it) },
                restrict = current
            )

            Spacer(Modifier.height(24.dp))

            // Détails animés
            AnimatedDetails(current = current, desired = desired)

            Spacer(Modifier.weight(1f))

            // Actions
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ElevatedButton(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.width(140.dp)
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retour")
                }
                ElevatedButton(
                    onClick = { viewModel.validateGammeChange { success, msg -> /* Snackbar */ } },
                    enabled = current != null && desired != null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.width(140.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Valider")
                }
            }

            // Footer
            Footer(zone, intervention)
        }
    }
}

@Composable
private fun GammeGrid(
    title: String,
    gammes: List<Gamme>,
    selected: Gamme?,
    onSelect: (Gamme) -> Unit,
    restrict: Gamme? = null
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        items(gammes) { gamme ->
            val isDisabled = restrict != null && gamme == restrict
            val borderColor by animateColorAsState(
                if (gamme == selected) MaterialTheme.colorScheme.primary else Color.Gray,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            val scale by animateFloatAsState(
                targetValue = if (gamme == selected) 1.05f else 1f,
                animationSpec = tween(durationMillis = 300)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                    .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(20.dp))
                    .clickable(enabled = !isDisabled) { onSelect(gamme) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = gamme.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (gamme == selected) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = if (gamme == selected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun AnimatedDetails(current: Gamme?, desired: Gamme?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        current?.let {
            Column {
                Text("Actuelle : ${it.name}", fontWeight = FontWeight.SemiBold)
                Text("Maille: ${it.meshSize} mm")
                Text("Fil: ${it.wireDiameter} mm")
                Text("Chaîne: ${it.chainCount}")
            }
        }
        desired?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text("Souhait : ${it.name}", fontWeight = FontWeight.SemiBold)
                Text("Maille: ${it.meshSize} mm")
                Text("Fil: ${it.wireDiameter} mm")
                Text("Chaîne: ${it.chainCount}")
            }
        }
    }
}

@Composable
private fun Footer(zone: String, intervention: String) {
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Zone: $zone | Interv: $intervention", style = MaterialTheme.typography.bodySmall)
        Text(now, style = MaterialTheme.typography.bodySmall)
    }
}
