package com.programovil.aura.regionsync.domain.usecase

import com.programovil.aura.regionsync.domain.repository.RegionSyncRepository

class SyncRegionalDataUseCase(private val repository: RegionSyncRepository) {
    suspend operator fun invoke() {
        val activeRegions = repository.getActiveRegions()
        activeRegions.forEach { region ->
            val data = repository.fetchRegionalData(region.id)
            repository.saveRegionalData(data)
        }
    }
}
