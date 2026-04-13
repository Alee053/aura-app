package com.programovil.aura.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    singleOf(::TodoRepositoryImpl).bind<TodoRepository>()
}