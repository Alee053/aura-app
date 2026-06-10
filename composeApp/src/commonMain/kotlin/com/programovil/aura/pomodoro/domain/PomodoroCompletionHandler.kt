package com.programovil.aura.pomodoro.domain

object PomodoroCompletionHandler {
    fun computeRemainingSeconds(
        state: PomodoroTimerState,
        currentTimeMillis: Long
    ): Int {
        if (!state.isRunning) {
            return state.timeLeftSeconds.coerceAtLeast(0)
        }

        val endsAt = state.endsAtEpochMillis ?: return state.timeLeftSeconds.coerceAtLeast(0)
        val millisLeft = endsAt - currentTimeMillis
        return ((millisLeft + 999L) / 1000L).coerceAtLeast(0L).toInt()
    }

    fun isExpired(
        state: PomodoroTimerState,
        currentTimeMillis: Long
    ): Boolean {
        return state.isRunning && computeRemainingSeconds(state, currentTimeMillis) <= 0
    }

    fun advanceToNextSession(state: PomodoroTimerState): PomodoroTimerState {
        val completedMode = state.mode
        val newSessionsCompleted: Int
        val nextMode: PomodoroMode
        val nextMinutes: Int

        if (completedMode == PomodoroMode.POMODORO) {
            newSessionsCompleted = state.sessionsCompleted + 1
            if (newSessionsCompleted % PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK == 0) {
                nextMode = PomodoroMode.LONG_BREAK
                nextMinutes = PomodoroDefaults.LONG_BREAK_MINUTES
            } else {
                nextMode = PomodoroMode.SHORT_BREAK
                nextMinutes = PomodoroDefaults.SHORT_BREAK_MINUTES
            }
        } else {
            newSessionsCompleted = state.sessionsCompleted
            nextMode = PomodoroMode.POMODORO
            nextMinutes = PomodoroDefaults.POMODORO_MINUTES
        }

        return state.copy(
            mode = nextMode,
            sessionsCompleted = newSessionsCompleted,
            timeLeftSeconds = nextMinutes * 60,
            initialTimeSeconds = nextMinutes * 60,
            selectedOption = nextMinutes,
            isRunning = false,
            endsAtEpochMillis = null,
            showCompletionMessage = true,
            completedMode = completedMode
        )
    }
}
