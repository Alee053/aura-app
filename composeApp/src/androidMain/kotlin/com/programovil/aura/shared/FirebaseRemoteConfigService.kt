package com.programovil.aura.shared

import android.util.Log
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

private const val TAG = "FirebaseRemoteConfig"

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        val defaults = FeatureFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        remoteConfig.setDefaultsAsync(defaults)
    }

    override suspend fun getBoolean(flag: FeatureFlag): Boolean {
        return remoteConfig.getBoolean(flag.key)
    }

    override suspend fun getString(flag: FeatureFlag, default: String): String {
        return remoteConfig.getString(flag.key).takeIf { it.isNotEmpty() } ?: default
    }

    override suspend fun fetchAndActivate(): Result<Unit> = runCatching {
        remoteConfig.fetchAndActivate().await()
    }

    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Remote config update received, activating...")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Remote config activated successfully")
                        onUpdate()
                    } else {
                        Log.e(TAG, "Failed to activate remote config", task.exception)
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG, "Remote config listener error: ${error.message}", error)
            }
        })
    }
}