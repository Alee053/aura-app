package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("completedDate")]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val completedDate: String, // YYYY-MM-DD
    val completedAt: Long
)