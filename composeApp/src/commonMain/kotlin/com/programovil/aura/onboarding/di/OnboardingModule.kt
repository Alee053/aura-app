package com.programovil.aura.onboarding.di

import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.data.OnboardingPreferencesImpl
import com.programovil.aura.onboarding.data.repository.OnboardingRepositoryImpl
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val onboardingModule = module {
    singleOf(::OnboardingRepositoryImpl) bind OnboardingRepository::class
    singleOf(::OnboardingPreferencesImpl) bind OnboardingPreferences::class
    factoryOf(::GetOnboardingSlidesUseCase)
    viewModelOf(::OnboardingViewModel)
}
