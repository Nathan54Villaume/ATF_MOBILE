package com.riva.atsmobile.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun VitesseGauge(
    vitesseActuelle: Float,
    vitesseConsigne: Float,
    vitesseMax: Float = 10f,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
) {
    // On garantit un maximum strictement positif
    val safeMax = if (vitesseMax <= 0f) 1f else vitesseMax

    val animatedVitesse = remember { Animatable(0f) }
    LaunchedEffect(vitesseActuelle) {
        // On borne aussi la valeur animée dans [0, safeMax]
        val target = vitesseActuelle.coerceIn(0f, safeMax)
        animatedVitesse.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h)
        val radius = min(w, h * 2f) / 2.2f

        // Arcs colorés pour les zones
        val arcSize = Size(radius * 2f, radius)
        val topLeft = Offset(center.x - radius, center.y - radius)

        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = 180f, sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = 20f)
        )
        drawArc(
            color = Color(0xFFFFA000),
            startAngle = 240f, sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = 20f)
        )
        drawArc(
            color = Color(0xFFD32F2F),
            startAngle = 300f, sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft, size = arcSize,
            style = Stroke(width = 20f)
        )

        // Calcul de l'angle de l'aiguille
        val angle = 180f + (animatedVitesse.value / safeMax) * 180f
        val rad   = Math.toRadians(angle.toDouble())
        val len   = radius * 0.85f

        val end = Offset(
            x = center.x + cos(rad).toFloat() * len,
            y = center.y + sin(rad).toFloat() * len
        )

        // Aiguille (rouge si on dépasse la consigne)
        drawLine(
            color = if (vitesseActuelle > vitesseConsigne) Color.Red else Color.White,
            start = center, end = end,
            strokeWidth = 8f, cap = StrokeCap.Round
        )

        // Pointe centrale
        drawCircle(
            color = Color.White,
            radius = 10f,
            center = center
        )
    }
}
