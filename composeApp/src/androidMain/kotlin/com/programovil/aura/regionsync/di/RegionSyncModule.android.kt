package com.programovil.aura.regionsync.di

import androidx.room.Room
import com.programovil.aura.regionsync.data.local.RegionalSyncDatabase
import com.programovil.aura.regionsync.data.remote.FirebaseRealtimeDatabaseDataSource
import com.programovil.aura.regionsync.data.remote.FirebaseRemoteConfigDataSource
import com.programovil.aura.regionsync.data.repository.RegionSyncRepositoryImpl
import com.programovil.aura.regionsync.domain.repository.RegionSyncRepository
import com.programovil.aura.regionsync.domain.usecase.GetActiveRegionsUseCase
import com.programovil.aura.regionsync.domain.usecase.SyncRegionalDataUseCase
import com.programovil.aura.regionsync.presentation.RegionSyncViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val regionSyncModule = module {
    single<RegionalSyncDatabase> {
        Room.databaseBuilder<RegionalSyncDatabase>(
            context = androidContext(),
            name = "regional_sync_database.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    single { get<RegionalSyncDatabase>().regionDataDao() }

    singleOf(::FirebaseRemoteConfigDataSource)
    singleOf(::FirebaseRealtimeDatabaseDataSource)

    singleOf(::RegionSyncRepositoryImpl) bind RegionSyncRepository::class

    factoryOf(::GetActiveRegionsUseCase)
    factoryOf(::SyncRegionalDataUseCase)

    viewModelOf(::RegionSyncViewModel)
}
