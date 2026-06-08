package com.programovil.aura.experiments.domain.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.programovil.aura.experiments.data.repository.DataStoreUserPlanRepositoryImpl
import com.programovil.aura.shared.RemoteConfigService
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun createUserPlanRepository(): UserPlanRepository {
    val dataStore: DataStore<Preferences> = object : KoinComponent {
        val ds: DataStore<Preferences> = get()
    }.ds
    val remoteConfigService: RemoteConfigService = object : KoinComponent {
        val rcs: RemoteConfigService = get()
    }.rcs
    return DataStoreUserPlanRepositoryImpl(dataStore, remoteConfigService)
}
