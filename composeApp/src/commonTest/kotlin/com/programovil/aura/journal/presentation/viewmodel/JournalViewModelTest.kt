package com.programovil.aura.journal.presentation.viewmodel

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import com.programovil.aura.journal.domain.usecase.DeleteJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntriesUseCase
import com.programovil.aura.shared.presentation.ErrorKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadEntries updates state when repository emits success`() = runTest(testDispatcher) {
        val entries = listOf(
            JournalEntry(
                id = "entry-1",
                title = "Title",
                content = "Content",
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val viewModel = createViewModel(flowOf(Result.success(entries)))

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(entries, viewModel.uiState.value.entries)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `loadEntries surfaces repository failure as JournalLoad error key`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            flowOf(Result.failure(IllegalStateException("User not authenticated")))
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList(), viewModel.uiState.value.entries)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(ErrorKey.JournalLoad, viewModel.uiState.value.error)
    }

    private fun createViewModel(
        entriesFlow: Flow<Result<List<JournalEntry>>>
    ): JournalViewModel {
        val repository = FakeJournalRepository(entriesFlow)
        return JournalViewModel(
            getEntriesUseCase = GetJournalEntriesUseCase(repository),
            deleteEntryUseCase = DeleteJournalEntryUseCase(repository)
        )
    }
}

private class FakeJournalRepository(
    private val entriesFlow: Flow<Result<List<JournalEntry>>>
) : JournalRepository {
    override fun getEntries(): Flow<Result<List<JournalEntry>>> = entriesFlow

    override suspend fun getEntry(id: String): JournalEntry? = null

    override suspend fun addEntry(title: String, content: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)

    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
}
