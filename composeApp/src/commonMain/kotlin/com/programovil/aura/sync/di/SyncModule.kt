package com.programovil.aura.sync.di

import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.sync.data.repository.SyncQueueRepository
import org.koin.dsl.module

val syncModule = module {
    single { SyncQueueRepository(get()) }
}