package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.habit.di.habitModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.todo.di.todoModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun getRegionSyncModule(): Module

fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    getRegionSyncModule(),
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
