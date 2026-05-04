package com.programovil.aura.habit.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DaySectionTest {

    @Test
    fun `TODAY has correct display name`() {
        assertEquals("Today", DaySection.TODAY.displayName)
    }

    @Test
    fun `TOMORROW has correct display name`() {
        assertEquals("Tomorrow", DaySection.TOMORROW.displayName)
    }

    @Test
    fun `THIS_WEEK has correct display name`() {
        assertEquals("This Week", DaySection.THIS_WEEK.displayName)
    }

    @Test
    fun `enum has exactly three values`() {
        assertEquals(3, DaySection.entries.size)
    }
}