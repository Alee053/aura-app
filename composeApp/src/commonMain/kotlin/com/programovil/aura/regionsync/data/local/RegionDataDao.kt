package com.programovil.aura.regionsync.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegionDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<RegionDataEntity>)

    @Query("SELECT * FROM regional_data WHERE regionId = :regionId")
    fun getRegionalDataFlow(regionId: String): Flow<List<RegionDataEntity>>

    @Query("DELETE FROM regional_data WHERE regionId = :regionId")
    suspend fun deleteRegionalData(regionId: String)
}
