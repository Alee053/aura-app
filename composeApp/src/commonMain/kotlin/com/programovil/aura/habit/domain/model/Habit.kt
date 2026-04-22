package com.programovil.aura.habit.domain.model

data class Habit(
    val id: String,
    val name: String,
    val recurrenceType: RecurrenceType,
    val daysOfWeek: List<Int> = emptyList(), // 1=Monday, 7=Sunday. Empty for DAILY.
    val color: String, // hex color code
    val createdAt: Long = System.currentTimeMillis(),
    val isSyncPending: Boolean = true
)
