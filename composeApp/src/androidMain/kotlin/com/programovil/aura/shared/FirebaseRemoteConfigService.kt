package com.programovil.aura.shared

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.tasks.await

private const val TAG = "RemoteConfig"

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val defaults = FeatureFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        Log.d(TAG, "Setting defaults: $defaults")
        remoteConfig.setDefaultsAsync(defaults)
    }

    override suspend fun getBoolean(flag: FeatureFlag): Boolean {
        val value = remoteConfig.getBoolean(flag.key)
        Log.d(TAG, "getBoolean(${flag.key}) = $value (default: ${flag.defaultValue})")
        return value
    }

    override suspend fun getString(flag: FeatureFlag, default: String): String {
        val value = remoteConfig.getString(flag.key).takeIf { !it.isNullOrEmpty() } ?: default
        Log.d(TAG, "getString(${flag.key}) = $value")
        return value
    }

    override suspend fun fetchAndActivate(): Result<Unit> = runCatching {
        Log.d(TAG, "Starting fetchAndActivate...")
        val success = remoteConfig.fetchAndActivate().await()
        Log.d(TAG, "fetchAndActivate completed: success=$success, fetchTime=${remoteConfig.info.fetchTimeMillis}")
        val allFlags = FeatureFlag.entries.associateWith { getBoolean(it) }
        Log.d(TAG, "Current flag values after fetch: $allFlags")
    }
}