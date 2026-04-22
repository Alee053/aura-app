package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.datetime.Clock

class AddHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(
        name: String,
        recurrenceType: RecurrenceType,
        daysOfWeek: List<Int>,
        color: String
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val habit = Habit(
            id = generateUUID(),
            name = name.trim(),
            recurrenceType = recurrenceType,
            daysOfWeek = if (recurrenceType == RecurrenceType.WEEKLY) daysOfWeek else emptyList(),
            color = color,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        return repository.addHabit(habit)
    }

    private fun generateUUID(): String {
        return "habit-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
    }
}
