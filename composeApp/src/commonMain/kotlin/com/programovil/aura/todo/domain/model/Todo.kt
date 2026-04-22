package com.programovil.aura.todo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null  // epoch millis, nullable
)
