package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val accent: Color,
    val isLight: Boolean
)

val PurplePalette = AppColors(
    primary = Color(0xFF6C5CE7),
    background = Color(0xFF140D2F),
    surface = Color(0xFF1F1240),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFBB86EC),
    isLight = false
)

val RedPalette = AppColors(
    primary = Color(0xFFB33939),
    background = Color(0xFF2C0B0B),
    surface = Color(0xFF3F1313),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFEA8685),
    isLight = false
)

val GreenPalette = AppColors(
    primary = Color(0xFF16A085),
    background = Color(0xFF0A2B23),
    surface = Color(0xFF104639),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF86ECDB),
    isLight = false
)

val DarkPalette = AppColors(
    primary = Color(0xFFBB86EC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF03DAC6),
    isLight = false
)

val HighContrastPalette = AppColors(
    primary = Color(0xFFFFFF00),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFFFFF00),
    isLight = false
)
