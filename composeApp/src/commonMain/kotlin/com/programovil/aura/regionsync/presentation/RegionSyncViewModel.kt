package com.programovil.aura.regionsync.presentation

data class RegionSyncUiState(
    val activeRegions: List<com.programovil.aura.regionsync.domain.model.Region> = emptyList(),
    val selectedRegion: com.programovil.aura.regionsync.domain.model.Region? = null,
    val regionalData: List<com.programovil.aura.regionsync.domain.model.RegionalDataItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null
)
