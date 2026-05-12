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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
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
import com.programovil.aura.home.presentation.composable.PresetButton
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val minutes = uiState.timeRemainingSeconds / 60
    val seconds = uiState.timeRemainingSeconds % 60
    val timeText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.background,
                        Color.Black
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FOCUS",
                        style = AppTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = timeText,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresetButton(text = "15M", onClick = { viewModel.selectDuration(15) })
                Spacer(modifier = Modifier.width(12.dp))
                PresetButton(
                    text = "25M",
                    isSelected = uiState.selectedDurationMinutes == 25,
                    onClick = { viewModel.selectDuration(25) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                PresetButton(text = "45M", onClick = { viewModel.selectDuration(45) })
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(80.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "SESSIONS TODAY : ${uiState.sessionsToday}",
                style = AppTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
        }
    }
}