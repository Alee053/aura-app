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
import com.google.firebase.auth.GoogleAuthProvider
import com.programovil.aura.data.remote.FirebaseConfig
import com.programovil.aura.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private val credentialManager = CredentialManager.create(this)

    private fun launchGoogleSignIn() {
        val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId("623141386052-ubttitbd4j8eqtvt4d2qvtbtba7cnfv9.apps.googleusercontent.com")
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
                Log.e(TAG, ">>>> credential type string: ${credential.type}")
                // googleid library uses TYPE_GOOGLE_ID_TOKEN_CREDENTIAL constant from GoogleIdTokenCredential
                val googleIdType = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                if (credential.type == googleIdType) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    Log.e(TAG, ">>>> idToken obtained, length=${idToken?.length}")
                    authViewModel.handleSignInResult(idToken)
                } else {
                    Log.e(TAG, ">>>> Unexpected credential type: ${credential.type}")
                    authViewModel.handleSignInResult(null)
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "getCredential failed: ${e.message}, type=${e.javaClass.simpleName}")
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

        setContent {
            App(
                onSignInClick = {
                    launchGoogleSignIn()
                }
            )
        }
    }
}