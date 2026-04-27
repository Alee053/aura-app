package com.programovil.aura.sync.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.sync.data.repository.SyncQueueRepository
import org.koin.compose.koinInject

@Composable
fun SyncStatusBar(
    modifier: Modifier = Modifier
) {
    val syncQueueRepo: SyncQueueRepository = koinInject()
    val pendingItems by syncQueueRepo.getPendingItems().collectAsState(initial = emptyList())
    val pendingCount = pendingItems.size

    if (pendingCount > 0) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PENDING SYNC: $pendingCount items",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
