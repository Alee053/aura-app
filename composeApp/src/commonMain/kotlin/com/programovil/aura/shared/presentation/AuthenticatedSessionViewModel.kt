package com.programovil.aura.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Survives Activity recreation, but authenticated children are cleared on sign-out. */
class AuthenticatedSessionViewModel : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    fun clearSession() { viewModelStore.clear() }
    override fun onCleared() { clearSession() }
}
