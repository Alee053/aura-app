package com.programovil.aura.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*

enum class ThemeMode { PURPLE, RED, GREEN, DARK, HIGH_CONTRAST }
val LocalColors = staticCompositionLocalOf { PurplePalette }
internal val LocalTypography = staticCompositionLocalOf { DefaultTypography }
object AppTheme {
    val colors: AppColors @Composable get() = LocalColors.current
    val typography: AppTypography @Composable get() = LocalTypography.current
    val spacing get() = AuraSpacing
    val shapes get() = AuraShapes
}
fun paletteFor(mode: ThemeMode) = when (mode) {
    ThemeMode.PURPLE -> PurplePalette
    ThemeMode.RED -> RedPalette
    ThemeMode.GREEN -> GreenPalette
    ThemeMode.DARK -> DarkPalette
    ThemeMode.HIGH_CONTRAST -> HighContrastPalette
}
@Composable
fun DsTheme(
    mode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.PURPLE,
    content: @Composable () -> Unit
) {
    val c = paletteFor(mode)
    val t = rememberAuraTypography()
    val base = if (c.isLight) lightColorScheme() else darkColorScheme()
    val scheme = base.copy(
        primary = c.primary, onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.accent, onSecondary = c.onAccent,
        secondaryContainer = c.primaryContainer, onSecondaryContainer = c.onPrimaryContainer,
        tertiary = c.success, onTertiary = c.surface,
        tertiaryContainer = c.successContainer, onTertiaryContainer = c.success,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.textSecondary,
        surfaceTint = c.primary, surfaceBright = c.surface, surfaceDim = c.background,
        surfaceContainerLowest = c.background, surfaceContainerLow = c.surface,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceVariant,
        surfaceContainerHighest = c.surfaceVariant,
        outline = c.controlOutline, outlineVariant = c.controlOutline,
        error = c.error, onError = c.onError,
        errorContainer = c.errorContainer, onErrorContainer = c.error,
        inverseSurface = c.hero, inverseOnSurface = c.onHero, inversePrimary = c.accent,
        scrim = androidx.compose.ui.graphics.Color.Black
    )
    val typography = Typography(
        displayLarge = t.displayLarge, displayMedium = t.displayLarge, displaySmall = t.headlineLarge,
        headlineLarge = t.headlineLarge, headlineMedium = t.headlineSmall, headlineSmall = t.headlineSmall,
        titleLarge = t.headlineSmall, titleMedium = t.titleMedium, titleSmall = t.labelLarge,
        bodyLarge = t.bodyLarge, bodyMedium = t.bodyMedium, bodySmall = t.labelMedium,
        labelLarge = t.labelLarge, labelMedium = t.labelMedium, labelSmall = t.labelSmall
    )
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalColors provides c, LocalTypography provides t, content = content)
    }
}
