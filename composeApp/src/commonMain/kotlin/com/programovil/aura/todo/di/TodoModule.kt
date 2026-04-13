package com.programovil.aura.todo.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import org.koin.dsl.module

val todoModule = module {
    single<TodoRepository> { TodoRepositoryImpl() }
}
