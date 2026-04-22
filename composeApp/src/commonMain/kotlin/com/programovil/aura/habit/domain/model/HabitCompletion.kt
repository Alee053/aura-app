package com.programovil.aura.habit.domain.model

data class HabitCompletion(
    val id: String,
    val habitId: String,
    val completedDate: String, // YYYY-MM-DD format
    val completedAt: Long = System.currentTimeMillis()
)
