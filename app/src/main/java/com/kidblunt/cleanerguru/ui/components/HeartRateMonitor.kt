package com.kidblunt.cleanerguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidblunt.cleanerguru.ui.theme.PinkPrimary
import kotlin.math.sin

@Composable
fun HeartRateMonitor(
    modifier: Modifier = Modifier,
    heartRate: Int = 72,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val heartBeat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (60000 / heartRate),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated heart icon
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Heart Rate",
                tint = if (isActive) {
                    PinkPrimary.copy(alpha = 0.7f + heartBeat * 0.3f)
                } else {
                    PinkPrimary.copy(alpha = 0.5f)
                },
                modifier = Modifier.size((24 + heartBeat * 4).dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Device Health",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "$heartRate BPM",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                    color = PinkPrimary
                )
            }

            // Heart rate wave visualization
            Canvas(
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
            ) {
                if (isActive) {
                    drawHeartRateWave(wavePhase, PinkPrimary)
                }
            }
        }
    }
}

private fun DrawScope.drawHeartRateWave(
    phase: Float,
    color: Color
) {
    val path = Path()
    val width = size.width
    val height = size.height
    val centerY = height / 2f

    path.moveTo(0f, centerY)

    for (x in 0..width.toInt()) {
        val normalizedX = x / width
        val waveY = centerY + sin((normalizedX * 4 * Math.PI + phase).toFloat()) * (height * 0.3f)
        
        // Create heartbeat-like spikes
        val spikeIntensity = when {
            normalizedX % 0.25f < 0.05f -> sin((normalizedX * 40 * Math.PI).toFloat()) * height * 0.4f
            else -> 0f
        }
        
        path.lineTo(x.toFloat(), waveY + spikeIntensity)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx())
    )
}