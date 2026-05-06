package com.programovil.aura.regionsync.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Assuming a simple manual DI for now. In a real app, use Hilt/Koin.
import com.programovil.aura.regionsync.data.local.RegionalSyncDatabase
import com.programovil.aura.regionsync.data.remote.FirebaseRealtimeDatabaseDataSource
import com.programovil.aura.regionsync.data.remote.FirebaseRemoteConfigDataSource
import com.programovil.aura.regionsync.data.repository.RegionSyncRepositoryImpl
import com.programovil.aura.regionsync.domain.model.Region
import com.programovil.aura.regionsync.domain.model.RegionalDataItem
import com.programovil.aura.regionsync.domain.usecase.GetActiveRegionsUseCase
import com.programovil.aura.regionsync.domain.usecase.SyncRegionalDataUseCase

@Composable
fun RegionalSyncScreen(
    modifier: Modifier = Modifier,
    viewModel: RegionalSyncViewModel // In a real app, inject via Hilt/Koin
) {
    val activeRegions by viewModel.activeRegions.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val regionalData by viewModel.regionalData.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Regional Data Synchronization",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activeRegions.isEmpty()) {
            Text("Loading active regions or no regions configured.")
        } else {
            Text("Select Region:", style = MaterialTheme.typography.subtitle1)
            Row(modifier = Modifier.fillMaxWidth()) {
                activeRegions.forEach { region ->
                    Button(
                        onClick = { viewModel.selectRegion(region) },
                        enabled = !syncing,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(region.name)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedRegion?.let { region ->
            Text(
                text = "Data for ${region.name} Region",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (regionalData.isEmpty()) {
                Text("No data available for this region locally.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(regionalData) {
                        RegionalDataItemCard(it)
                    }
                }
            }
        } ?: run { Text("Please select a region to view its data.") }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.performSync() },
            enabled = !syncing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (syncing) {
                CircularProgressIndicator(color = MaterialTheme.colors.onPrimary)
            } else {
                Text("Sync All Regional Data")
            }
        }

        message?.let { 
            Text(
                text = it,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun RegionalDataItemCard(item: RegionalDataItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "ID: ${item.id}", style = MaterialTheme.typography.subtitle2)
            Text(text = "Name: ${item.name}", style = MaterialTheme.typography.body1)
            Text(text = "Value: ${item.value}", style = MaterialTheme.typography.body2)
        }
    }
}

// Placeholder for Room Database creation and DI. You'll need to use a real KMP Room setup.
expect fun getRegionalSyncDatabase(): RegionalSyncDatabase

@Composable
fun rememberRegionalSyncViewModel(): RegionalSyncViewModel {
    val database = getRegionalSyncDatabase()
    val regionDataDao = database.regionDataDao()
    val remoteConfigDataSource = FirebaseRemoteConfigDataSource()
    val realtimeDatabaseDataSource = FirebaseRealtimeDatabaseDataSource()
    val repository = RegionSyncRepositoryImpl(remoteConfigDataSource, realtimeDatabaseDataSource, regionDataDao)
    val getActiveRegionsUseCase = GetActiveRegionsUseCase(repository)
    val syncRegionalDataUseCase = SyncRegionalDataUseCase(repository)

    return RegionalSyncViewModel(getActiveRegionsUseCase, syncRegionalDataUseCase, repository)
}
