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
import kotlin.test.assertTrue

class UpdateJournalEntryUseCaseTest {

    private val repository = mock(of<JournalRepository>())
    private val useCase = UpdateJournalEntryUseCase(repository)

    @Test
    fun `invoke forwards entry to repository`() = runTest {
        val entry = JournalEntry("j-1", "Title", "Body", 100L, 200L)
        coEvery { repository.updateEntry(entry) } returns Result.success(Unit)

        val result = useCase(entry)

        assertTrue(result.isSuccess)
        coVerify { repository.updateEntry(entry) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val entry = JournalEntry("j-1", "Title", "Body", 100L, 200L)
        val error = RuntimeException("not found")
        coEvery { repository.updateEntry(entry) } returns Result.failure(error)

        val result = useCase(entry)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
