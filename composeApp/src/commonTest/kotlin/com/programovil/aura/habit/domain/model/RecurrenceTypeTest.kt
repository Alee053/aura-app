package com.programovil.aura.habit.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RecurrenceTypeTest {

    @Test
    fun `DAILY has correct name`() {
        assertEquals("DAILY", RecurrenceType.DAILY.name)
    }

    @Test
    fun `WEEKLY has correct name`() {
        assertEquals("WEEKLY", RecurrenceType.WEEKLY.name)
    }

    @Test
    fun `MONTHLY has correct name`() {
        assertEquals("MONTHLY", RecurrenceType.MONTHLY.name)
    }

    @Test
    fun `enum has exactly three values`() {
        assertEquals(3, RecurrenceType.entries.size)
    }
}