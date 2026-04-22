package com.programovil.aura.auth.domain

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.programovil.aura.auth.presentation.AuthViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

actual fun createAuthService(): AuthService = AndroidAuthService()

private class AndroidAuthService : AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val scope = MainScope()

    override fun getCurrentAuthState(): AuthViewModel.AuthState {
        return if (auth.currentUser != null) AuthViewModel.AuthState.SignedIn else AuthViewModel.AuthState.SignedOut
    }

    override fun handleSignIn(idToken: String?, onResult: (AuthViewModel.AuthState) -> Unit) {
        if (idToken == null) {
            onResult(AuthViewModel.AuthState.Error("No token"))
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(AuthViewModel.AuthState.SignedIn)
                } else {
                    onResult(AuthViewModel.AuthState.Error(task.exception?.message ?: "Unknown error"))
                }
            }
    }

    override fun signOut() {
        auth.signOut()
    }

    override fun addStateListener(onStateChanged: (AuthViewModel.AuthState) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val state = if (firebaseAuth.currentUser != null) {
                AuthViewModel.AuthState.SignedIn
            } else {
                AuthViewModel.AuthState.SignedOut
            }
            onStateChanged(state)
        }
    }
}
