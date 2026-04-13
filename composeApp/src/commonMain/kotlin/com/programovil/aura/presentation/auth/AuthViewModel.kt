package com.programovil.aura.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.programovil.aura.data.remote.FirebaseConfig

class AuthViewModel : ViewModel() {

    sealed class AuthState {
        data object Loading : AuthState()
        data object SignedIn : AuthState()
        data object SignedOut : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _authState.value = if (FirebaseConfig.auth.currentUser != null) {
            AuthState.SignedIn
        } else {
            AuthState.SignedOut
        }
    }

    fun handleSignInResult(idToken: String?) {
        if (idToken == null) {
            _authState.value = AuthState.Error("Sign-in failed: no token")
            return
        }
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseConfig.auth.signInWithCredential(credential).await()
                _authState.value = AuthState.SignedIn
            } catch (e: ApiException) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        FirebaseConfig.auth.signOut()
        _authState.value = AuthState.SignedOut
    }
}