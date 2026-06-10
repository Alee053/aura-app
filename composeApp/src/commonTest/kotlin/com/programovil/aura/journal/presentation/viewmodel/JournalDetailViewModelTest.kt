package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import com.programovil.aura.journal.domain.usecase.AddJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.UpdateJournalEntryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JournalDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeDetailJournalRepository()
    private val viewModels = mutableListOf<JournalDetailViewModel>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `new-entry mode (null id) does not load any entry on init`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = null)
        runCurrent()

        assertEquals(0, repository.getEntryCalls.size)
        assertNull(viewModel.uiState.value.entry)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals("", viewModel.uiState.value.content)
    }

    @Test
    fun `existing-entry mode loads the entry and seeds title and content`() = runTest(testDispatcher) {
        val entry = JournalEntry("j-1", "Hello", "World", 100L, 200L)
        repository.entriesById["j-1"] = entry

        val viewModel = createViewModel(entryId = "j-1")
        runCurrent()

        assertEquals(entry, viewModel.uiState.value.entry)
        assertEquals("Hello", viewModel.uiState.value.title)
        assertEquals("World", viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `existing-entry mode sets JournalNotFound error when the entry is missing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = "missing")
        runCurrent()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateTitle and updateContent update the state without touching use cases`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = null)
        runCurrent()

        viewModel.updateTitle("New")
        viewModel.updateContent("Body")
        runCurrent()

        assertEquals("New", viewModel.uiState.value.title)
        assertEquals("Body", viewModel.uiState.value.content)
        assertEquals(0, repository.addCalls.size)
        assertEquals(0, repository.updateCalls.size)
    }

    @Test
    fun `saveEntry with blank title is a no-op`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = null)
        runCurrent()
        viewModel.updateTitle("   ")
        viewModel.saveEntry()
        runCurrent()

        assertEquals(0, repository.addCalls.size)
        assertEquals(0, repository.updateCalls.size)
    }

    @Test
    fun `saveEntry in new mode calls addEntry with trimmed values`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = null)
        runCurrent()
        viewModel.updateTitle("  Title  ")
        viewModel.updateContent("  Body  ")
        viewModel.saveEntry()
        runCurrent()

        assertEquals(1, repository.addCalls.size)
        assertEquals("Title" to "Body", repository.addCalls.single())
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveEntry in edit mode calls updateEntry with trimmed values`() = runTest(testDispatcher) {
        val existing = JournalEntry("j-1", "Old", "Old body", 100L, 200L)
        repository.entriesById["j-1"] = existing

        val viewModel = createViewModel(entryId = "j-1")
        runCurrent()
        viewModel.updateTitle("  New  ")
        viewModel.updateContent("  New body  ")
        viewModel.saveEntry()
        runCurrent()

        assertEquals(1, repository.updateCalls.size)
        val updated = repository.updateCalls.single()
        assertEquals("j-1", updated.id)
        assertEquals("New", updated.title)
        assertEquals("New body", updated.content)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveEntry in add mode sets JournalSave error when addEntry fails`() = runTest(testDispatcher) {
        repository.addResult = Result.failure(RuntimeException("db"))

        val viewModel = createViewModel(entryId = null)
        runCurrent()
        viewModel.updateTitle("T")
        viewModel.updateContent("B")
        viewModel.saveEntry()
        runCurrent()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveEntry in edit mode sets JournalSave error when updateEntry fails`() = runTest(testDispatcher) {
        val existing = JournalEntry("j-1", "T", "B", 100L, 200L)
        repository.entriesById["j-1"] = existing
        repository.updateResult = Result.failure(RuntimeException("db"))

        val viewModel = createViewModel(entryId = "j-1")
        runCurrent()
        viewModel.saveEntry()
        runCurrent()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearError resets the error to null`() = runTest(testDispatcher) {
        val viewModel = createViewModel(entryId = "missing")
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()
        runCurrent()

        assertNull(viewModel.uiState.value.error)
    }

    private fun createViewModel(entryId: String?): JournalDetailViewModel {
        val getUseCase = GetJournalEntryUseCase(repository)
        val addUseCase = AddJournalEntryUseCase(repository)
        val updateUseCase = UpdateJournalEntryUseCase(repository)
        val viewModel = JournalDetailViewModel(
            entryId = entryId,
            getEntryUseCase = getUseCase,
            addEntryUseCase = addUseCase,
            updateEntryUseCase = updateUseCase
        )
        viewModels += viewModel
        return viewModel
    }
}

private class FakeDetailJournalRepository : JournalRepository {
    val entriesById: MutableMap<String, JournalEntry> = mutableMapOf()
    val getEntryCalls: MutableList<String> = mutableListOf()
    val addCalls: MutableList<Pair<String, String>> = mutableListOf()
    val updateCalls: MutableList<JournalEntry> = mutableListOf()
    var addResult: Result<Unit> = Result.success(Unit)
    var updateResult: Result<Unit> = Result.success(Unit)

    override fun getEntries() = kotlinx.coroutines.flow.flowOf(Result.success(entriesById.values.toList()))

    override suspend fun getEntry(id: String): JournalEntry? {
        getEntryCalls += id
        return entriesById[id]
    }

    override suspend fun addEntry(title: String, content: String): Result<Unit> {
        addCalls += title to content
        return addResult
    }

    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> {
        updateCalls += entry
        return updateResult
    }

    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
}
