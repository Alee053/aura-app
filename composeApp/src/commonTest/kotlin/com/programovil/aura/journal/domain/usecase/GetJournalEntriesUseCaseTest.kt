package com.programovil.aura.journal.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetJournalEntriesUseCaseTest {

    private val repository = mock(of<JournalRepository>())
    private val useCase = GetJournalEntriesUseCase(repository)

    @Test
    fun `invoke forwards the repository flow unchanged on success`() = runTest {
        val entries = listOf(
            JournalEntry("j-1", "A", "a", 1L, 1L),
            JournalEntry("j-2", "B", "b", 2L, 2L)
        )
        every { repository.getEntries() } returns flowOf(Result.success(entries))

        useCase().test {
            val result = awaitItem()
            assertEquals(Result.success(entries), result)
            awaitComplete()
        }
    }

    @Test
    fun `invoke forwards the repository flow unchanged on failure`() = runTest {
        val error = RuntimeException("db error")
        every { repository.getEntries() } returns flowOf(Result.failure(error))

        useCase().test {
            val result = awaitItem()
            assertEquals(Result.failure<Any>(error).exceptionOrNull(), result.exceptionOrNull())
            awaitComplete()
        }
    }
}
