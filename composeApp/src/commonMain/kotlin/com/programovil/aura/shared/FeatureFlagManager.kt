package com.programovil.aura.shared

import com.programovil.aura.habit.data.local.ConfigDao
import com.programovil.aura.habit.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService,
    private val configDao: ConfigDao
) {
    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        // Sincronización inicial con caché local
        try {
            remoteConfigService.fetchAndActivate()
            val updatedFlags = FeatureFlag.entries.associate { flag ->
                val remoteValue = remoteConfigService.getBoolean(flag)
                // Guardar localmente en Room
                configDao.insertConfig(ConfigEntity(flag.key, remoteValue.toString()))
                flag to remoteValue
            }
            _flags.value = updatedFlags
        } catch (e: Exception) {
            // Si no hay internet, usar la última versión guardada
            val localFlags = FeatureFlag.entries.associate { flag ->
                val cached = configDao.getConfig(flag.key)
                val value = cached?.value?.toBoolean() ?: flag.defaultValue
                flag to value
            }
            _flags.value = localFlags
        }
    }

    suspend fun getFlagValue(flag: FeatureFlag): Boolean {
        return flags.value[flag] ?: configDao.getConfig(flag.key)?.value?.toBoolean() ?: flag.defaultValue
    }
}
