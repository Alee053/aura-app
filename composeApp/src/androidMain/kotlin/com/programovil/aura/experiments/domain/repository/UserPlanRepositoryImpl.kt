package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.data.repository.DataStoreUserPlanRepositoryImpl
import com.programovil.aura.shared.UserPlanManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun createUserPlanRepository(): UserPlanRepository {
    val userPlanManager: UserPlanManager = object : KoinComponent {
        val mgr: UserPlanManager = get()
    }.mgr
    val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = object : KoinComponent {
        val ds: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = get()
    }.ds
    return DataStoreUserPlanRepositoryImpl(dataStore, userPlanManager)
}
