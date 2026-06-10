package com.programovil.aura.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppVisibilityTracker {
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun markForeground() {
        _isForeground.value = true
        AppLifecycleEvents.onForegrounded()
    }

    fun markBackground() {
        _isForeground.value = false
    }
}
