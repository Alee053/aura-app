package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    indices = [Index("habitId"), Index("completedDate")]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val completedDate: String,
    val completedAt: Long
)
