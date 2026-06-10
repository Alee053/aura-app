package com.programovil.aura.todo.data.mapper

import com.programovil.aura.todo.domain.model.Todo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoMapperTest {

    @Test
    fun `toDomain maps every field from data`() {
        val data = TodoData(
            id = "t-1",
            title = "Buy milk",
            description = "From the corner store",
            isCompleted = false,
            dueDate = 1_700_000_000_000L
        )
        val domain = data.toDomain()
        assertEquals(
            Todo("t-1", "Buy milk", "From the corner store", false, 1_700_000_000_000L),
            domain
        )
    }

    @Test
    fun `toDomain handles null description and due date`() {
        val data = TodoData(
            id = "t-2",
            title = "Read",
            description = null,
            isCompleted = true,
            dueDate = null
        )
        val domain = data.toDomain()
        assertEquals(Todo("t-2", "Read", null, true, null), domain)
    }

    @Test
    fun `toDomain defaults missing fields with constructor defaults`() {
        val data = TodoData(id = "t-3", title = "Walk", isCompleted = false)
        assertNull(data.toDomain().description)
        assertNull(data.toDomain().dueDate)
    }
}
