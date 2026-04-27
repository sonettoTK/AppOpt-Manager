package com.keran.appoptmanager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.keran.appoptmanager.ui.theme.SoftTeal
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    primaryColor: Color = SoftTeal,
    secondaryColor: Color = primaryColor.copy(alpha = 0.3f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val rotation1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 = infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    val scale1 = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val scale2 = infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.8f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    val alpha1 = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )

    val alpha2 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(64.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.width / 3

            drawCircle(
                color = primaryColor.copy(alpha = alpha1.value),
                radius = radius * scale1.value,
                center = Offset(centerX, centerY)
            )

            drawCircle(
                color = secondaryColor.copy(alpha = alpha2.value),
                radius = radius * scale2.value,
                center = Offset(centerX, centerY),
                style = Stroke(width = 4.dp.toPx())
            )

            val angle1 = Math.toRadians(rotation1.value.toDouble())
            val angle2 = Math.toRadians(rotation2.value.toDouble())

            val dotRadius1 = 6.dp.toPx()
            val dotRadius2 = 4.dp.toPx()
            val orbitRadius = radius * 0.8f

            drawCircle(
                color = primaryColor.copy(alpha = alpha1.value),
                radius = dotRadius1,
                center = Offset(
                    centerX + (cos(angle1) * orbitRadius).toFloat(),
                    centerY + (sin(angle1) * orbitRadius).toFloat()
                )
            )

            drawCircle(
                color = secondaryColor.copy(alpha = alpha2.value),
                radius = dotRadius2,
                center = Offset(
                    centerX + (cos(angle2) * orbitRadius).toFloat(),
                    centerY + (sin(angle2) * orbitRadius).toFloat()
                )
            )

            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = alpha1.value),
                    primaryColor.copy(alpha = 0f)
                ),
                radius = radius * scale1.value * 1.2f
            )

            drawCircle(
                brush = gradientBrush,
                radius = radius * scale1.value * 0.6f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
