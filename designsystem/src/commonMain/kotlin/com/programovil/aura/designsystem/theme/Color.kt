package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val background: Color, val surface: Color, val surfaceVariant: Color,
    val textPrimary: Color, val textSecondary: Color,
    val accent: Color, val onAccent: Color,
    val outline: Color, val controlOutline: Color,
    val success: Color, val successContainer: Color,
    val warning: Color, val warningContainer: Color,
    val error: Color, val onError: Color, val errorContainer: Color,
    val hero: Color, val onHero: Color = Color.White,
    val isLight: Boolean, val highContrast: Boolean = false
)

val PurplePalette = AppColors(
    primary = Color(0xFF5B4DDE), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E3FC), onPrimaryContainer = Color(0xFF30245F),
    background = Color(0xFFF7F5F2), surface = Color.White, surfaceVariant = Color(0xFFEEEBF3),
    textPrimary = Color(0xFF24212C), textSecondary = Color(0xFF625D6C),
    accent = Color(0xFFF3AB8E), onAccent = Color(0xFF542B1D),
    outline = Color(0xFFDDD7E4), controlOutline = Color(0xFF797181),
    success = Color(0xFF236B53), successContainer = Color(0xFFE2F2E9),
    warning = Color(0xFF805200), warningContainer = Color(0xFFFFF0D0),
    error = Color(0xFFB32635), onError = Color.White, errorContainer = Color(0xFFFCE8EB),
    hero = Color(0xFF29213F), isLight = true
)
val GreenPalette = PurplePalette.copy(
    primary = Color(0xFF08745F), primaryContainer = Color(0xFFD9F0E5),
    onPrimaryContainer = Color(0xFF153C30), hero = Color(0xFF173B31)
)
val RedPalette = PurplePalette.copy(
    primary = Color(0xFFA33750), primaryContainer = Color(0xFFF8E1E6),
    onPrimaryContainer = Color(0xFF542031), hero = Color(0xFF482431)
)
val DarkPalette = AppColors(
    primary = Color(0xFFC5B8FF), onPrimary = Color(0xFF281E57),
    primaryContainer = Color(0xFF3B315F), onPrimaryContainer = Color(0xFFEEE7FF),
    background = Color(0xFF111018), surface = Color(0xFF1D1A27), surfaceVariant = Color(0xFF2A2637),
    textPrimary = Color(0xFFF6F2FF), textSecondary = Color(0xFFC5BED1),
    accent = Color(0xFFF3AB8E), onAccent = Color(0xFF542B1D),
    outline = Color(0xFF4B435B), controlOutline = Color(0xFFA49AAF),
    success = Color(0xFF8DD9B8), successContainer = Color(0xFF173A2D),
    warning = Color(0xFFFFD18A), warningContainer = Color(0xFF443016),
    error = Color(0xFFFFB3BE), onError = Color(0xFF4A202A), errorContainer = Color(0xFF4A202A),
    hero = Color(0xFF302741), isLight = false
)
val HighContrastPalette = DarkPalette.copy(
    primary = Color.Yellow, onPrimary = Color.Black,
    primaryContainer = Color(0xFF252500), onPrimaryContainer = Color.Yellow,
    background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF151515),
    textPrimary = Color.White, textSecondary = Color.White,
    accent = Color.Yellow, onAccent = Color.Black, outline = Color.White, controlOutline = Color.White,
    success = Color(0xFF73FFB3), successContainer = Color(0xFF002516),
    warning = Color.Yellow, warningContainer = Color(0xFF252500),
    errorContainer = Color(0xFF32000C), hero = Color.Black, highContrast = true
)
