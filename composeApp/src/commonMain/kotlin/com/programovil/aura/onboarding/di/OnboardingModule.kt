package com.programovil.aura.onboarding.di

import com.programovil.aura.onboarding.data.OnboardingRepositoryImpl
import com.programovil.aura.onboarding.domain.OnboardingRepository
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import com.programovil.aura.shared.data.DATASTORE_FILE_NAME
import com.programovil.aura.shared.data.createDataStore
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    single<OnboardingRepository> {
        OnboardingRepositoryImpl(
            remoteConfigService = get(),
            dataStore = createDataStore()
        )
    }
    viewModelOf(::OnboardingViewModel)
}
