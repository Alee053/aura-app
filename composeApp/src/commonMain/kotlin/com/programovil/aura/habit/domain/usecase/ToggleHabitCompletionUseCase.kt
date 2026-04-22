package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.repository.HabitRepository

class ToggleHabitCompletionUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habitId: String, date: String): Result<Unit> {
        if (habitId.isBlank()) return Result.failure(IllegalArgumentException("Habit ID cannot be empty"))
        if (date.isBlank()) return Result.failure(IllegalArgumentException("Date cannot be empty"))
        return repository.toggleCompletion(habitId, date)
    }
}
