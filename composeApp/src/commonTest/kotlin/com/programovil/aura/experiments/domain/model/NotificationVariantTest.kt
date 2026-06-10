package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NotificationVariantTest {

    @Test
    fun `two NotificationVariants with the same values are equal`() {
        val a = NotificationVariant(
            timesPerDay = 3,
            tone = Tone.Gentle,
            useDueDateChannel = true
        )
        val b = NotificationVariant(
            timesPerDay = 3,
            tone = Tone.Gentle,
            useDueDateChannel = true
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy changes only the specified field`() {
        val original = NotificationVariant(
            timesPerDay = 1,
            tone = Tone.Gentle,
            useDueDateChannel = false
        )
        val updated = original.copy(timesPerDay = 5)
        assertEquals(5, updated.timesPerDay)
        assertEquals(original.tone, updated.tone)
        assertEquals(original.useDueDateChannel, updated.useDueDateChannel)
    }

    @Test
    fun `differing useDueDateChannel breaks equality`() {
        val a = NotificationVariant(1, Tone.Gentle, false)
        val b = NotificationVariant(1, Tone.Gentle, true)
        assertNotEquals(a, b)
    }
}
