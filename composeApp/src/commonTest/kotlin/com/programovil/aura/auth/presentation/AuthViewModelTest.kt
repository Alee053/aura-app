package com.programovil.aura.auth.presentation

import androidx.lifecycle.viewModelScope
import com.programovil.aura.auth.domain.AuthError
import com.programovil.aura.auth.domain.AuthService
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthViewModelTest {

    private val viewModels = mutableListOf<AuthViewModel>()

    @AfterTest
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }

    @Test
    fun `addStateListener wires the listener to update auth state`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        service.lastListener?.invoke(AuthViewModel.AuthState.SignedIn)

        assertEquals(AuthViewModel.AuthState.SignedIn, viewModel.authState.value)
    }

    @Test
    fun `multiple state updates are reflected in the state`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        service.lastListener?.invoke(AuthViewModel.AuthState.SignedOut)
        service.lastListener?.invoke(AuthViewModel.AuthState.SignedIn)

        assertEquals(AuthViewModel.AuthState.SignedIn, viewModel.authState.value)
    }

    @Test
    fun `handleSignInResult sets loading and delegates to service`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        viewModel.handleSignInResult("id-token")

        assertEquals(AuthViewModel.AuthState.Loading, viewModel.authState.value)
        assertEquals("id-token", service.lastSignInIdToken)

        service.lastSignInCallback?.invoke(AuthViewModel.AuthState.SignedIn)
        assertEquals(AuthViewModel.AuthState.SignedIn, viewModel.authState.value)
    }

    @Test
    fun `handleSignInResult with null idToken still delegates`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        viewModel.handleSignInResult(null)

        assertEquals(AuthViewModel.AuthState.Loading, viewModel.authState.value)
        assertEquals(null, service.lastSignInIdToken)

        service.lastSignInCallback?.invoke(AuthViewModel.AuthState.SignedOut)
        assertEquals(AuthViewModel.AuthState.SignedOut, viewModel.authState.value)
    }

    @Test
    fun `reportAuthError sets error state with the given error`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        viewModel.reportAuthError(AuthError.NoToken)

        val state = viewModel.authState.value
        assertTrue(state is AuthViewModel.AuthState.Error)
        assertEquals(AuthError.NoToken, state.error)
    }

    @Test
    fun `cancelSignIn returns to signed out without delegating to auth service`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        viewModel.cancelSignIn()

        assertEquals(AuthViewModel.AuthState.SignedOut, viewModel.authState.value)
        assertEquals(null, service.lastSignInIdToken)
        assertEquals(0, service.signOutCount)
    }

    @Test
    fun `signOut calls the service`() {
        val service = FakeAuthService()
        val viewModel = createViewModel(service)

        viewModel.signOut()

        assertEquals(1, service.signOutCount)
    }

    private fun createViewModel(service: AuthService): AuthViewModel {
        val viewModel = AuthViewModel(service)
        viewModels += viewModel
        return viewModel
    }
}

private class FakeAuthService : AuthService {
    var lastListener: ((AuthViewModel.AuthState) -> Unit)? = null
    var lastSignInIdToken: String? = null
    var lastSignInCallback: ((AuthViewModel.AuthState) -> Unit)? = null
    var signOutCount: Int = 0

    override fun getCurrentAuthState(): AuthViewModel.AuthState = AuthViewModel.AuthState.SignedOut

    override fun handleSignIn(idToken: String?, callback: (AuthViewModel.AuthState) -> Unit) {
        lastSignInIdToken = idToken
        lastSignInCallback = callback
    }

    override fun signOut() {
        signOutCount += 1
    }

    override fun addStateListener(listener: (AuthViewModel.AuthState) -> Unit) {
        lastListener = listener
    }
}
