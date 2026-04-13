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
        android.util.Log.e("AuthViewModel", ">>>> handleSignInResult called, idToken present=${idToken != null}")
        if (idToken == null) {
            android.util.Log.e("AuthViewModel", ">>>> idToken is null, setting Error state")
            _authState.value = AuthState.Error("Sign-in failed: no token")
            return
        }
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                android.util.Log.e("AuthViewModel", ">>>> about to call signInWithCredential")
                FirebaseConfig.auth.signInWithCredential(credential).await()
                android.util.Log.e("AuthViewModel", ">>>> signInWithCredential succeeded, currentUser=${FirebaseConfig.auth.currentUser?.email}")
                _authState.value = AuthState.SignedIn
            } catch (e: ApiException) {
                android.util.Log.e("AuthViewModel", ">>>> ApiException: ${e.statusCode} - ${e.message}")
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", ">>>> Exception: ${e.javaClass.simpleName} - ${e.message}")
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        FirebaseConfig.auth.signOut()
        _authState.value = AuthState.SignedOut
    }
}
