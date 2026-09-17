package com.programovil.aura.shared.presentation

import androidx.compose.runtime.saveable.listSaver
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType

val TodoEditorSaver = listSaver<Todo?, Any>(
    save = { item -> if (item == null) emptyList() else listOf(item.id, item.title,
        item.description ?: "", item.isCompleted, item.dueDate ?: Long.MIN_VALUE) },
    restore = { fields -> if (fields.isEmpty()) null else Todo(fields[0] as String, fields[1] as String,
        (fields[2] as String).takeIf { it.isNotEmpty() }, fields[3] as Boolean,
        (fields[4] as Long).takeIf { it != Long.MIN_VALUE }) }
)
val HabitEditorSaver = listSaver<Habit?, Any>(
    save = { item -> if (item == null) emptyList() else listOf(item.id, item.name, item.recurrenceType.name,
        item.targetCount, item.color, item.createdAt) },
    restore = { fields -> if (fields.isEmpty()) null else Habit(fields[0] as String, fields[1] as String,
        RecurrenceType.valueOf(fields[2] as String), fields[3] as Int, fields[4] as String, fields[5] as Long) }
)
