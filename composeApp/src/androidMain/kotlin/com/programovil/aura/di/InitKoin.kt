package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.todo.di.todoModule

fun getModules() = listOf(
    authModule,
    todoModule,
    notificationModule
)