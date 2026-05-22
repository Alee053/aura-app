package com.programovil.aura.todo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null
)
