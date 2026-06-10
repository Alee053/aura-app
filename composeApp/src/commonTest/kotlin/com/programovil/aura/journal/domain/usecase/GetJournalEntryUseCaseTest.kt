package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetJournalEntryUseCaseTest {

    private val repository = mock(of<JournalRepository>())
    private val useCase = GetJournalEntryUseCase(repository)

    @Test
    fun `invoke returns entry from repository when found`() = runTest {
        val entry = JournalEntry("j-1", "A", "a", 1L, 1L)
        coEvery { repository.getEntry("j-1") } returns entry

        val result = useCase("j-1")

        assertEquals(entry, result)
        coVerify { repository.getEntry("j-1") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke returns null when repository has no entry`() = runTest {
        coEvery { repository.getEntry("missing") } returns null

        val result = useCase("missing")

        assertNull(result)
    }
}
