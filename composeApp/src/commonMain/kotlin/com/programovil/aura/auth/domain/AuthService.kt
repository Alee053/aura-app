package com.programovil.aura.auth.domain

import com.programovil.aura.auth.presentation.AuthViewModel

interface AuthService {
    fun getCurrentAuthState(): AuthViewModel.AuthState
    fun handleSignIn(idToken: String?, onResult: (AuthViewModel.AuthState) -> Unit)
    fun signOut()
    fun addStateListener(onStateChanged: (AuthViewModel.AuthState) -> Unit)
}

expect fun createAuthService(): AuthService
