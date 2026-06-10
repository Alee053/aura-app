package com.programovil.aura.shared

import android.util.Log
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

private const val TAG = "FirebaseRemoteConfig"

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig
    private var listenerRegistration: ConfigUpdateListenerRegistration? = null

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        val flagDefaults: Map<String, Any> = FeatureFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        val userPlanDefaults: Map<String, Any> = UserPlanFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        remoteConfig.setDefaultsAsync(flagDefaults + userPlanDefaults)
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return remoteConfig.getBoolean(key)
    }

    override suspend fun getString(key: String, default: String): String {
        return remoteConfig.getString(key).takeIf { it.isNotEmpty() } ?: default
    }

    override suspend fun fetchAndActivate(): Result<Unit> = runCatching {
        remoteConfig.fetchAndActivate().await()
    }

    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
        listenerRegistration?.remove()
        listenerRegistration = remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Remote config update received for keys: ${configUpdate.updatedKeys}, activating...")
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
                Log.e(TAG, "Remote config listener error [${error.code}]: ${error.message}", error)
            }
        })
        Log.d(TAG, "Real-time config listener registered and held")
    }
}
