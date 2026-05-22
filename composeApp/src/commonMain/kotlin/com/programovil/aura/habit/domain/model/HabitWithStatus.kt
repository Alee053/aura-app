package com.programovil.aura.habit.domain.model

data class HabitWithStatus(
    val habit: Habit,
    val currentPeriodProgress: Pair<Int, Int>,
    val streak: Int,
    val last7Days: List<DayCompletion>
)
