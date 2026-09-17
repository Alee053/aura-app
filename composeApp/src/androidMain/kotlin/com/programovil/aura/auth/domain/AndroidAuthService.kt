package com.programovil.aura.auth.domain

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.programovil.aura.auth.presentation.AuthViewModel

actual fun createAuthService(): AuthService = AndroidAuthService()

private const val TAG = "AndroidAuthService"

private class AndroidAuthService : AuthService {
    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentAuthState(): AuthViewModel.AuthState {
        return if (auth.currentUser != null) AuthViewModel.AuthState.SignedIn else AuthViewModel.AuthState.SignedOut
    }

    override fun handleSignIn(idToken: String?, onResult: (AuthViewModel.AuthState) -> Unit) {
        if (idToken == null || idToken.isBlank()) {
            Log.w(TAG, "handleSignIn called with null/blank idToken")
            onResult(AuthViewModel.AuthState.Error(AuthError.NoToken))
            return
        }
        Log.d(TAG, "handleSignIn: idToken length=${idToken.length}")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential: success, uid=${auth.currentUser?.uid}")
                    onResult(AuthViewModel.AuthState.SignedIn)
                } else {
                    val ex = task.exception
                    Log.e(
                        TAG,
                        "signInWithCredential failed: ${ex?.javaClass?.name}: ${ex?.message}",
                        ex
                    )
                    val causeMsg = generateSequence(ex as Throwable?) { it.cause }
                        .mapNotNull { it.message }
                        .firstOrNull()
                    val message = listOfNotNull(ex?.message, causeMsg)
                        .firstOrNull { it.isNotBlank() }
                    onResult(AuthViewModel.AuthState.Error(AuthError.Exception(message)))
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
