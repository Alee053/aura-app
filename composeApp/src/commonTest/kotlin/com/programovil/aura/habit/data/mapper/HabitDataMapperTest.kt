package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitDataMapperTest {

    @Test
    fun `HabitData toDomain parses RecurrenceType and defaults createdAt to zero`() {
        val data = HabitData(
            id = "h-1",
            name = "Run",
            recurrenceType = "DAILY",
            targetCount = 1,
            color = "#FF0000",
            createdAt = null
        )
        val habit = data.toDomain()
        assertEquals(
            Habit("h-1", "Run", RecurrenceType.DAILY, 1, "#FF0000", 0L),
            habit
        )
    }

    @Test
    fun `HabitData toDomain preserves createdAt when present`() {
        val data = HabitData(
            id = "h-1",
            name = "Run",
            recurrenceType = "WEEKLY",
            targetCount = 3,
            color = "#00FF00",
            createdAt = 1_700_000_000_000L
        )
        val habit = data.toDomain()
        assertEquals(1_700_000_000_000L, habit.createdAt)
    }

    @Test
    fun `Habit toData serialises RecurrenceType via name`() {
        val habit = Habit("h-2", "Read", RecurrenceType.MONTHLY, 2, "#0000FF", 100L)
        val data = habit.toData()
        assertEquals("MONTHLY", data.recurrenceType)
        assertEquals(habit.id, data.id)
        assertEquals(habit.name, data.name)
        assertEquals(habit.targetCount, data.targetCount)
        assertEquals(habit.color, data.color)
        assertEquals(habit.createdAt, data.createdAt)
    }

    @Test
    fun `HabitCompletionData toDomain defaults completedAt to zero when null`() {
        val data = HabitCompletionData(
            id = "c-1",
            habitId = "h-1",
            completedDate = "2026-01-15",
            completedAt = null
        )
        val completion = data.toDomain()
        assertEquals(HabitCompletion("c-1", "h-1", "2026-01-15", 0L), completion)
    }

    @Test
    fun `HabitCompletion toData preserves completedAt`() {
        val completion = HabitCompletion("c-2", "h-2", "2026-02-01", 5_000L)
        val data = completion.toData()
        assertEquals(completion.id, data.id)
        assertEquals(completion.habitId, data.habitId)
        assertEquals(completion.completedDate, data.completedDate)
        assertEquals(completion.completedAt, data.completedAt)
    }
}
