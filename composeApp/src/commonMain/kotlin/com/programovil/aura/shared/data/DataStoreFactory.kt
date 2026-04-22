package com.programovil.aura.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(): DataStore<Preferences>

internal const val DATASTORE_FILE_NAME = "aura_preferences.preferences_pb"
