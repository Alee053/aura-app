package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule

fun getModules() = listOf(
    dataModule,
    domainModule,
    presentationModule,
    authModule
)