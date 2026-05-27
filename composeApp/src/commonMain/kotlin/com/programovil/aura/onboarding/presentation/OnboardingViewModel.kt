package com.programovil.aura.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.onboarding.domain.OnboardingRepository
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

sealed class OnboardingUiState {
    object Loading : OnboardingUiState()
    data class Content(val slides: List<OnboardingSlide>) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val repository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome: StateFlow<Boolean> = _navigateToHome.asStateFlow()

    private var slides: List<OnboardingSlide> = emptyList()

    init {
        loadSlides()
    }

    private fun loadSlides() {
        viewModelScope.launch {
            repository.getSlides().collect { result ->
                result.fold(
                    onSuccess = { slidesList ->
                        slides = slidesList
                        _uiState.value = OnboardingUiState.Content(slidesList)
                    },
                    onFailure = { error ->
                        _uiState.value = OnboardingUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    fun nextPage() {
        val slides = ( _uiState.value as? OnboardingUiState.Content)?.slides ?: return
        if (_currentPage.value < slides.size - 1) {
            _currentPage.value++
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
        }
    }

    fun skip() {
        _navigateToHome.value = true
    }

    fun start() {
        viewModelScope.launch {
            repository.setOnboarded(true)
            _navigateToHome.value = true
        }
    }

    fun onNavigated() {
        _navigateToHome.value = false
    }
}
