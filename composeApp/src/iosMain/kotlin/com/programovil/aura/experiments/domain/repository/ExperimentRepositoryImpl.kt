package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.ExperimentEvent

actual fun createExperimentRepository(): ExperimentRepository = object : ExperimentRepository {
    override suspend fun logEvent(event: ExperimentEvent): Result<Unit> = Result.success(Unit)
}
