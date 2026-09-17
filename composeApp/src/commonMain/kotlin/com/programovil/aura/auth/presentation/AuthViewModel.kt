package com.programovil.aura.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.auth.domain.AuthError
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
        data class Error(val error: AuthError) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var signInInFlight: Boolean = false

    init {
        authService.addStateListener { state ->
            // Don't let the underlying auth listener overwrite an in-flight
            // sign-in (the explicit handleSignIn result takes precedence).
            if (!signInInFlight) {
                _authState.value = state
            }
        }
    }

    fun handleSignInResult(idToken: String?) {
        signInInFlight = true
        _authState.value = AuthState.Loading
        authService.handleSignIn(idToken) { state ->
            signInInFlight = false
            _authState.value = state
        }
    }

    fun reportAuthError(error: AuthError) {
        signInInFlight = false
        _authState.value = AuthState.Error(error)
    }

    /**
     * Cancelling the platform credential picker happens before Firebase is
     * called, so it should return to the idle signed-out screen rather than
     * leaving the session gate in Loading.
     */
    fun cancelSignIn() {
        signInInFlight = false
        _authState.value = AuthState.SignedOut
    }

    fun signOut() {
        authService.signOut()
    }
}
