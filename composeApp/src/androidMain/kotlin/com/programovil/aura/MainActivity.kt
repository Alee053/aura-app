package com.programovil.aura

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.programovil.aura.auth.presentation.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val credentialManager by lazy { CredentialManager.create(this) }

    private fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val result = getCredential(filterByAuthorizedAccounts = true)
                handleCredentialResult(result)
            } catch (e: GetCredentialException) {
                Log.d(TAG, "No authorized accounts, showing account picker: ${e.message}")
                try {
                    val result = getCredential(filterByAuthorizedAccounts = false)
                    handleCredentialResult(result)
                } catch (e2: GetCredentialException) {
                    Log.e(TAG, "Credential sign-in failed: ${e2.message}")
                    authViewModel.handleSignInResult(null)
                } catch (e2: Exception) {
                    Log.e(TAG, "Unexpected error: ${e2.message}")
                    authViewModel.handleSignInResult(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
                authViewModel.handleSignInResult(null)
            }
        }
    }

    private suspend fun getCredential(filterByAuthorizedAccounts: Boolean): GetCredentialResponse {
        val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId("1093577707060-no5gln1m1iri29khllqd0us0bchd7gqf.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return credentialManager.getCredential(
            context = this@MainActivity,
            request = request
        )
    }

    private fun handleCredentialResult(result: GetCredentialResponse) {
        val credential = result.credential
        val googleIdType = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        if (credential.type == googleIdType) {
            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            authViewModel.handleSignInResult(googleIdTokenCredential.idToken)
        } else {
            Log.e(TAG, "Unexpected credential type: ${credential.type}")
            authViewModel.handleSignInResult(null)
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
