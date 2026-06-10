package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.Mockable

@Mockable
class DeleteHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habitId: String): Result<Unit> {
        return repository.deleteHabit(habitId)
    }
}