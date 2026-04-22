package com.programovil.aura.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.auth.domain.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService
) : ViewModel() {

    sealed class AuthState {
        data object Loading : AuthState()
        data object SignedIn : AuthState()
        data object SignedOut : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Rely on the listener to provide the initial state immediately
        authService.addStateListener { state ->
            _authState.value = state
        }
    }

    fun handleSignInResult(idToken: String?) {
        _authState.value = AuthState.Loading
        authService.handleSignIn(idToken) { state ->
            _authState.value = state
        }
    }

    fun signOut() {
        authService.signOut()
    }
}
