package com.programovil.aura.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode {
    PURPLE, RED, GREEN, DARK, HIGH_CONTRAST
}

val LocalColors = staticCompositionLocalOf { PurplePalette }
internal val LocalTypography = staticCompositionLocalOf { DefaultTypography }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalColors.current
    val typography: AppTypography
        @Composable
        get() = LocalTypography.current
}

@Composable
fun DsTheme(
    mode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.PURPLE,
    content: @Composable () -> Unit
) {
    val colors = when (mode) {
        ThemeMode.PURPLE -> PurplePalette
        ThemeMode.RED -> RedPalette
        ThemeMode.GREEN -> GreenPalette
        ThemeMode.DARK -> DarkPalette
        ThemeMode.HIGH_CONTRAST -> HighContrastPalette
    }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides DefaultTypography
    ) {
        content()
    }
}
