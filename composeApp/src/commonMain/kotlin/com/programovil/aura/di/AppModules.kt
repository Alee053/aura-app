package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.todo.di.todoModule
import com.programovil.aura.todo.presentation.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { AuthViewModel() }
    viewModel { TodoViewModel(get()) }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    // Data layer specifics if needed
}

fun getModules() = listOf(
    authModule,
    todoModule,
    domainModule,
    dataModule,
    presentationModule
)