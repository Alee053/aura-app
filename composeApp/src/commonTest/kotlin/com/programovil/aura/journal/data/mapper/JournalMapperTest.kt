package com.programovil.aura.journal.data.mapper

import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.domain.model.JournalEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalMapperTest {

    @Test
    fun `toDomain maps every field from entity`() {
        val entity = JournalEntity(
            id = "j-1",
            title = "Title",
            content = "Body",
            createdAt = 1_000L,
            updatedAt = 2_000L
        )
        val domain = entity.toDomain()
        assertEquals(JournalEntry("j-1", "Title", "Body", 1_000L, 2_000L), domain)
    }

    @Test
    fun `toEntity maps every field from domain`() {
        val domain = JournalEntry("j-2", "Hello", "World", 100L, 200L)
        val entity = domain.toEntity()
        assertEquals(JournalEntity("j-2", "Hello", "World", 100L, 200L), entity)
    }
}
