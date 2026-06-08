package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import io.mockative.Mockable

@Mockable
interface ExperimentRepository {
    suspend fun logEvent(event: ExperimentEvent): Result<Unit>
}

expect fun createExperimentRepository(): ExperimentRepository
