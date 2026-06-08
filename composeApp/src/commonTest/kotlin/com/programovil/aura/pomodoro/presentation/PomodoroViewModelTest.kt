package com.programovil.aura.pomodoro.presentation

import app.cash.turbine.test
import com.programovil.aura.pomodoro.domain.PomodoroDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroViewModelTest {

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
    fun `initial state is a 25-minute idle pomodoro`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.timeLeftSeconds)
            assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.initialTimeSeconds)
            assertEquals(PomodoroMode.POMODORO, state.mode)
            assertFalse(state.isRunning)
            assertEquals(0, state.sessionsCompleted)
            assertEquals(1f, state.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTimeOptionSelected resets timer and stops running state`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()
        viewModel.toggleTimer()
        assertTrue(viewModel.uiState.value.isRunning)

        viewModel.onTimeOptionSelected(10)

        val state = viewModel.uiState.value
        assertEquals(10 * 60, state.timeLeftSeconds)
        assertEquals(10 * 60, state.initialTimeSeconds)
        assertEquals(10, state.selectedOption)
        assertFalse(state.isRunning)
    }

    @Test
    fun `toggleTimer starts and stops the countdown`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()

        viewModel.toggleTimer()
        assertTrue(viewModel.uiState.value.isRunning)

        testDispatcher.scheduler.advanceTimeBy(PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()
        assertEquals(
            PomodoroDefaults.POMODORO_MINUTES * 60 - 1,
            viewModel.uiState.value.timeLeftSeconds
        )

        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test
    fun `resetTimer restores initial duration and stops running`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()
        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceTimeBy(2 * PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()

        viewModel.resetTimer()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.timeLeftSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `completing a pomodoro transitions to a 5-minute short break`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()
        viewModel.onTimeOptionSelected(1)

        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceTimeBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(1, state.sessionsCompleted)
        assertEquals(PomodoroDefaults.SHORT_BREAK_MINUTES * 60, state.timeLeftSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `every fourth completed pomodoro transitions to a 15-minute long break`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()

        // 4 pomodoros + 3 intervening short breaks = 7 transitions to reach the long break.
        repeat(7) {
            viewModel.onTimeOptionSelected(1)
            viewModel.toggleTimer()
            testDispatcher.scheduler.advanceTimeBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.LONG_BREAK, state.mode)
        assertEquals(
            PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK,
            state.sessionsCompleted
        )
        assertEquals(PomodoroDefaults.LONG_BREAK_MINUTES * 60, state.timeLeftSeconds)
    }

    @Test
    fun `finishing a break transitions back to a pomodoro`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()
        viewModel.onTimeOptionSelected(1)
        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceTimeBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.mode)

        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceTimeBy(5 * 60 * PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.POMODORO, state.mode)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, state.timeLeftSeconds)
    }

    @Test
    fun `skipSession counts as a completed session and advances mode`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()

        viewModel.skipSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sessionsCompleted)
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertFalse(state.isRunning)
    }

    @Test
    fun `progress reflects ratio of remaining to initial time`() = runTest(testDispatcher) {
        val viewModel = PomodoroViewModel()
        viewModel.onTimeOptionSelected(2)
        viewModel.toggleTimer()
        testDispatcher.scheduler.advanceTimeBy(60 * PomodoroDefaults.TICK_INTERVAL_MS)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(60, state.timeLeftSeconds)
        assertEquals(120, state.initialTimeSeconds)
        assertEquals(0.5f, state.progress)
    }
}
