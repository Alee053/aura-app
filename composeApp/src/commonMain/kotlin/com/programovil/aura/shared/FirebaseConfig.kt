package com.programovil.aura.shared

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

object FirebaseConfig {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
}
