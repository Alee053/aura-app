package com.programovil.aura.regionsync.data.remote

import com.programovil.aura.regionsync.domain.model.Region

// This is a placeholder. Actual implementation will depend on Firebase KMP library or expect/actual.
// The user needs to configure Firebase Remote Config manually.
expect class FirebaseRemoteConfigDataSource {
    suspend fun getActiveRegionIds(): List<String>
}
