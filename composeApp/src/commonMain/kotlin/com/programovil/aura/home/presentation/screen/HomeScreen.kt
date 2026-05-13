package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.home.presentation.composable.DashboardCard
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTodoClick: () -> Unit = {},
    onHabitClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.background,
                        AppTheme.colors.surface
                    )
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AURA",
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.textPrimary
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AppTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        DashboardCard(
            title = "TASKS TODAY",
            value = if (uiState.isLoading) "..." else uiState.dashboardData.incompleteTodos.toString(),
            subtitle = "${uiState.dashboardData.incompleteTodos} remaining",
            onClick = onTodoClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        DashboardCard(
            title = "HABIT STREAK",
            value = if (uiState.isLoading) "..." else "${uiState.dashboardData.currentStreak}",
            subtitle = "${uiState.dashboardData.completedHabitsToday}/${uiState.dashboardData.totalHabitsToday} done today",
            onClick = onHabitClick
        )
    }
}
