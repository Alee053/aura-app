package com.programovil.aura.regionsync.data.remote

import com.programovil.aura.regionsync.domain.model.RegionalDataItem

// This is a placeholder. Actual implementation will depend on Firebase KMP library or expect/actual.
// The user needs to configure Firebase Realtime Database manually.
expect class FirebaseRealtimeDatabaseDataSource {
    suspend fun fetchRegionalData(regionId: String): List<RegionalDataItem>
}
