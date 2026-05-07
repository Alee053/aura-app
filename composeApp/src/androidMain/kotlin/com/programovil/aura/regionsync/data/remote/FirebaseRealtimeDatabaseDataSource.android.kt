

























































package com.programovil.aura.regionsync.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.programovil.aura.regionsync.domain.model.RegionalDataItem
import kotlinx.coroutines.tasks.await

actual class FirebaseRealtimeDatabaseDataSource {
    private val database = FirebaseDatabase.getInstance()

    actual suspend fun fetchRegionalData(regionId: String): List<RegionalDataItem> {
        val ref = database.getReference("regions/$regionId/data")
        val snapshot = ref.get().await()
        val data = mutableListOf<RegionalDataItem>()
        snapshot.children.forEach { child ->
            val id = child.child("id").getValue(String::class.java) ?: ""
            val name = child.child("name").getValue(String::class.java) ?: ""
            val value = child.child("value").getValue(String::class.java) ?: ""
            data.add(RegionalDataItem(id, regionId, name, value))
        }
        return data
    }
}
