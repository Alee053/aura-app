package com.programovil.aura.home.domain.usecase

import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.usecase.GetHabitsWithStatusUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDashboardDataUseCase(
    private val getTodosUseCase: GetTodosUseCase,
    private val getHabitsWithStatusUseCase: GetHabitsWithStatusUseCase
) {
    operator fun invoke(): Flow<Result<DashboardData>> {
        return combine(getTodosUseCase(), getHabitsWithStatusUseCase()) { todosResult, habitsResult ->
            todosResult.exceptionOrNull()?.let { return@combine Result.failure(it) }
            habitsResult.exceptionOrNull()?.let { return@combine Result.failure(it) }
            val todos = todosResult.getOrNull()
            val habits = habitsResult.getOrNull()
            val incompleteTodos = todos?.count { !it.isCompleted } ?: 0
            val completedHabitsToday = habits?.count { it.last7Days.lastOrNull()?.isCompleted == true } ?: 0
            val totalHabitsToday = habits?.size ?: 0
            val streakValues = habits?.map { it.streak } ?: emptyList()
            val currentStreak = streakValues.maxOrNull() ?: 0

            Result.success(
                DashboardData(
                    incompleteTodos = incompleteTodos,
                    completedHabitsToday = completedHabitsToday,
                    totalHabitsToday = totalHabitsToday,
                    currentStreak = currentStreak
                )
            )
        }
    }
}
