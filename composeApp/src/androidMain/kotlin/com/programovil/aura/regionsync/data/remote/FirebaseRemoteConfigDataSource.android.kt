package com.programovil.aura.regionsync.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

actual class FirebaseRemoteConfigDataSource {
    private val remoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour
        })
        setDefaultsAsync(mapOf("active_regions" to "north,south")) // Default regions
    }

    actual suspend fun getActiveRegionIds(): List<String> {
        remoteConfig.fetchAndActivate().await()
        val activeRegionsString = remoteConfig.getString("active_regions")
        return activeRegionsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
