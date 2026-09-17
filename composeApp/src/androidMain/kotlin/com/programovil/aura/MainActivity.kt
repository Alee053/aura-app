package com.programovil.aura

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.programovil.aura.auth.domain.AuthError
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.shared.PomodoroLaunchEvents
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import java.security.SecureRandom

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var credentialRequestInFlight by mutableStateOf(false)

    private lateinit var authViewModel: AuthViewModel
    private val credentialManager by lazy { CredentialManager.create(this) }

    private fun buildGoogleIdOption(filterByAuthorizedAccounts: Boolean): GetGoogleIdOption {
        val nonceBytes = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(nonceBytes)
        val nonce = nonceBytes.joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

        return GetGoogleIdOption.Builder()
            // The google-services Gradle plugin generates this value from the
            // type-3 OAuth client in google-services.json. Keeping it here in
            // sync with Firebase avoids a second hardcoded client ID.
            .setServerClientId(getString(R.string.default_web_client_id))
            .setNonce(nonce)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()
    }

    private suspend fun requestGoogleIdToken(filterByAuthorizedAccounts: Boolean): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(buildGoogleIdOption(filterByAuthorizedAccounts))
            .build()

        val result: GetCredentialResponse = credentialManager.getCredential(
            context = this@MainActivity,
            request = request
        )
        val credential = result.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.e(TAG, "Unexpected credential type: ${credential.type}")
            return null
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun launchGoogleSignIn() {
        if (credentialRequestInFlight) return
        credentialRequestInFlight = true
        lifecycleScope.launch {
            try {
                // The first request is seamless for users who already granted
                // access. A new install/device can legitimately have no
                // authorized account, so retry with the account picker.
                authViewModel.handleSignInResult(
                    requestGoogleIdToken(filterByAuthorizedAccounts = true)
                )
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Google sign-in cancelled")
                authViewModel.cancelSignIn()
            } catch (e: NoCredentialException) {
                Log.d(TAG, "No authorized Google account; opening account picker")
                try {
                    authViewModel.handleSignInResult(
                        requestGoogleIdToken(filterByAuthorizedAccounts = false)
                    )
                } catch (fallbackError: GetCredentialCancellationException) {
                    Log.d(TAG, "Google account picker cancelled")
                    authViewModel.cancelSignIn()
                } catch (fallbackError: NoCredentialException) {
                    Log.w(TAG, "No Google credential available", fallbackError)
                    authViewModel.reportAuthError(AuthError.NoCredential)
                } catch (fallbackError: GetCredentialException) {
                    Log.e(TAG, "Google account picker failed", fallbackError)
                    authViewModel.reportAuthError(
                        AuthError.Exception(fallbackError.message)
                    )
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Unexpected Google account picker error", fallbackError)
                    authViewModel.reportAuthError(
                        AuthError.Exception(fallbackError.message)
                    )
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google credential request failed", e)
                authViewModel.reportAuthError(AuthError.Exception(e.message))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected Google sign-in error", e)
                authViewModel.reportAuthError(AuthError.Exception(e.message))
            } finally {
                credentialRequestInFlight = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel = getViewModel()
        handleIntent(intent)

        setContent {
            App(
                credentialRequestInFlight = credentialRequestInFlight,
                onSignInClick = {
                    launchGoogleSignIn()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_OPEN_POMODORO_COMPLETION ||
            intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_POMODORO_COMPLETION, false) == true
        ) {
            PomodoroLaunchEvents.requestSync()
            intent.removeExtra(NotificationHelper.EXTRA_OPEN_POMODORO_COMPLETION)
            intent.action = null
        }
    }

    private companion object {
        const val NONCE_SIZE = 32
    }
}
