package com.programovil.aura.pomodoro.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PomodoroCompletionHandlerTest {

    @Test
    fun `computeRemainingSeconds returns timeLeft when not running`() {
        val state = PomodoroTimerState(timeLeftSeconds = 120, initialTimeSeconds = 300, isRunning = false)
        val remaining = PomodoroCompletionHandler.computeRemainingSeconds(state, currentTimeMillis = 1_000L)
        assertEquals(120, remaining)
    }

    @Test
    fun `computeRemainingSeconds returns zero when not running and timeLeft is negative`() {
        val state = PomodoroTimerState(timeLeftSeconds = -1, initialTimeSeconds = 300, isRunning = false)
        val remaining = PomodoroCompletionHandler.computeRemainingSeconds(state, currentTimeMillis = 1_000L)
        assertEquals(0, remaining)
    }

    @Test
    fun `computeRemainingSeconds returns timeLeft when running and endsAt is null`() {
        val state = PomodoroTimerState(timeLeftSeconds = 60, initialTimeSeconds = 300, isRunning = true, endsAtEpochMillis = null)
        val remaining = PomodoroCompletionHandler.computeRemainingSeconds(state, currentTimeMillis = 1_000L)
        assertEquals(60, remaining)
    }

    @Test
    fun `computeRemainingSeconds computes time-to-endsAt when running`() {
        val state = PomodoroTimerState(
            timeLeftSeconds = 300,
            initialTimeSeconds = 300,
            isRunning = true,
            endsAtEpochMillis = 10_000L
        )
        val remaining = PomodoroCompletionHandler.computeRemainingSeconds(state, currentTimeMillis = 5_500L)
        assertEquals(5, remaining)
    }

    @Test
    fun `computeRemainingSeconds returns zero when expired`() {
        val state = PomodoroTimerState(
            timeLeftSeconds = 300,
            initialTimeSeconds = 300,
            isRunning = true,
            endsAtEpochMillis = 10_000L
        )
        val remaining = PomodoroCompletionHandler.computeRemainingSeconds(state, currentTimeMillis = 20_000L)
        assertEquals(0, remaining)
    }

    @Test
    fun `isExpired is true only when running and remaining is zero`() {
        val running = PomodoroTimerState(
            isRunning = true,
            timeLeftSeconds = 0,
            initialTimeSeconds = 300,
            endsAtEpochMillis = 100L
        )
        val paused = running.copy(isRunning = false)
        val future = running.copy(endsAtEpochMillis = 1_000_000L)
        assertTrue(PomodoroCompletionHandler.isExpired(running, currentTimeMillis = 200L))
        assertFalse(PomodoroCompletionHandler.isExpired(paused, currentTimeMillis = 200L))
        assertFalse(PomodoroCompletionHandler.isExpired(future, currentTimeMillis = 200L))
    }

    @Test
    fun `advanceToNextSession from pomodoro to short break increments session count`() {
        val state = PomodoroTimerState(
            mode = PomodoroMode.POMODORO,
            timeLeftSeconds = 0,
            initialTimeSeconds = 1500,
            sessionsCompleted = 0,
            selectedOption = 25
        )
        val next = PomodoroCompletionHandler.advanceToNextSession(state)
        assertEquals(PomodoroMode.SHORT_BREAK, next.mode)
        assertEquals(1, next.sessionsCompleted)
        assertEquals(PomodoroDefaults.SHORT_BREAK_MINUTES * 60, next.timeLeftSeconds)
        assertEquals(PomodoroDefaults.SHORT_BREAK_MINUTES * 60, next.initialTimeSeconds)
        assertEquals(PomodoroDefaults.SHORT_BREAK_MINUTES, next.selectedOption)
        assertFalse(next.isRunning)
        assertNull(next.endsAtEpochMillis)
        assertTrue(next.showCompletionMessage)
        assertEquals(PomodoroMode.POMODORO, next.completedMode)
    }

    @Test
    fun `advanceToNextSession from pomodoro to long break on fourth session`() {
        val state = PomodoroTimerState(
            mode = PomodoroMode.POMODORO,
            timeLeftSeconds = 0,
            initialTimeSeconds = 1500,
            sessionsCompleted = 3
        )
        val next = PomodoroCompletionHandler.advanceToNextSession(state)
        assertEquals(PomodoroMode.LONG_BREAK, next.mode)
        assertEquals(4, next.sessionsCompleted)
        assertEquals(PomodoroDefaults.LONG_BREAK_MINUTES * 60, next.timeLeftSeconds)
    }

    @Test
    fun `advanceToNextSession from short break back to pomodoro keeps session count`() {
        val state = PomodoroTimerState(
            mode = PomodoroMode.SHORT_BREAK,
            timeLeftSeconds = 0,
            initialTimeSeconds = 300,
            sessionsCompleted = 1,
            selectedOption = 5
        )
        val next = PomodoroCompletionHandler.advanceToNextSession(state)
        assertEquals(PomodoroMode.POMODORO, next.mode)
        assertEquals(1, next.sessionsCompleted)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, next.timeLeftSeconds)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES, next.selectedOption)
        assertEquals(PomodoroMode.SHORT_BREAK, next.completedMode)
    }

    @Test
    fun `advanceToNextSession from long break back to pomodoro keeps session count`() {
        val state = PomodoroTimerState(
            mode = PomodoroMode.LONG_BREAK,
            timeLeftSeconds = 0,
            initialTimeSeconds = 900,
            sessionsCompleted = 4,
            selectedOption = 15
        )
        val next = PomodoroCompletionHandler.advanceToNextSession(state)
        assertEquals(PomodoroMode.POMODORO, next.mode)
        assertEquals(4, next.sessionsCompleted)
        assertEquals(PomodoroDefaults.POMODORO_MINUTES * 60, next.timeLeftSeconds)
    }
}
