package com.programovil.aura.di

import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel(get(), get(), get(), get()) }
}