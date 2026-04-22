package com.programovil.aura.habit.domain.model

import com.programovil.aura.habit.domain.model.Habit

data class HabitWithStatus(
    val habit: Habit,
    val isDone: Boolean,
    val isMissed: Boolean,
    val streak: Int = 0,
    val targetDate: String = ""  // YYYY-MM-DD format
)
