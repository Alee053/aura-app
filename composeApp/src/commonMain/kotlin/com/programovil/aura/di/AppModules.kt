package com.programovil.aura.di

import com.programovil.aura.data.repository.TodoRepositoryImpl
import com.programovil.aura.domain.repository.TodoRepository
import com.programovil.aura.presentation.auth.AuthViewModel
import com.programovil.aura.presentation.todo.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel(get()) }
    viewModel { AuthViewModel() }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    single<TodoRepository> { TodoRepositoryImpl() }
}

fun getModules() = listOf(
    domainModule,
    dataModule,
    presentationModule
)
