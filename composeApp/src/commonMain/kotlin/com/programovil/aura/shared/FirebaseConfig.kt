package com.programovil.aura.shared

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

object FirebaseConfig {
    private var _isInitialized = false

    fun initialize(context: Context) {
        if (_isInitialized) return
        Firebase.initialize(context)
        _isInitialized = true
    }

    val auth = Firebase.auth
    val firestore = Firebase.firestore
}
