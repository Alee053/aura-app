package com.programovil.aura.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorUtilsTest {

    @Test
    fun `parseHexColor parses 6 digit hex with hash`() {
        val color = parseHexColor("#FF6B6B")
        assertEquals(0xFFFF6B6B.toInt(), color.toArgb())
    }

    @Test
    fun `parseHexColor parses 6 digit hex without hash`() {
        val color = parseHexColor("00FF00")
        assertEquals(0xFF00FF00.toInt(), color.toArgb())
    }

    @Test
    fun `parseHexColor parses 8 digit hex with alpha`() {
        val color = parseHexColor("80FF6B6B")
        assertEquals(0x80FF6B6B.toInt(), color.toArgb())
    }

    @Test
    fun `parseHexColor returns gray for invalid length`() {
        val color = parseHexColor("BAD")
        assertEquals(Color.Gray.toArgb(), color.toArgb())
    }
}
