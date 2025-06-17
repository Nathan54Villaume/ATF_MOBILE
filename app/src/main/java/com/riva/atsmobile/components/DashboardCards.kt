package com.riva.atsmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrefileuseCard(
    titre: String,
    isActive: Boolean,
    vitesseActuelle: Float,
    vitesseConsigne: Float,
    diametre: Float,
    longueurBobine: Float,
    poidsBobine: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Vitesse", "${vitesseActuelle} m/s (cible: $vitesseConsigne)")
            InfoRow("Diamètre", "$diametre mm")
            InfoRow("Longueur bobine", "$longueurBobine m")
            InfoRow("Poids bobine", "$poidsBobine kg")
        }
    }
}

@Composable
fun SoudeuseCard(
    titre: String,
    isActive: Boolean,
    vitesseActuelle: Float,
    vitesseConsigne: Float,
    diamFil: Float,
    diamTrame: Float,
    longueur: Float,
    largeur: Float,
    energie: Int,
    nbPanneaux: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Vitesse", "${vitesseActuelle} m/min (cible: $vitesseConsigne)")
            InfoRow("Diam. fil / trame", "$diamFil / $diamTrame mm")
            InfoRow("Dim. panneau", "$longueur x $largeur mm")
            InfoRow("Énergie", "$energie Wh")
            InfoRow("Panneaux/pac", "$nbPanneaux")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}