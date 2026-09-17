package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import aura_app.designsystem.generated.resources.*
import org.jetbrains.compose.resources.Font

@Immutable
data class AppTypography(
    val displayLarge: TextStyle, val headlineLarge: TextStyle, val headlineSmall: TextStyle,
    val titleMedium: TextStyle, val bodyLarge: TextStyle, val bodyMedium: TextStyle,
    val labelLarge: TextStyle, val labelMedium: TextStyle, val labelSmall: TextStyle,
    val timer: TextStyle, val journal: TextStyle
)
fun auraTypography(family: FontFamily = FontFamily.SansSerif): AppTypography {
    fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    return AppTypography(
        displayLarge = style(40, 48, FontWeight.SemiBold).copy(letterSpacing = (-0.4).sp),
        headlineLarge = style(30, 38, FontWeight.Bold).copy(letterSpacing = (-0.4).sp),
        headlineSmall = style(22, 30, FontWeight.SemiBold),
        titleMedium = style(18, 26, FontWeight.SemiBold),
        bodyLarge = style(16, 24), bodyMedium = style(14, 20),
        labelLarge = style(14, 20, FontWeight.SemiBold),
        labelMedium = style(12, 18, FontWeight.Medium),
        labelSmall = style(12, 18, FontWeight.Medium),
        timer = style(64, 72, FontWeight.Medium).copy(fontFeatureSettings = "tnum"),
        journal = style(17, 28)
    )
}
val DefaultTypography = auraTypography()

@Composable
internal fun rememberAuraTypography() = auraTypography(
    FontFamily(
        Font(Res.font.manrope_regular, FontWeight.Normal),
        Font(Res.font.manrope_medium, FontWeight.Medium),
        Font(Res.font.manrope_semibold, FontWeight.SemiBold),
        Font(Res.font.manrope_bold, FontWeight.Bold)
    )
)
