package com.programovil.aura.pomodoro.domain

import kotlinx.datetime.Clock

interface TimeProvider {
    fun currentTimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
