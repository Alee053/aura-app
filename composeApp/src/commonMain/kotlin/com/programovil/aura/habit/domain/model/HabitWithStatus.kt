package com.programovil.aura.habit.domain.model

data class HabitWithStatus(
    val habit: Habit,
    val isDone: Boolean,
    val isMissed: Boolean,
    val streak: Int = 0
)
