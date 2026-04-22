package com.programovil.aura.todo.data.mapper

import com.programovil.aura.todo.domain.model.Todo

data class TodoData(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val dueDate: Long? = null
)

fun TodoData.toDomain(): Todo = Todo(
    id = id,
    title = title,
    isCompleted = isCompleted,
    dueDate = dueDate
)