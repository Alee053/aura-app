package com.programovil.aura.todo.di

import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.domain.repository.createTodoRepository
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val todoModule = module {
    // Data layer
    single { createTodoRepository() }

    // Domain layer - use cases
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)

    // Presentation layer
    viewModel { TodoViewModel(get(), get(), get(), get()) }
}
