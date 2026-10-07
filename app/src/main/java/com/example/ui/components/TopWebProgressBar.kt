package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.NeonCyan

@Composable
fun TopWebProgressBar(
    progress: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading && progress < 100,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = (progress.coerceIn(0, 100)) / 100f,
            animationSpec = tween(durationMillis = 200),
            label = "TopProgress"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = NeonCyan,
            trackColor = CyberNavyDark.copy(alpha = 0.5f),
            strokeCap = StrokeCap.Square
        )
    }
}
