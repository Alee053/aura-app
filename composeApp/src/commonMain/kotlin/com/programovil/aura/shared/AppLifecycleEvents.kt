package com.programovil.aura.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppLifecycleEvents {
    private val _foregroundEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val foregroundEvents: SharedFlow<Unit> = _foregroundEvents.asSharedFlow()

    fun onForegrounded() {
        _foregroundEvents.tryEmit(Unit)
    }
}
