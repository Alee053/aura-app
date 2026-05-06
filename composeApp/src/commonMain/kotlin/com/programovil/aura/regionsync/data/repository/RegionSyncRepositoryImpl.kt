package com.programovil.aura.regionsync.data.repository

import com.programovil.aura.regionsync.data.local.RegionDataDao
import com.programovil.aura.regionsync.data.local.RegionDataEntity
import com.programovil.aura.regionsync.data.remote.FirebaseRealtimeDatabaseDataSource
import com.programovil.aura.regionsync.data.remote.FirebaseRemoteConfigDataSource
import com.programovil.aura.regionsync.domain.model.Region
import com.programovil.aura.regionsync.domain.model.RegionalDataItem
import com.programovil.aura.regionsync.domain.repository.RegionSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RegionSyncRepositoryImpl(
    private val remoteConfigDataSource: FirebaseRemoteConfigDataSource,
    private val realtimeDatabaseDataSource: FirebaseRealtimeDatabaseDataSource,
    private val regionDataDao: RegionDataDao
) : RegionSyncRepository {

    override suspend fun getActiveRegions(): List<Region> {
        val regionIds = remoteConfigDataSource.getActiveRegionIds()
        // For simplicity, we are creating Region objects with id as name.
        // In a real app, region names might come from Remote Config as well or a predefined list.
        return regionIds.map { Region(it, it.capitalize()) }
    }

    override suspend fun fetchRegionalData(regionId: String): List<RegionalDataItem> {
        return realtimeDatabaseDataSource.fetchRegionalData(regionId)
    }

    override suspend fun saveRegionalData(data: List<RegionalDataItem>) {
        // First, clear existing data for the regions being updated (optional, depending on merge strategy)
        if (data.isNotEmpty()) {
            val regionId = data.first().regionId
            regionDataDao.deleteRegionalData(regionId) // Clear data for this region
            regionDataDao.insertAll(data.map { RegionDataEntity.fromDomain(it) })
        }
    }

    override fun getRegionalDataFlow(regionId: String): Flow<List<RegionalDataItem>> {
        return regionDataDao.getRegionalDataFlow(regionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
