package com.programovil.aura.auth.di

import com.programovil.aura.auth.domain.createAuthService
import com.programovil.aura.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    single { createAuthService() }
    viewModelOf(::AuthViewModel)
}
