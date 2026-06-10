package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HomeVariantTest {

    @Test
    fun `two HomeVariants with the same values are equal`() {
        val a = HomeVariant(
            showsDailyMotivation = true,
            tone = Tone.Gentle,
            motivationPhrase = mapOf("en" to "Hello", "es" to "Hola")
        )
        val b = HomeVariant(
            showsDailyMotivation = true,
            tone = Tone.Gentle,
            motivationPhrase = mapOf("en" to "Hello", "es" to "Hola")
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy changes only the specified field`() {
        val original = HomeVariant(
            showsDailyMotivation = false,
            tone = Tone.Gentle,
            motivationPhrase = mapOf("en" to "Hi")
        )
        val updated = original.copy(showsDailyMotivation = true)
        assertEquals(true, updated.showsDailyMotivation)
        assertEquals(original.tone, updated.tone)
        assertEquals(original.motivationPhrase, updated.motivationPhrase)
    }

    @Test
    fun `differing tone breaks equality`() {
        val a = HomeVariant(true, Tone.Gentle, emptyMap())
        val b = HomeVariant(true, Tone.Direct, emptyMap())
        assertNotEquals(a, b)
    }
}
