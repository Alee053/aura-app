package com.programovil.aura.pomodoro.presentation

import com.programovil.aura.pomodoro.domain.PomodoroDefaults
import com.programovil.aura.pomodoro.domain.PomodoroMode
import com.programovil.aura.pomodoro.domain.PomodoroStateRepository
import com.programovil.aura.pomodoro.domain.PomodoroTimerState
import com.programovil.aura.pomodoro.domain.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePomodoroStateRepository
    private lateinit var timeProvider: FakeTimeProvider

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePomodoroStateRepository()
        timeProvider = FakeTimeProvider()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is a 25-minute idle pomodoro`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, viewModel.uiState.value.timeLeftSeconds)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, viewModel.uiState.value.initialTimeSeconds)
        assertEquals(PomodoroMode.POMODORO, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(0, viewModel.uiState.value.sessionsCompleted)
        assertEquals(1f, viewModel.uiState.value.progress)
    }

    @Test
    fun `onTimeOptionSelected resets timer and stops running state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.toggleTimer()
        runCurrent()

        viewModel.onTimeOptionSelected(10)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(10 * 60, state.timeLeftSeconds)
        assertEquals(10 * 60, state.initialTimeSeconds)
        assertEquals(10, state.selectedOption)
        assertFalse(state.isRunning)
    }

    @Test
    fun `toggleTimer starts and stops the countdown`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.toggleTimer()
        runCurrent()
        assertTrue(viewModel.uiState.value.isRunning)

        advanceClockBy(PomodoroDefaults.TICK_INTERVAL_MS)
        assertEquals(
            PomodoroDefaults.POMODORO_MINUTES * 60 - 1,
            viewModel.uiState.value.timeLeftSeconds
        )

        viewModel.toggleTimer()
        runCurrent()
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test
    fun `resetTimer restores initial duration and stops running`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.toggleTimer()
        runCurrent()
        advanceClockBy(2 * PomodoroDefaults.TICK_INTERVAL_MS)

        viewModel.resetTimer()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.timeLeftSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `completing a pomodoro transitions to a 5-minute short break and shows completion message`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTimeOptionSelected(1)
        runCurrent()

        viewModel.toggleTimer()
        runCurrent()
        advanceClockBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(1, state.sessionsCompleted)
        assertEquals(PomodoroDefaults.SHORT_BREAK_MINUTES * 60, state.timeLeftSeconds)
        assertFalse(state.isRunning)
        assertTrue(state.showCompletionMessage)
        assertEquals(PomodoroMode.POMODORO, state.completedMode)
    }

    @Test
    fun `every fourth completed pomodoro transitions to a 15-minute long break`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        repeat(7) {
            viewModel.onTimeOptionSelected(1)
            runCurrent()
            viewModel.toggleTimer()
            runCurrent()
            advanceClockBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)
            viewModel.dismissCompletionMessage()
            runCurrent()
        }

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.LONG_BREAK, state.mode)
        assertEquals(PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK, state.sessionsCompleted)
        assertEquals(PomodoroDefaults.LONG_BREAK_MINUTES * 60, state.timeLeftSeconds)
    }

    @Test
    fun `finishing a break transitions back to a pomodoro`() = runTest(testDispatcher) {
        repository.saveImmediate(
            PomodoroTimerState(
                timeLeftSeconds = 2,
                initialTimeSeconds = 2,
                isRunning = false,
                mode = PomodoroMode.SHORT_BREAK,
                sessionsCompleted = 1,
                selectedOption = PomodoroDefaults.SHORT_BREAK_MINUTES
            )
        )
        val viewModel = createViewModel()

        viewModel.toggleTimer()
        runCurrent()
        advanceClockBy(2 * PomodoroDefaults.TICK_INTERVAL_MS)

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.POMODORO, state.mode)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.timeLeftSeconds)
        assertTrue(state.showCompletionMessage)
        assertEquals(PomodoroMode.SHORT_BREAK, state.completedMode)
    }

    @Test
    fun `skipSession counts as a completed session and advances mode`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.skipSession()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, state.sessionsCompleted)
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertFalse(state.isRunning)
    }

    @Test
    fun `progress reflects ratio of remaining to initial time`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTimeOptionSelected(2)
        runCurrent()
        viewModel.toggleTimer()
        runCurrent()

        advanceClockBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)

        val state = viewModel.uiState.value
        assertEquals(60, state.timeLeftSeconds)
        assertEquals(120, state.initialTimeSeconds)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun `restores active timer after viewmodel recreation`() = runTest(testDispatcher) {
        val firstViewModel = createViewModel()
        firstViewModel.onTimeOptionSelected(1)
        runCurrent()
        firstViewModel.toggleTimer()
        runCurrent()
        advanceClockBy(15 * PomodoroDefaults.TICK_INTERVAL_MS)

        val secondViewModel = createViewModel()

        assertTrue(secondViewModel.uiState.value.isRunning)
        assertEquals(45, secondViewModel.uiState.value.timeLeftSeconds)
        advanceClockBy(5 * PomodoroDefaults.TICK_INTERVAL_MS)
        assertEquals(40, secondViewModel.uiState.value.timeLeftSeconds)
    }

    @Test
    fun `restores finished timer after app reopen and shows completion message`() = runTest(testDispatcher) {
        repository.saveImmediate(
            PomodoroTimerState(
                timeLeftSeconds = 1,
                initialTimeSeconds = 1,
                isRunning = true,
                mode = PomodoroMode.POMODORO,
                sessionsCompleted = 0,
                selectedOption = 1,
                endsAtEpochMillis = timeProvider.currentTimeMillis() - 500L
            )
        )

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(1, state.sessionsCompleted)
        assertFalse(state.isRunning)
        assertTrue(state.showCompletionMessage)
        assertEquals(PomodoroMode.POMODORO, state.completedMode)
    }

    @Test
    fun `dismissCompletionMessage clears persisted completion state`() = runTest(testDispatcher) {
        repository.saveImmediate(
            PomodoroTimerState(
                showCompletionMessage = true,
                completedMode = PomodoroMode.POMODORO
            )
        )
        val viewModel = createViewModel()

        viewModel.dismissCompletionMessage()
        runCurrent()

        assertFalse(viewModel.uiState.value.showCompletionMessage)
        assertNull(viewModel.uiState.value.completedMode)
    }

    private fun createViewModel(): PomodoroViewModel {
        val viewModel = PomodoroViewModel(repository, timeProvider)
        testDispatcher.scheduler.runCurrent()
        return viewModel
    }

    private fun advanceClockBy(milliseconds: Long) {
        timeProvider.advanceBy(milliseconds)
        testDispatcher.scheduler.advanceTimeBy(milliseconds)
        testDispatcher.scheduler.runCurrent()
    }
}

private class FakePomodoroStateRepository(
    initialState: PomodoroTimerState = PomodoroTimerState()
) : PomodoroStateRepository {
    private val stateFlow = MutableStateFlow(initialState)

    override val state: Flow<PomodoroTimerState> = stateFlow

    override suspend fun saveState(state: PomodoroTimerState) {
        stateFlow.value = state
    }

    fun saveImmediate(state: PomodoroTimerState) {
        stateFlow.update { state }
    }
}

private class FakeTimeProvider(
    private var nowMillis: Long = 0L
) : TimeProvider {
    override fun currentTimeMillis(): Long = nowMillis

    fun advanceBy(milliseconds: Long) {
        nowMillis += milliseconds
    }
}
