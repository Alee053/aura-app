package com.programovil.aura.onboarding.di

import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.data.repository.OnboardingRepositoryImpl
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val onboardingModule = module {
    singleOf(::OnboardingRepositoryImpl) bind OnboardingRepository::class
    single { OnboardingPreferences(get()) }
    factoryOf(::GetOnboardingSlidesUseCase)
}