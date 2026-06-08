package com.programovil.aura

import android.os.Bundle
import android.util.Log
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
import com.programovil.aura.auth.presentation.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

private const val TAG = "AuraSignIn"

private const val GOOGLE_SERVER_CLIENT_ID =
    "1093577707060-no5gln1m1iri29khllqd0us0bchd7gqf.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val credentialManager by lazy { CredentialManager.create(this) }

    private fun buildGoogleIdOption(filterByAuthorizedAccounts: Boolean): GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setServerClientId(GOOGLE_SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()

    private suspend fun requestGoogleIdToken(filterByAuthorizedAccounts: Boolean): String? {
        Log.d(TAG, "Requesting Google ID token (filterAuthorized=$filterByAuthorizedAccounts)")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(buildGoogleIdOption(filterByAuthorizedAccounts))
            .build()
        val result: GetCredentialResponse = credentialManager.getCredential(
            context = this@MainActivity,
            request = request
        )
        val credential = result.credential
        val googleIdType = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        Log.d(TAG, "Credential type returned: ${credential.type}")
        if (credential.type != googleIdType) {
            Log.e(TAG, "Unexpected credential type: ${credential.type}")
            return null
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        Log.d(TAG, "Got idToken (length=${idToken?.length ?: 0})")
        return idToken
    }

    private fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val idToken = requestGoogleIdToken(filterByAuthorizedAccounts = true)
                authViewModel.handleSignInResult(idToken)
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "User cancelled the sign-in flow")
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No authorized Google account, falling back to all-accounts picker: ${e.message}")
                try {
                    val idToken = requestGoogleIdToken(filterByAuthorizedAccounts = false)
                    authViewModel.handleSignInResult(idToken)
                } catch (fallbackError: GetCredentialCancellationException) {
                    Log.w(TAG, "User cancelled the fallback picker")
                } catch (fallbackError: GetCredentialException) {
                    Log.e(TAG, "Fallback sign-in failed: type=${fallbackError.type} msg=${fallbackError.message}")
                    authViewModel.handleSignInResult(null)
                    authViewModel.reportAuthError(AuthError.NoCredential)
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Fallback unexpected error", fallbackError)
                    authViewModel.handleSignInResult(null)
                    authViewModel.reportAuthError(AuthError.Exception(fallbackError.message))
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential sign-in failed: type=${e.type} msg=${e.message}")
                authViewModel.handleSignInResult(null)
                authViewModel.reportAuthError(AuthError.NoCredential)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                authViewModel.handleSignInResult(null)
                authViewModel.reportAuthError(AuthError.Exception(e.message))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel = getViewModel()

        setContent {
            App(
                onSignInClick = {
                    launchGoogleSignIn()
                }
            )
        }
    }
}
