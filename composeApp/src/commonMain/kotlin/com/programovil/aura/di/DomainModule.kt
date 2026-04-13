package com.programovil.aura.di

import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)
}