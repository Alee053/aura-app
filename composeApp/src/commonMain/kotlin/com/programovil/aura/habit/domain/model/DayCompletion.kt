package com.programovil.aura.habit.domain.model

data class DayCompletion(
    val date: String,
    val isCompleted: Boolean,
    val isScheduled: Boolean
)