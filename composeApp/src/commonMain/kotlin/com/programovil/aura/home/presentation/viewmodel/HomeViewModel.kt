package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.experiments.domain.model.HomeVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.home.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val dashboardData: DashboardData = DashboardData(),
    val isLoading: Boolean = true,
    val homeVariant: HomeVariant = HomeVariant(
        showsDailyMotivation = false,
        tone = Tone.Gentle,
        motivationPhrase = emptyMap()
    ),
    val loadFailed: Boolean = false,
    val hasData: Boolean = false,
    val locale: String = "en"
) {
    val resolvedMotivationPhrase: String
        get() = homeVariant.motivationPhrase[locale]
            ?: homeVariant.motivationPhrase["en"]
            ?: ""
}

class HomeViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val getHomeVariantUseCase: GetHomeVariantUseCase,
    locale: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(locale = locale))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null
    fun retryLoad() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getDashboardDataUseCase().catch { emit(Result.failure(it)) }.collect { result ->
                result.onSuccess { data ->
                    _uiState.update { it.copy(dashboardData = data, isLoading = false, loadFailed = false, hasData = true) }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
            }
        }
    }
    init {
        retryLoad()
        viewModelScope.launch {
            getHomeVariantUseCase().collect { variant ->
                _uiState.update { state -> state.copy(homeVariant = variant) }
            }
        }
    }
}
