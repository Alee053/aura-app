package org.example.aura_app.di

import org.example.aura_app.presentation.todo.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel() }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    // Repository implementations and data sources go here
}

fun getModules() = listOf(
    domainModule,
    dataModule,
    presentationModule
)
