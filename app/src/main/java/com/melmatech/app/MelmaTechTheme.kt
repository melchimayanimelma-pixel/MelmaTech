package com.melmatech.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design system centralisé de MelmaTech.
 * Les composants Material 3 (boutons, champs, cartes, menus et dialogues)
 * héritent de cette palette et de ces formes dans toute l'application.
 */
private val MelmaTechColors = lightColorScheme(
    primary = Color(0xFF6C3CE9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE5FF),
    onPrimaryContainer = Color(0xFF321078),
    secondary = Color(0xFF315F9B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EDFF),
    onSecondaryContainer = Color(0xFF183B68),
    tertiary = Color(0xFF16836B),
    onTertiary = Color.White,
    background = Color(0xFFF6F5FA),
    onBackground = Color(0xFF201D29),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201D29),
    surfaceVariant = Color(0xFFF0EDF5),
    onSurfaceVariant = Color(0xFF686371),
    outline = Color(0xFF8D8798),
    outlineVariant = Color(0xFFE2DEE9),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val MelmaTechShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MelmaTechTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MelmaTechColors,
        typography = Typography(),
        shapes = MelmaTechShapes,
        content = content
    )
}
