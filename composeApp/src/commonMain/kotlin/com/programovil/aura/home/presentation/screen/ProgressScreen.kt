package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.programovil.aura.home.presentation.composable.StatCard
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onBackClick: () -> Unit = {}
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Progress",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = AppTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "STREAK",
                    value = "${uiState.streak}d",
                    icon = "🔥",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "TOTAL",
                    value = uiState.totalCompleted.toString(),
                    icon = "✅",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "AVG",
                    value = uiState.activeDays.toString(),
                    icon = "📈",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Your Activity",
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Light
            )
            Text(
                text = "${uiState.totalCompleted} tasks · ${uiState.activeDays} active days",
                style = AppTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val days = (1..30).toList()
            val emptyPrefix = listOf(null, null, null)
            val allCells = emptyPrefix + days

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(260.dp)
            ) {
                items(allCells) { day ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (day == 21) Modifier.border(1.dp, AppTheme.colors.primary, CircleShape)
                                else Modifier
                            )
                            .background(
                                if (day != null) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Text(
                                text = day.toString(),
                                color = if (day == 21) AppTheme.colors.primary else Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Productivity",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Low", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        repeat(5) { i ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        AppTheme.colors.primary.copy(alpha = 0.1f + (i * 0.15f)),
                                        CircleShape
                                    )
                            )
                        }
                        Text("High", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}