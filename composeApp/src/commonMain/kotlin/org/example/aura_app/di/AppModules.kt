package org.example.aura_app.di

import org.example.aura_app.data.repository.TodoRepositoryImpl
import org.example.aura_app.domain.repository.TodoRepository
import org.example.aura_app.presentation.auth.AuthViewModel
import org.example.aura_app.presentation.todo.TodoViewModel
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
