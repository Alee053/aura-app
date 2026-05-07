package com.programovil.aura.regionsync.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.regionsync.domain.model.Region
import com.programovil.aura.regionsync.domain.usecase.GetActiveRegionsUseCase
import com.programovil.aura.regionsync.domain.usecase.SyncRegionalDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegionSyncViewModel(
    private val getActiveRegionsUseCase: GetActiveRegionsUseCase,
    private val syncRegionalDataUseCase: SyncRegionalDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegionSyncUiState())
    val uiState: StateFlow<RegionSyncUiState> = _uiState.asStateFlow()

    init {
        loadActiveRegions()
    }

    fun loadActiveRegions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val regions = getActiveRegionsUseCase()
                _uiState.value = _uiState.value.copy(
                    activeRegions = regions,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading regions"
                )
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                syncRegionalDataUseCase()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Error syncing data"
                )
            }
        }
    }

    fun selectRegion(region: Region) {
        _uiState.value = _uiState.value.copy(selectedRegion = region)
    }
}
