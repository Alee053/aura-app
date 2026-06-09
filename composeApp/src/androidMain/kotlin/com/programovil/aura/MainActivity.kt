package com.programovil.aura

import android.content.Intent
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
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.shared.PomodoroLaunchEvents
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val credentialManager by lazy { CredentialManager.create(this) }

    private fun launchGoogleSignIn() {
        val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )
                val credential = result.credential
                val googleIdType = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                if (credential.type == googleIdType) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    authViewModel.handleSignInResult(googleIdTokenCredential.idToken)
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    authViewModel.handleSignInResult(null)
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential sign-in failed: ${e.message}")
                authViewModel.handleSignInResult(null)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
                authViewModel.handleSignInResult(null)
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
}
