package com.programovil.aura.regionsync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.programovil.aura.regionsync.domain.model.RegionalDataItem

@Entity(tableName = "regional_data")
data class RegionDataEntity(
    @PrimaryKey val id: String,
    val regionId: String,
    val name: String,
    val value: String
) {
    fun toDomain(): RegionalDataItem {
        return RegionalDataItem(id, regionId, name, value)
    }

    companion object {
        fun fromDomain(regionalDataItem: RegionalDataItem): RegionDataEntity {
            return RegionDataEntity(
                id = regionalDataItem.id,
                regionId = regionalDataItem.regionId,
                name = regionalDataItem.name,
                value = regionalDataItem.value
            )
        }
    }
}
