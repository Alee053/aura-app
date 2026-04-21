package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import java.util.UUID

class AddHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(
        name: String,
        recurrenceType: RecurrenceType,
        daysOfWeek: List<Int>,
        color: String
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            recurrenceType = recurrenceType,
            daysOfWeek = if (recurrenceType == RecurrenceType.WEEKLY) daysOfWeek else emptyList(),
            color = color
        )
        return repository.addHabit(habit)
    }
}
