package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Cyber Radar Scanner Component
 * Displays a futuristic radar scan line, pulsing concentric rings, and the Didehbon eye logo.
 */
@Composable
fun DidehbonRadarScanner(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")

    // Continuous 360-degree rotation for radar sweep
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    // Pulse wave animation
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RingScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Radar Grid & Sweep Canvas
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) * 0.95f

            // Outer ring
            drawCircle(
                color = NeonCyan.copy(alpha = 0.45f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Middle pulsing ring
            drawCircle(
                color = ElectricBlue.copy(alpha = pulseAlpha * 0.5f),
                radius = maxRadius * 0.68f * ringScale,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner ring
            drawCircle(
                color = NeonCyan.copy(alpha = 0.35f),
                radius = maxRadius * 0.38f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crosshair lines
            drawLine(
                color = CyberTeal.copy(alpha = 0.25f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberTeal.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Sweeping radar wedge / line
            rotate(degrees = angle, pivot = center) {
                // Radar beam line
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.1f),
                            NeonCyan.copy(alpha = 0.9f)
                        ),
                        start = center,
                        end = Offset(center.x + maxRadius, center.y)
                    ),
                    start = center,
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 2.5.dp.toPx()
                )

                // Sweep fan gradient behind the line
                val sweepAngle = 45f
                for (step in 1..8) {
                    val currentStepAngle = -sweepAngle * (step / 8f)
                    val rad = (currentStepAngle * PI / 180f).toFloat()
                    val targetX = center.x + maxRadius * cos(rad)
                    val targetY = center.y + maxRadius * sin(rad)
                    val alpha = (1f - (step / 8f)) * 0.25f

                    drawLine(
                        color = NeonCyan.copy(alpha = alpha),
                        start = center,
                        end = Offset(targetX, targetY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Center Logo Image
        Box(
            modifier = Modifier
                .size(size * 0.46f)
                .clip(CircleShape)
                .border(width = 2.dp, color = NeonCyan.copy(alpha = 0.85f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_didehbon_logo_1791359354563),
                contentDescription = "Didehbon Radar Eye Logo",
                modifier = Modifier
                    .size(size * 0.44f)
                    .clip(CircleShape)
            )
        }
    }
}
