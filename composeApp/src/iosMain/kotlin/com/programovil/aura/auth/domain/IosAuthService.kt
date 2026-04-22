package com.programovil.aura.auth.domain

import com.programovil.aura.auth.presentation.AuthViewModel

actual fun createAuthService(): AuthService = IosAuthService()

private class IosAuthService : AuthService {
    override fun getCurrentAuthState(): AuthViewModel.AuthState = AuthViewModel.AuthState.SignedOut
    override fun handleSignIn(idToken: String?, onResult: (AuthViewModel.AuthState) -> Unit) {
        onResult(AuthViewModel.AuthState.Error("Not implemented on iOS"))
    }
    override fun signOut() {}
    override fun addStateListener(onStateChanged: (AuthViewModel.AuthState) -> Unit) {}
}
