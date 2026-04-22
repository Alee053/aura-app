package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetHabitHistoryUseCase(private val repository: HabitRepository) {
    operator fun invoke(habitId: String): Flow<List<HabitCompletion>> {
        return repository.getCompletionsForHabit(habitId)
    }
}
