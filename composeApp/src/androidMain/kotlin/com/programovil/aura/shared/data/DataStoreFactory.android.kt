package com.programovil.aura.shared.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_preferences"
)

actual fun createDataStore(): DataStore<Preferences> {
    val context: Context = object : KoinComponent {
        val ctx: Context = get()
    }.ctx
    return context.dataStore
}
