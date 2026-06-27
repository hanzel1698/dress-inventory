package com.hanzel.dressinventory.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF7B4B6E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6D9EC),
    onPrimaryContainer = Color(0xFF31102A),
    secondary = Color(0xFF6F5B62),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8DEE6),
    onSecondaryContainer = Color(0xFF28181E),
    tertiary = Color(0xFF815341),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF321205),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF211A1E),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF211A1E),
    surfaceVariant = Color(0xFFF0DEE5),
    onSurfaceVariant = Color(0xFF4F4449),
    outline = Color(0xFF81737A),
    surfaceContainer = Color(0xFFFBEEF2),
    surfaceContainerHigh = Color(0xFFF5E8ED),
    surfaceContainerHighest = Color(0xFFEFE3E7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE9B8D8),
    onPrimary = Color(0xFF48203F),
    primaryContainer = Color(0xFF613756),
    onPrimaryContainer = Color(0xFFF6D9EC),
    secondary = Color(0xFFDBC2CA),
    onSecondary = Color(0xFF3F2D34),
    secondaryContainer = Color(0xFF56434A),
    onSecondaryContainer = Color(0xFFF8DEE6),
    tertiary = Color(0xFFF5B9A2),
    onTertiary = Color(0xFF4B2617),
    tertiaryContainer = Color(0xFF663C2B),
    onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF181216),
    onBackground = Color(0xFFEDDFE4),
    surface = Color(0xFF181216),
    onSurface = Color(0xFFEDDFE4),
    surfaceVariant = Color(0xFF4F4449),
    onSurfaceVariant = Color(0xFFD3C2C9),
    outline = Color(0xFF9C8D93),
    surfaceContainer = Color(0xFF251E22),
    surfaceContainerHigh = Color(0xFF2F282C),
    surfaceContainerHighest = Color(0xFF3A3337),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = t.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun DressTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
