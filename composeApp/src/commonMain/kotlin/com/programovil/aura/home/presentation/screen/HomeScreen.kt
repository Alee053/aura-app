package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTodoClick: () -> Unit = {},
    onHabitClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
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
            .draggable(
                state = rememberDraggableState { delta ->
                    if (delta < -20) {
                        onTodoClick()
                    }
                },
                orientation = Orientation.Vertical
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { onTodoClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.dashboardData.incompleteTodos.toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = "TASKS TODAY",
                        style = AppTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Swipe up to see your tasks",
                style = AppTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                modifier = Modifier.clickable { onTodoClick() }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "SETTINGS",
                style = AppTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onSettingsClick() }
            )
        }
    }
}
