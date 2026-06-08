package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.repository.ExperimentRepository

class LogExperimentEventUseCase(private val repository: ExperimentRepository) {
    suspend operator fun invoke(event: ExperimentEvent): Result<Unit> = repository.logEvent(event)
}
