package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.repository.JournalRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddJournalEntryUseCaseTest {

    private val repository = mock(of<JournalRepository>())
    private val useCase = AddJournalEntryUseCase(repository)

    @Test
    fun `invoke forwards title and content to repository`() = runTest {
        coEvery { repository.addEntry("Title", "Body") } returns Result.success(Unit)

        val result = useCase("Title", "Body")

        assertTrue(result.isSuccess)
        coVerify { repository.addEntry("Title", "Body") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("db error")
        coEvery { repository.addEntry("Title", "Body") } returns Result.failure(error)

        val result = useCase("Title", "Body")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
