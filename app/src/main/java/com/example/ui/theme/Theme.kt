package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HighContrastNavyColorScheme = darkColorScheme(
    primary = SignalYellow,
    onPrimary = OnSignalYellow,
    primaryContainer = SignalYellowDim,
    onPrimaryContainer = SignalYellowBright,
    secondary = PureWhite,
    onSecondary = DeepNavy,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = PureWhite,
    tertiary = SignalMint,
    onTertiary = DeepNavy,
    tertiaryContainer = SignalMintContainer,
    onTertiaryContainer = SignalMint,
    background = DeepNavy,
    onBackground = PureWhite,
    surface = NavySurface,
    onSurface = PureWhite,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = SoftIceWhite,
    surfaceContainerLowest = MidnightNavy,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavySurfaceElevated,
    surfaceContainerHigh = NavySurfaceHigh,
    outline = NavyOutline,
    outlineVariant = NavyOutlineBright,
    error = BrightCoral,
    onError = DeepNavy,
    errorContainer = BrightCoralContainer,
    onErrorContainer = BrightCoral
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyCarParkingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighContrastNavyColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
