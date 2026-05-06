package com.programovil.aura.regionsync.domain.usecase

import com.programovil.aura.regionsync.domain.model.Region
import com.programovil.aura.regionsync.domain.repository.RegionSyncRepository

class GetActiveRegionsUseCase(private val repository: RegionSyncRepository) {
    suspend operator fun invoke(): List<Region> {
        return repository.getActiveRegions()
    }
}
