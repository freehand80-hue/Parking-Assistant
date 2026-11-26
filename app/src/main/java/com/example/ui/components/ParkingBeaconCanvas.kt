package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Draws a subtle deep-navy architectural telemetry grid and top-right beacon glow
 * that preserves high outdoor contrast while giving the screen distinctive depth.
 */
fun Modifier.parkingNavyBackdrop(
    baseColor: Color,
    accentGlow: Color,
    gridLineColor: Color
): Modifier = this.drawBehind {
    drawRect(color = baseColor)

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                accentGlow.copy(alpha = 0.14f),
                accentGlow.copy(alpha = 0.04f),
                Color.Transparent
            ),
            center = Offset(x = size.width * 0.85f, y = size.height * 0.08f),
            radius = size.maxDimension * 0.65f
        )
    )

    val spacingPx = 36.dp.toPx()
    val strokePx = 1.dp.toPx()
    val lineColor = gridLineColor.copy(alpha = 0.16f)

    var x = spacingPx
    while (x < size.width) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokePx
        )
        x += spacingPx
    }

    var y = spacingPx
    while (y < size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx
        )
        y += spacingPx
    }
}
