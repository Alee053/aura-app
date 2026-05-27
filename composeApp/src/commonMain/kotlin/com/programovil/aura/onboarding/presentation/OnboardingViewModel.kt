package com.programovil.aura.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val getSlidesUseCase: GetOnboardingSlidesUseCase,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    sealed class OnboardingState {
        data object Loading : OnboardingState()
        data class Loaded(
            val slides: List<OnboardingSlide>,
            val currentIndex: Int,
            val isLastSlide: Boolean
        ) : OnboardingState()
        data class Completed(val skipped: Boolean) : OnboardingState()
        data class Error(val message: String) : OnboardingState()
    }

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Loading)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        loadSlides()
    }

    private fun loadSlides() {
        viewModelScope.launch {
            getSlidesUseCase().fold(
                onSuccess = { slides ->
                    if (slides.isEmpty()) {
                        _state.value = OnboardingState.Completed(skipped = true)
                    } else {
                        _state.value = OnboardingState.Loaded(
                            slides = slides,
                            currentIndex = 0,
                            isLastSlide = slides.size == 1
                        )
                    }
                },
                onFailure = { error ->
                    _state.value = OnboardingState.Error(
                        error.message ?: "Failed to load onboarding"
                    )
                }
            )
        }
    }

    fun next() {
        val current = _state.value as? OnboardingState.Loaded ?: return
        if (current.currentIndex < current.slides.lastIndex) {
            val newIndex = current.currentIndex + 1
            _state.value = current.copy(
                currentIndex = newIndex,
                isLastSlide = newIndex == current.slides.lastIndex
            )
        }
    }

    fun previous() {
        val current = _state.value as? OnboardingState.Loaded ?: return
        if (current.currentIndex > 0) {
            val newIndex = current.currentIndex - 1
            _state.value = current.copy(
                currentIndex = newIndex,
                isLastSlide = false
            )
        }
    }

    fun skip() {
        _state.value = OnboardingState.Completed(skipped = true)
    }

    fun start() {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingCompleted()
            _state.value = OnboardingState.Completed(skipped = false)
        }
    }
}