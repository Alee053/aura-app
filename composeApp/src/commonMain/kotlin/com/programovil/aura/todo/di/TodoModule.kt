package com.programovil.aura.todo.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.presentation.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val todoModule = module {
    single<TodoRepository> { TodoRepositoryImpl() }
    viewModel { TodoViewModel(get()) }
}
