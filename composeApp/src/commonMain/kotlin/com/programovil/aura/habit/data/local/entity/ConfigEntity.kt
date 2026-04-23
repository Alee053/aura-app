package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_configs")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)
