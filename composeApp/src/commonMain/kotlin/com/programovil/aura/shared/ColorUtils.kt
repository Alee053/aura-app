package com.programovil.aura.shared

import androidx.compose.ui.graphics.Color

/**
 * Parses a hex color string (e.g., "#FF6B6B" or "FF6B6B") into a Compose Color.
 */
fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return when (cleanHex.length) {
        6 -> {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(red = r, green = g, blue = b, alpha = 255)
        }
        8 -> {
            val a = cleanHex.substring(0, 2).toInt(16)
            val r = cleanHex.substring(2, 4).toInt(16)
            val g = cleanHex.substring(4, 6).toInt(16)
            val b = cleanHex.substring(6, 8).toInt(16)
            Color(red = r, green = g, blue = b, alpha = a)
        }
        else -> Color.Gray // Fallback
    }
}
