package com.programovil.aura.habit.data.sync

interface HabitSyncScheduler {
    fun enqueueSync()
    suspend fun syncNow(): Boolean
}

expect fun createHabitSyncScheduler(): HabitSyncScheduler
