package com.programovil.aura.regionsync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.regionsync.data.local.RegionalSyncDatabase
import com.programovil.aura.regionsync.data.remote.FirebaseRealtimeDatabaseDataSource
import com.programovil.aura.regionsync.data.remote.FirebaseRemoteConfigDataSource
import com.programovil.aura.regionsync.data.repository.RegionSyncRepositoryImpl
import com.programovil.aura.regionsync.domain.usecase.SyncRegionalDataUseCase

// This will be in androidMain because WorkManager is Android-specific.
class RegionalSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Manual Dependency Injection for the worker. In a real app, consider Hilt/Koin workers.
            val database = Room.databaseBuilder<RegionalSyncDatabase>(
                context = applicationContext,
                name = "regional_sync_database.db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
            val regionDataDao = database.regionDataDao()
            val remoteConfigDataSource = FirebaseRemoteConfigDataSource()
            val realtimeDatabaseDataSource = FirebaseRealtimeDatabaseDataSource()
            val repository = RegionSyncRepositoryImpl(remoteConfigDataSource, realtimeDatabaseDataSource, regionDataDao)
            val syncRegionalDataUseCase = SyncRegionalDataUseCase(repository)

            syncRegionalDataUseCase()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
