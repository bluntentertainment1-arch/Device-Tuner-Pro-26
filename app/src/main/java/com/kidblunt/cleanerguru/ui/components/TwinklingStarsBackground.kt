package com.kidblunt.cleanerguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kidblunt.cleanerguru.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun TwinklingStarsBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 30,
    starColor: Color = NeonPink.copy(alpha = 0.8f)
) {
    val density = LocalDensity.current
    val genZColors = listOf(NeonPink, ElectricBlue, VibrantPurple, NeonGreen, SunsetOrange)
    
    val stars = remember {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = with(density) { Random.nextFloat() * 4.dp.toPx() + 2.dp.toPx() },
                speed = Random.nextFloat() * 3f + 1f,
                phase = Random.nextFloat() * 2f * Math.PI.toFloat(),
                color = genZColors.random().copy(alpha = Random.nextFloat() * 0.8f + 0.2f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Gradient background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    VibrantPurple.copy(alpha = 0.1f),
                    NeonPink.copy(alpha = 0.05f),
                    ElectricBlue.copy(alpha = 0.1f)
                ),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.width * 0.8f
            )
        )
        
        drawTwinklingStars(stars, time, colorShift, genZColors, size)
    }
}

private fun DrawScope.drawTwinklingStars(
    stars: List<Star>,
    time: Float,
    colorShift: Float,
    genZColors: List<Color>,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    stars.forEach { star ->
        val alpha = (sin(time * star.speed + star.phase) + 1f) / 2f
        val colorIndex = ((colorShift + star.phase) * genZColors.size).toInt() % genZColors.size
        val currentColor = genZColors[colorIndex].copy(alpha = alpha * 0.9f)
        
        val x = star.x * canvasSize.width
        val y = star.y * canvasSize.height
        
        // Draw main star with glow effect
        val glowRadius = star.size * (1.5f + alpha * 0.5f)
        drawCircle(
            color = currentColor.copy(alpha = alpha * 0.3f),
            radius = glowRadius,
            center = Offset(x, y)
        )
        
        drawCircle(
            color = currentColor,
            radius = star.size,
            center = Offset(x, y)
        )
        
        // Draw dynamic twinkling effect
        val twinkleSize = star.size * (alpha + 1f)
        val twinkleAlpha = alpha * 0.8f
        
        // Horizontal line
        drawLine(
            color = currentColor.copy(alpha = twinkleAlpha),
            start = Offset(x - twinkleSize, y),
            end = Offset(x + twinkleSize, y),
            strokeWidth = 2f
        )
        
        // Vertical line
        drawLine(
            color = currentColor.copy(alpha = twinkleAlpha),
            start = Offset(x, y - twinkleSize),
            end = Offset(x, y + twinkleSize),
            strokeWidth = 2f
        )
        
        // Diagonal lines for extra sparkle
        val diagonalSize = twinkleSize * 0.7f
        drawLine(
            color = currentColor.copy(alpha = twinkleAlpha * 0.6f),
            start = Offset(x - diagonalSize, y - diagonalSize),
            end = Offset(x + diagonalSize, y + diagonalSize),
            strokeWidth = 1f
        )
        drawLine(
            color = currentColor.copy(alpha = twinkleAlpha * 0.6f),
            start = Offset(x - diagonalSize, y + diagonalSize),
            end = Offset(x + diagonalSize, y - diagonalSize),
            strokeWidth = 1f
        )
    }
}