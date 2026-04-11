package com.programovil.aura.data.remote

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.initialize

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
