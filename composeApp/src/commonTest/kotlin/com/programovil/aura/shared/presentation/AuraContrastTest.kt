package com.programovil.aura.shared.presentation

import androidx.compose.ui.graphics.Color
import com.programovil.aura.designsystem.theme.*
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class AuraContrastTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double = if (value <= .04045f) value / 12.92 else ((value + .055) / 1.055).pow(2.4)
        return .2126 * channel(color.red) + .7152 * channel(color.green) + .0722 * channel(color.blue)
    }
    private fun ratio(a: Color, b: Color): Double {
        val values = listOf(luminance(a), luminance(b)).sorted()
        return (values[1] + .05) / (values[0] + .05)
    }
    @Test fun textPairsMeetNormalTextContrastAcrossAllThemes() {
        ThemeMode.entries.forEach { mode ->
            val c = paletteFor(mode)
            listOf(c.textPrimary to c.background, c.textSecondary to c.background,
                c.textPrimary to c.surface, c.textSecondary to c.surfaceVariant,
                c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                c.onHero to c.hero, c.error to c.errorContainer, c.success to c.successContainer,
                c.warning to c.warningContainer).forEach { (foreground, background) ->
                assertTrue(ratio(foreground, background) >= 4.5, "$mode contrast: " + ratio(foreground, background))
            }
            assertTrue(ratio(c.controlOutline, c.surface) >= 3.0, "$mode control boundary")
        }
    }
}
