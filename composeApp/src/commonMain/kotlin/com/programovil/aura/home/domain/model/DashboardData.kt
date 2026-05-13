package com.programovil.aura.home.domain.model

data class DashboardData(
    val incompleteTodos: Int = 0,
    val completedHabitsToday: Int = 0,
    val totalHabitsToday: Int = 0,
    val currentStreak: Int = 0
)
