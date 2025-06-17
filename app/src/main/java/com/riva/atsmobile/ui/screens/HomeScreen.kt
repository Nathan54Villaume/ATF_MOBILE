package com.riva.atsmobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riva.atsmobile.R
import com.riva.atsmobile.ui.shared.BaseScreen
import com.riva.atsmobile.viewmodel.SelectionViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: SelectionViewModel, navController: NavController) {
    val nom by viewModel.nom.collectAsState()
    val roleOriginal by viewModel.role.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val devMode by viewModel.devModeEnabled.collectAsState()

    val role = if (devMode) "ADMIN" else roleOriginal

    val nomAffiche = when {
        nom.isBlank() -> "utilisateur"
        nom.contains(" ") -> nom.substringAfter(" ")
        nom.contains(".") -> nom.substringAfter(".")
        else -> nom
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isTablet = screenWidthDp > 600

    val logoHeight = when {
        screenHeightDp > 900 -> 320.dp
        screenHeightDp > 700 -> 240.dp
        else -> 160.dp
    }

    val rotation = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
    }

    LaunchedEffect(roleOriginal) {
        if (roleOriginal == "OPERATEUR") {
            delay(1500)
            visible = false
            delay(300)
            navController.navigate("changement_gamme")
        }
    }

    BaseScreen(
        title = "Accueil",
        navController = navController,
        viewModel = viewModel,
        showBack = false,
        showLogout = false,
        connectionStatus = isOnline
    ) { padding ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(durationMillis = 2000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenue ${nomAffiche.trim()} ,",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    when (role) {
                        "ADMIN" -> {
                            if (isTablet) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LogoClickable(
                                        resId = R.drawable.logo2,
                                        height = logoHeight,
                                        rotationZ = rotation.value,
                                        onClick = { navController.navigate("dashboard_ats") }
                                    )
                                    LogoClickable(
                                        resId = R.drawable.logo3,
                                        height = logoHeight,
                                        rotationZ = rotation.value,
                                        onClick = { navController.navigate("dashboard_atr") }
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LogoClickable(
                                        resId = R.drawable.logo2,
                                        height = logoHeight,
                                        rotationZ = rotation.value,
                                        onClick = { navController.navigate("dashboard_atr") }
                                    )
                                    LogoClickable(
                                        resId = R.drawable.logo3,
                                        height = logoHeight,
                                        rotationZ = rotation.value,
                                        onClick = { navController.navigate("dashboard_ats") }
                                    )
                                }
                            }
                        }
                        else -> {
                            AnimatedVisibility(
                                visible = visible,
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ImageRotative(R.drawable.logo2, logoHeight, rotation.value)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageRotative(imageRes: Int, height: Dp, rotationZ: Float) {
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(height)
            .graphicsLayer(rotationZ = rotationZ)
    )
}

@Composable
fun LogoClickable(resId: Int, height: Dp, rotationZ: Float, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = null
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(height)
                .graphicsLayer(rotationZ = rotationZ)
        )
    }
}