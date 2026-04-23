package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.habit.di.habitModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.todo.di.todoModule
import org.koin.dsl.module

fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    module {
        single<RemoteConfigService> { remoteConfigService }
        // Se inyecta ConfigDao para la sincronización con caché local
        single { FeatureFlagManager(get(), get()) }
    }
)
