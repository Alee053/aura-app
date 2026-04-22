package com.programovil.aura.habit.data.sync

private class NoOpHabitSyncScheduler : HabitSyncScheduler {
    override fun enqueueSync() = Unit
    override suspend fun syncNow(): Boolean = false
}

actual fun createHabitSyncScheduler(): HabitSyncScheduler = NoOpHabitSyncScheduler()
