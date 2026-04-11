package org.example.aura_app.domain.model

data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)