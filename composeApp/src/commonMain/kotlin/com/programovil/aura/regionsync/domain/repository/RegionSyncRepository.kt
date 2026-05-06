package com.programovil.aura.regionsync.domain.repository

import com.programovil.aura.regionsync.domain.model.Region
import com.programovil.aura.regionsync.domain.model.RegionalDataItem
import kotlinx.coroutines.flow.Flow

interface RegionSyncRepository {
    suspend fun getActiveRegions(): List<Region>
    suspend fun fetchRegionalData(regionId: String): List<RegionalDataItem>
    suspend fun saveRegionalData(data: List<RegionalDataItem>)
    fun getRegionalDataFlow(regionId: String): Flow<List<RegionalDataItem>>
}
