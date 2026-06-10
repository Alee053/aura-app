package com.programovil.aura.auth.domain

sealed class AuthError {
    data object NoCredential : AuthError()
    data object NoToken : AuthError()
    data class Exception(val message: String?) : AuthError()
}
