package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FreshKitchenLightScheme = lightColorScheme(
    primary = BasilGreen,
    onPrimary = Color.White,
    primaryContainer = BasilGreenLight,
    onPrimaryContainer = BasilGreenDark,
    secondary = SaffronDark,
    onSecondary = Color.White,
    secondaryContainer = SaffronLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Saffron,
    onTertiary = Color(0xFF3E2700),
    background = WarmIvory,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = WarmOutline,
    outlineVariant = Color(0xFFE6DFCC),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val FreshKitchenDarkScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF0A2E0D),
    primaryContainer = Color(0xFF1E4620),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Saffron,
    onSecondary = Color(0xFF3B2100),
    secondaryContainer = Color(0xFF5A3700),
    onSecondaryContainer = SaffronLight,
    tertiary = SaffronContainer,
    onTertiary = Color(0xFF3B2100),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C4B4),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2E3A2E),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val ArthFoodShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ArthFoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FreshKitchenDarkScheme else FreshKitchenLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ArthFoodShapes,
        content = content
    )
}
