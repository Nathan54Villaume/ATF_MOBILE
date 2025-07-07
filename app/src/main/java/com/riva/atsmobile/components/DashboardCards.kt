package com.riva.atsmobile.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrefileuseCard(
    titre: String,
    // isActive: Boolean, // Ce paramètre n'est plus nécessaire ici
    vitesseActuelle: Float,
    vitesseConsigne: Float,
    diametre: Float,
    longueurBobine: Float,
    poidsBobine: Float
) {
    var expanded by remember { mutableStateOf(false) } // État d'expansion de la carte

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 300)), // Animation pour l'expansion
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF333333), Color(0xFF222222))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { expanded = !expanded } // Rendre la carte cliquable pour expand/collapse
                .padding(16.dp)
        ) {
            // En-tête (toujours visible)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64FFDA)
                )
                // Résumé dans l'en-tête (visible même quand replié)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$diametre mm", // Afficher le diamètre
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "$vitesseActuelle m/s", // Vitesse actuelle
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (vitesseActuelle > 0f) Color(0xFF00C853) else Color(0xFFD50000),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Replier" else "Dérouler",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Contenu déroulant
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("Vitesse consigne", "$vitesseConsigne m/s") // Correction m/s
                Spacer(modifier = Modifier.height(4.dp)) // Espacement réduit après la première ligne
                InfoRow("Longueur bobine", "$longueurBobine m")
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Poids bobine", "${poidsBobine.toInt()} kg")
            }
        }
    }
}

@Composable
fun SoudeuseCard(
    titre: String,
    // isActive: Boolean, // Ce paramètre n'est plus nécessaire si la pastille est basée sur la vitesse
    vitesseActuelle: Float,   // en m/s
    vitesseConsigne: Float,   // en m/s
    diamFil: Float,
    diamTrame: Float,
    longueur: Float,
    largeur: Float,
    energie: Int,
    nbPanneaux: Int
) {
    var expanded by remember { mutableStateOf(false) } // État d'expansion de la carte

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 300)), // Animation pour l'expansion
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF333333), Color(0xFF222222))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { expanded = !expanded } // Rendre la carte cliquable pour expand/collapse
                .padding(16.dp)
        ) {
            // En-tête (toujours visible)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color =  Color(0xFF64FFDA)
                )
                // Résumé dans l'en-tête (visible même quand replié)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$vitesseActuelle m/s", // Vitesse actuelle
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                // MODIFICATION ICI : Pastille de Soudeuse basée sur vitesseActuelle
                                color = if (vitesseActuelle > 0f) Color(0xFF00C853) else Color(0xFFD50000),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Replier" else "Dérouler",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Contenu déroulant
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("Vitesse consigne", "$vitesseConsigne m/s") // Correction m/s
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Diam. fil / trame", "$diamFil / $diamTrame mm")
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Dim. panneau", "$longueur x $largeur mm")
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Énergie", "$energie Wh")
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Panneaux/pac", "$nbPanneaux")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFFBDBDBD),
            fontSize = 16.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}