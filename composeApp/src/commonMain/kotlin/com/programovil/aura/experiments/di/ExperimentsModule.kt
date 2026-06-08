package com.programovil.aura.experiments.di

import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.experiments.domain.repository.createExperimentRepository
import com.programovil.aura.experiments.domain.repository.createUserPlanRepository
import com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase
import com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.experiments.domain.usecase.LogExperimentEventUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val experimentsModule: Module = module {
    single { createUserPlanRepository() } bind UserPlanRepository::class
    single { createExperimentRepository() } bind ExperimentRepository::class

    factoryOf(::GetUserPlanUseCase)
    factoryOf(::GetHomeVariantUseCase)
    factoryOf(::GetNotificationVariantUseCase)
    factoryOf(::LogExperimentEventUseCase)
}
