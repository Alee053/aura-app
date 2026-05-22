package com.programovil.aura.home.domain.usecase

import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDashboardDataUseCase(
    private val getTodosUseCase: GetTodosUseCase,
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase
) {
    operator fun invoke(): Flow<Result<DashboardData>> {
        return combine(getTodosUseCase(), getHabitsGroupedByDayUseCase()) { todosResult, habitsResult ->
            val todos = todosResult.getOrNull()
            val habits = habitsResult.getOrNull()
            val incompleteTodos = todos?.count { !it.isCompleted } ?: 0
            val todayHabits = habits?.get(DaySection.TODAY) ?: emptyList()
            val completedHabitsToday = todayHabits.count { it.isDone }
            val streakValues = todayHabits.map { h -> h.streak }
            val currentStreak = if (streakValues.isEmpty()) 0 else streakValues.maxOrNull() ?: 0

            Result.success(
                DashboardData(
                    incompleteTodos = incompleteTodos,
                    completedHabitsToday = completedHabitsToday,
                    totalHabitsToday = todayHabits.size,
                    currentStreak = currentStreak
                )
            )
        }
    }
}
