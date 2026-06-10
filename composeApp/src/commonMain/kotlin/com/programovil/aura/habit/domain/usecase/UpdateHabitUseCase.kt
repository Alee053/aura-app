package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.Mockable

@Mockable
class UpdateHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habit: Habit): Result<Unit> {
        return repository.updateHabit(habit)
    }
}