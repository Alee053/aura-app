package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.home.presentation.composable.PreferenceItem
import com.programovil.aura.home.presentation.composable.ThemeCard
import com.programovil.aura.home.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var soundsEnabled by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(true) }

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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
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
                    text = "Settings",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = AppTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Themes",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeCard(
                name = "Arctic Night",
                colors = listOf(Color(0xFF1A237E), Color(0xFF6C5CE7)),
                isSelected = currentThemeMode == ThemeMode.PURPLE,
                onSelect = { onThemeChange(ThemeMode.PURPLE) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Forest Dawn",
                colors = listOf(Color(0xFF0A2B23), Color(0xFF16A085)),
                isSelected = currentThemeMode == ThemeMode.GREEN,
                onSelect = { onThemeChange(ThemeMode.GREEN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Silent Desert",
                colors = listOf(Color(0xFF2C0B0B), Color(0xFFB33939)),
                isSelected = currentThemeMode == ThemeMode.RED,
                onSelect = { onThemeChange(ThemeMode.RED) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Preferences",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PreferenceItem(
                title = "Notifications",
                subtitle = "Receive daily task reminders",
                checked = uiState.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceItem(
                title = "Sounds",
                subtitle = "Subtle sound effects",
                checked = soundsEnabled,
                onCheckedChange = { soundsEnabled = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceItem(
                title = "Vibration",
                subtitle = "Haptic feedback",
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AURA",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Version 1.0.0",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Made with love for mindful productivity",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}
