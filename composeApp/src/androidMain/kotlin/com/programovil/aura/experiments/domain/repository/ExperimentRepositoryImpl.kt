package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.data.repository.FirebaseExperimentRepositoryImpl

actual fun createExperimentRepository(): ExperimentRepository = FirebaseExperimentRepositoryImpl()
