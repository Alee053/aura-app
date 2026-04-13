package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.todo.di.todoModule
import org.koin.dsl.module

// ViewModels are registered in their feature modules (authModule, todoModule)
val presentationModule = module {
    // Keep minimal - duplicate ViewModel registrations cause BeanDefinitionOverrideException
}

fun getModules() = listOf(
    authModule,
    todoModule,
    presentationModule
)