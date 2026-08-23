package com.gamestack.core.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamestack.ui.theme.GameStackTheme

// Reusable shimmer primitive for any loading skeleton — the box shape/layout
// that composes these (grid, carousel, list row) is each screen's own concern.
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Preview(showBackground = true)
@Composable
private fun ShimmerPlaceholderPreview() {
    GameStackTheme {
        ShimmerPlaceholder(modifier = Modifier.width(160.dp).height(200.dp))
    }
}
