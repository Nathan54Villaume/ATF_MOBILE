package com.riva.atsmobile.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun VitesseGauge(
    vitesseActuelle: Float,
    vitesseConsigne: Float,
    vitesseMax: Float = 10f,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp) // Hauteur un peu plus grande pour le texte
) {
    // 1) Animation de la vitesse
    val animatedV = remember { Animatable(0f) }
    LaunchedEffect(vitesseActuelle) {
        animatedV.animateTo(
            targetValue = vitesseActuelle.coerceAtMost(vitesseMax),
            animationSpec = tween(durationMillis = 600)
        )
    }

    // Couleur du centre, lue _avant_ le Canvas
    val centerColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 2) Le gauge
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(x = w / 2, y = h)
            val radius = min(w, h * 2) / 2.2f

            val arcSize = Size(radius * 2, radius)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Zones vert / orange / rouge
            drawArc(
                color = Color(0xFF4CAF50), startAngle = 180f, sweepAngle = 60f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = 20f)
            )
            drawArc(
                color = Color(0xFFFFA000), startAngle = 240f, sweepAngle = 60f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = 20f)
            )
            drawArc(
                color = Color(0xFFD32F2F), startAngle = 300f, sweepAngle = 60f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = 20f)
            )

            // Aiguille
            val angle = 180f + (animatedV.value / vitesseMax) * 180f
            val rad   = Math.toRadians(angle.toDouble())
            val len   = radius * 0.85f
            val end   = Offset(
                x = center.x + cos(rad).toFloat() * len,
                y = center.y + sin(rad).toFloat() * len
            )

            drawLine(
                color = if (animatedV.value > vitesseConsigne) Color.Red else Color.White,
                start = center, end = end,
                strokeWidth = 8f, cap = StrokeCap.Round
            )

            // Centre de l'aiguille
            drawCircle(
                color = centerColor,
                radius = 10f,
                center = center
            )
        }

        // 3) Les textes superposés
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // actuelle, formatée à 1 décimale
            Text(
                text = "${"%.1f".format(animatedV.value)} m/s",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            // consigne
            Text(
                text = "Cible : ${"%.1f".format(vitesseConsigne)} m/s",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}
