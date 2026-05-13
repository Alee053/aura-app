# App Restructure & Design System Adoption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the Aura app to 4 clean tabs (Home, Todos, Habits, Settings), remove broken screens (Focus, Progress, Tasks), fully adopt the design system, and fix Settings persistence.

**Architecture:** Clean Architecture with Koin DI. Extract `home/` monolith into proper `home/` (dashboard) and `settings/` (preferences) modules. Remove dead routes. Adopt design system tokens across all screens.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, DataStore, Room KMP, Firestore

---

## Task 1: Expand Design System — Color Tokens

**Files:**
- Modify: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Color.kt`

- [ ] **Step 1: Add `error` and `textSecondary` to `AppColors` and all palettes**

Replace `Color.kt` with:

```kotlin
package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val accent: Color,
    val isLight: Boolean,
    val error: Color = Color(0xFFCF6679),
    val textSecondary: Color = textPrimary.copy(alpha = 0.6f)
)

val PurplePalette = AppColors(
    primary = Color(0xFF6C5CE7),
    background = Color(0xFF140D2F),
    surface = Color(0xFF1F1240),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFBB86EC),
    isLight = false
)

val RedPalette = AppColors(
    primary = Color(0xFFB33939),
    background = Color(0xFF2C0B0B),
    surface = Color(0xFF3F1313),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFEA8685),
    isLight = false
)

val GreenPalette = AppColors(
    primary = Color(0xFF16A085),
    background = Color(0xFF0A2B23),
    surface = Color(0xFF104639),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF86ECDB),
    isLight = false
)

val DarkPalette = AppColors(
    primary = Color(0xFFBB86EC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF03DAC6),
    isLight = false
)

val HighContrastPalette = AppColors(
    primary = Color(0xFFFFFF00),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    textPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFFFFF00),
    isLight = false
)
```

- [ ] **Step 2: Commit**

```bash
git add designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Color.kt
git commit -m "feat(design-system): add error and textSecondary color tokens to AppColors"
```

---

## Task 2: Expand Design System — Typography Tokens

**Files:**
- Modify: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Type.kt`

- [ ] **Step 1: Add `displayLarge`, `titleMedium`, `bodyLarge`, and `labelSmall` to `AppTypography`**

Replace `Type.kt` with:

```kotlin
package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val displayLarge: TextStyle,
    val headlineLarge: TextStyle,
    val headlineSmall: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

val DefaultTypography = AppTypography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 80.sp,
        lineHeight = 88.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 2: Commit**

```bash
git add designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Type.kt
git commit -m "feat(design-system): add displayLarge, titleMedium, bodyLarge, labelSmall typography tokens"
```

---

## Task 3: Add Sounds/Vibration Persistence to NotificationPreferences

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/data/NotificationPreferences.kt`

- [ ] **Step 1: Add `soundsEnabled` and `vibrationEnabled` keys and flows**

Replace `NotificationPreferences.kt` with:

```kotlin
package com.programovil.aura.notification.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val dailySummaryEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] ?: false }

    val notificationHour: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    val soundsEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.SOUNDS_ENABLED] ?: false }

    val vibrationEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.VIBRATION_ENABLED] ?: true }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SOUNDS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.VIBRATION_ENABLED] = enabled }
    }

    fun testNotification() {
        // no-op in common
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/data/NotificationPreferences.kt
git commit -m "feat(notifications): add sounds and vibration preference persistence"
```

---

## Task 4: Create Settings Module — Extract from Home

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/di/SettingsModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/domain/repository/ThemeRepository.kt`
- Move: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepositoryImpl.kt` → `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/data/ThemeRepositoryImpl.kt`
- Move: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/ThemeCard.kt` → `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/composable/ThemeCard.kt`
- Move: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PreferenceItem.kt` → `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/composable/PreferenceItem.kt`
- Move: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/SettingsViewModel.kt` → `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/viewmodel/SettingsViewModel.kt`
- Move: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/SettingsScreen.kt` → `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepository.kt` (old location)

- [ ] **Step 1: Create settings directory structure**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/di
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/domain/repository
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/data
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/composable
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/viewmodel
```

- [ ] **Step 2: Create `ThemeRepository` interface in domain**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/domain/repository/ThemeRepository.kt`:

```kotlin
package com.programovil.aura.settings.domain.repository

import com.programovil.aura.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 3: Move and update `ThemeRepositoryImpl`**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/data/ThemeRepositoryImpl.kt` with the same content but updated package and import:

```kotlin
package com.programovil.aura.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    override fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data.map { prefs ->
            val name = prefs[themeModeKey] ?: ThemeMode.PURPLE.name
            ThemeMode.valueOf(name)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
```

- [ ] **Step 4: Move and update `SettingsViewModel`**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/viewmodel/SettingsViewModel.kt` with updated imports, plus fix sounds/vibration to be persisted through `NotificationPreferences`:

```kotlin
package com.programovil.aura.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import com.programovil.aura.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.PURPLE,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val soundsEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true
)

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.getThemeMode().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.dailySummaryEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
                if (enabled) {
                    val state = _uiState.value
                    notificationScheduler.scheduleDailySummary(state.notificationHour, state.notificationMinute)
                } else {
                    notificationScheduler.cancelDailySummary()
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.soundsEnabled.collect { enabled ->
                _uiState.update { it.copy(soundsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.vibrationEnabled.collect { enabled ->
                _uiState.update { it.copy(vibrationEnabled = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setDailySummaryEnabled(enabled)
        }
    }

    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setSoundsEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setVibrationEnabled(enabled)
        }
    }
}
```

- [ ] **Step 5: Create `SettingsModule.kt`**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/di/SettingsModule.kt`:

```kotlin
package com.programovil.aura.settings.di

import com.programovil.aura.settings.data.ThemeRepositoryImpl
import com.programovil.aura.settings.domain.repository.ThemeRepository
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
    viewModelOf(::SettingsViewModel)
}
```

- [ ] **Step 6: Move `ThemeCard.kt` to settings package with design system tokens**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/composable/ThemeCard.kt`:

```kotlin
package com.programovil.aura.settings.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun ThemeCard(
    name: String,
    colors: List<androidx.compose.ui.graphics.Color>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppTheme.colors.textPrimary.copy(alpha = 0.1f)),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(colors),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            Switch(
                checked = isSelected,
                onCheckedChange = { if (it) onSelect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.textPrimary,
                    checkedTrackColor = AppTheme.colors.primary,
                    uncheckedThumbColor = AppTheme.colors.textPrimary,
                    uncheckedTrackColor = AppTheme.colors.textPrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
```

- [ ] **Step 7: Move `PreferenceItem.kt` to settings package with design system tokens**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/composable/PreferenceItem.kt`:

```kotlin
package com.programovil.aura.settings.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun PreferenceItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppTheme.colors.textPrimary.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.textPrimary,
                    checkedTrackColor = AppTheme.colors.primary,
                    uncheckedThumbColor = AppTheme.colors.textPrimary,
                    uncheckedTrackColor = AppTheme.colors.textPrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
```

- [ ] **Step 8: Create new `SettingsScreen.kt` in settings package with all 5 themes and design system tokens**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt`:

```kotlin
package com.programovil.aura.settings.presentation.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.settings.presentation.composable.PreferenceItem
import com.programovil.aura.settings.presentation.composable.ThemeCard
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Settings",
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Themes",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeCard(
                name = "Arctic Night",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF6C5CE7)),
                isSelected = currentThemeMode == ThemeMode.PURPLE,
                onSelect = { onThemeChange(ThemeMode.PURPLE) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Forest Dawn",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF0A2B23), androidx.compose.ui.graphics.Color(0xFF16A085)),
                isSelected = currentThemeMode == ThemeMode.GREEN,
                onSelect = { onThemeChange(ThemeMode.GREEN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Silent Desert",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF2C0B0B), androidx.compose.ui.graphics.Color(0xFFB33939)),
                isSelected = currentThemeMode == ThemeMode.RED,
                onSelect = { onThemeChange(ThemeMode.RED) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Midnight",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF121212), androidx.compose.ui.graphics.Color(0xFFBB86EC)),
                isSelected = currentThemeMode == ThemeMode.DARK,
                onSelect = { onThemeChange(ThemeMode.DARK) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "High Contrast",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF000000), androidx.compose.ui.graphics.Color(0xFFFFFF00)),
                isSelected = currentThemeMode == ThemeMode.HIGH_CONTRAST,
                onSelect = { onThemeChange(ThemeMode.HIGH_CONTRAST) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Preferences",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary,
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
                checked = uiState.soundsEnabled,
                onCheckedChange = { viewModel.setSoundsEnabled(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceItem(
                title = "Vibration",
                subtitle = "Haptic feedback",
                checked = uiState.vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) }
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
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Version 1.0.0",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Made with love for mindful productivity",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
```

- [ ] **Step 9: Delete old files from home/ that are now in settings/**

```bash
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepository.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepositoryImpl.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/SettingsViewModel.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/SettingsScreen.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/ThemeCard.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PreferenceItem.kt
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat(settings): extract settings module from home, add sound/vibration persistence, expose all 5 themes"
```

---

## Task 5: Create Home Dashboard Domain Layer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/domain/model/DashboardData.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/domain/usecase/GetDashboardDataUseCase.kt`

- [ ] **Step 1: Create `DashboardData` model**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/home/domain/model/DashboardData.kt`:

```kotlin
package com.programovil.aura.home.domain.model

data class DashboardData(
    val incompleteTodos: Int = 0,
    val completedHabitsToday: Int = 0,
    val totalHabitsToday: Int = 0,
    val currentStreak: Int = 0
)
```

- [ ] **Step 2: Create `GetDashboardDataUseCase`**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/home/domain/usecase/GetDashboardDataUseCase.kt`:

```kotlin
package com.programovil.aura.home.domain.usecase

import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDashboardDataUseCase(
    private val getTodosUseCase: GetTodosUseCase,
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase
) {
    operator fun invoke(): Flow<Result<DashboardData>> {
        return combine(getTodosUseCase(), getHabitsGroupedByDayUseCase()) { todosResult, habitsResult ->
            val incompleteTodos = todosResult.getOrNull()?.count { !it.isCompleted } ?: 0
            val todayHabits = habitsResult.getOrNull()?.get(DaySection.TODAY) ?: emptyList()
            val completedHabitsToday = todayHabits.count { it.isDone }
            val currentStreak = todayHabits.maxOfOrNull { it.streak } ?: 0

            Result.success(
                DashboardData(
                    incompleteTodos = incompleteTodos,
                    completedHabitsToday = completedHabitsToday,
                    totalHabitsToday = todayHabits.size,
                    currentStreak = currentStreak
                )
            )
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/domain/
git commit -m "feat(home): add DashboardData model and GetDashboardDataUseCase"
```

---

## Task 6: Update HomeViewModel to Use GetDashboardDataUseCase

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/HomeViewModel.kt`

- [ ] **Step 1: Rewrite HomeViewModel to use use case**

Replace `HomeViewModel.kt` with:

```kotlin
package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.home.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val dashboardData: DashboardData = DashboardData(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDashboardDataUseCase().collect { result ->
                result.onSuccess { data ->
                    _uiState.update { it.copy(dashboardData = data, isLoading = false) }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/HomeViewModel.kt
git commit -m "refactor(home): HomeViewModel uses GetDashboardDataUseCase instead of direct repo"
```

---

## Task 7: Create DashboardCard Composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/DashboardCard.kt`

- [ ] **Step 1: Create `DashboardCard` composable**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/DashboardCard.kt`:

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun DashboardCard(
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppTheme.colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = AppTheme.typography.displayLarge,
                color = AppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/DashboardCard.kt
git commit -m "feat(home): add DashboardCard composable with design system tokens"
```

---

## Task 8: Delete Removed Screens and Composables

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/TasksScreen.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/FocusScreen.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/ProgressScreen.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/FocusViewModel.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/ProgressViewModel.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/TaskCard.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/CategoryChip.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PresetButton.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/StatCard.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/HomeButton.kt`

- [ ] **Step 1: Delete all removed files**

```bash
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/TasksScreen.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/FocusScreen.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/ProgressScreen.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/FocusViewModel.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/ProgressViewModel.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/TaskCard.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/CategoryChip.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PresetButton.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/StatCard.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/HomeButton.kt
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "chore: remove Focus, Progress, Tasks screens and unused composables"
```

---

## Task 9: Simplify NavRoute

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`

- [ ] **Step 1: Rewrite NavRoute with only 4 routes**

Replace `NavRoute.kt` with:

```kotlin
package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object Todo : NavRoute()

    @Serializable
    data object Habit : NavRoute()

    @Serializable
    data object Settings : NavRoute()
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt
git commit -m "refactor(nav): simplify NavRoute to Home, Todo, Habit, Settings"
```

---

## Task 10: Rewrite AppNavHost

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`

- [ ] **Step 1: Rewrite AppNavHost with 4 routes only**

Replace `AppNavHost.kt` with:

```kotlin
package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                onTodoClick = { navController.navigate(NavRoute.Todo) },
                onHabitClick = { navController.navigate(NavRoute.Habit) },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }
        composable<NavRoute.Todo> {
            TodoScreen(
                viewModel = todoViewModel
            )
        }
        composable<NavRoute.Habit> {
            HabitScreen()
        }
        composable<NavRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt
git commit -m "refactor(nav): simplify AppNavHost to 4 routes"
```

---

## Task 11: Rewrite HomeScreen as Dashboard

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`

- [ ] **Step 1: Rewrite HomeScreen as dashboard with design system tokens**

Replace `HomeScreen.kt` with:

```kotlin
package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
        // Top bar with title and settings icon
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

        // Tasks dashboard card
        DashboardCard(
            title = "TASKS TODAY",
            value = if (uiState.isLoading) "..." else uiState.dashboardData.incompleteTodos.toString(),
            subtitle = "${uiState.dashboardData.incompleteTodos} remaining",
            onClick = onTodoClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Habits dashboard card
        DashboardCard(
            title = "HABIT STREAK",
            value = if (uiState.isLoading) "..." else "${uiState.dashboardData.currentStreak}",
            subtitle = "${uiState.dashboardData.completedHabitsToday}/${uiState.dashboardData.totalHabitsToday} done today",
            onClick = onHabitClick
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt
git commit -m "feat(home): rewrite HomeScreen as dashboard with design system tokens"
```

---

## Task 12: Rewrite App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Rewrite App.kt with always-visible bottom nav, Settings tab, and design system tokens**

Replace `App.kt` with:

```kotlin
package com.programovil.aura

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.DsTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.navigation.NavRoute
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    onSignInClick: () -> Unit = {}
) {
    val settingsViewModel = koinViewModel<com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel>()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val currentThemeMode = settingsState.themeMode

    DsTheme(mode = currentThemeMode) {
        val authViewModel: AuthViewModel = koinViewModel()
        val authState by authViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthViewModel.AuthState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primary)
                }
            }
            is AuthViewModel.AuthState.SignedIn -> {
                AuthenticatedApp(
                    currentThemeMode = currentThemeMode,
                    onThemeChange = { settingsViewModel.setThemeMode(it) }
                )
            }
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.Error -> {
                SignInScreen(
                    errorMessage = if (state is AuthViewModel.AuthState.Error) state.message else null,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}

@Composable
fun AuthenticatedApp(
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinViewModel()
    val featureFlagManager: FeatureFlagManager = koinInject()
    val featureFlags by featureFlagManager.flags.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(Unit) {
        featureFlagManager.initialize()
    }

    val showTodos by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true)
    }
    val showHabits by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true)
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.primary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Home>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                if (showTodos) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Checklist, contentDescription = "Todos") },
                        label = { Text("Todos") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Todo>() } == true,
                        onClick = {
                            navController.navigate(NavRoute.Todo) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                if (showHabits) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Habits") },
                        label = { Text("Habits") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Habit>() } == true,
                        onClick = {
                            navController.navigate(NavRoute.Habit) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Settings>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Settings) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AppNavHost(
                navController = navController,
                todoViewModel = todoViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange
            )
        }
    }
}

@Composable
fun SignInScreen(
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            "Aura",
            style = AppTheme.typography.headlineLarge,
            color = AppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign in to sync your todos",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        com.programovil.aura.designsystem.components.button.PrimaryButton(
            text = "Sign in with Google",
            onClick = onSignInClick
        )
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                color = AppTheme.colors.error,
                style = AppTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat(app): rewrite App with always-visible bottom nav, Settings tab, design system tokens"
```

---

## Task 13: Update DI Modules

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/di/HomeModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

- [ ] **Step 1: Trim `HomeModule.kt`**

Replace `HomeModule.kt` with:

```kotlin
package com.programovil.aura.home.di

import com.programovil.aura.home.domain.usecase.GetDashboardDataUseCase
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeModule = module {
    factoryOf(::GetDashboardDataUseCase)
    viewModelOf(::HomeViewModel)
}
```

- [ ] **Step 2: Add `settingsModule` to `InitKoin.kt`**

Replace `InitKoin.kt` with:

```kotlin
package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.habit.di.habitModule
import com.programovil.aura.home.di.homeModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.settings.di.settingsModule
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.todo.di.todoModule
import org.koin.dsl.module

fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/di/HomeModule.kt composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "refactor(di): trim HomeModule, add settingsModule to InitKoin"
```

---

## Task 14: Update TodoScreen with Design System Tokens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Update TodoScreen to use `BasicInput`, `AppTheme.colors`, and new typography tokens. Remove `onSignOut` parameter.**

Replace `TodoScreen.kt` with:

```kotlin
package com.programovil.aura.todo.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.presentation.composable.TodoItem
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.add_todo
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.empty_todos
import aura_app.composeapp.generated.resources.new_todo_hint
import aura_app.composeapp.generated.resources.ok
import aura_app.composeapp.generated.resources.select_due_date
import aura_app.composeapp.generated.resources.todos_title
import aura_app.composeapp.generated.resources.due_date_label
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel
) {
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var newTodoTitle by remember { mutableStateOf("") }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.todos_title),
                        style = AppTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (newTodoTitle.isNotBlank()) {
                        viewModel.addTodo(newTodoTitle, selectedDueDate)
                        newTodoTitle = ""
                        selectedDueDate = null
                    }
                },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_todo),
                    tint = AppTheme.colors.textPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            BasicInput(
                value = newTodoTitle,
                onValueChange = { newTodoTitle = it },
                label = stringResource(Res.string.new_todo_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(Res.string.select_due_date),
                            tint = if (selectedDueDate != null) AppTheme.colors.primary else AppTheme.colors.textSecondary
                        )
                    }
                }
            )

            selectedDueDate?.let { millis ->
                val date = Instant.fromEpochMilliseconds(millis)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                Text(
                    text = stringResource(Res.string.due_date_label, date.toString()),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                todos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(Res.string.empty_todos),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onDelete = { viewModel.deleteTodo(todo.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Note:** The `BasicInput` composable from the design system doesn't support `trailingIcon`. We'll need to add an optional `trailingIcon` parameter to `BasicInput` as part of this task. If the composable doesn't accept that parameter, just use `OutlinedTextField` directly with themed colors for the todo input field, keeping the `BasicInput` for the simple input case. Since `BasicInput` doesn't support `trailingIcon`, use `OutlinedTextField` with themed colors for the todo input.

Actually, let me re-check: the `BasicInput` in our design system does NOT have a `trailingIcon` parameter. So for the todo input with the calendar icon, we'll need to use a themed `OutlinedTextField` directly.

- [ ] **Step 2: Update Step 1 — use `OutlinedTextField` with themed colors for the todo input (since `BasicInput` doesn't support `trailingIcon`)**

In the `TodoScreen`, replace the `BasicInput` call with a themed `OutlinedTextField`:

```kotlin
            OutlinedTextField(
                value = newTodoTitle,
                onValueChange = { newTodoTitle = it },
                label = { Text(stringResource(Res.string.new_todo_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                textStyle = AppTheme.typography.bodyMedium.copy(
                    color = AppTheme.colors.textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    disabledTextColor = AppTheme.colors.textPrimary.copy(alpha = 0.38f),
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                    disabledBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.12f),
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(Res.string.select_due_date),
                            tint = if (selectedDueDate != null) AppTheme.colors.primary else AppTheme.colors.textSecondary
                        )
                    }
                }
            )
```

This requires adding these imports:
```kotlin
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
```

And removing `BasicInput` import.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt
git commit -m "refactor(todo): update TodoScreen with design system tokens, remove onSignOut"
```

---

## Task 15: Update TodoItem with Design System Tokens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`

- [ ] **Step 1: Update TodoItem colors to use `AppTheme.colors`**

Replace `TodoItem.kt` with:

```kotlin
package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.delete
import aura_app.composeapp.generated.resources.due_label
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppTheme.colors.primary,
                    uncheckedColor = AppTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                todo.dueDate?.let { millis ->
                    val date = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Text(
                        text = stringResource(Res.string.due_label, date.toString()),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Text(
                    stringResource(Res.string.delete),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.primary
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt
git commit -m "refactor(todo): update TodoItem with design system color tokens"
```

---

## Task 16: Update HabitScreen with Design System Tokens

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

- [ ] **Step 1: Update HabitScreen colors and typography to use design system tokens. Remove `onSignOut` parameter.**

Update `HabitScreen.kt` — replace the `@Composable fun HabitScreen` function with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.habits_title))
                        Text(
                            text = today.toString(),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_habit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Today section
                if (uiState.todayHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.today),
                            subtitle = today.toString()
                        )
                    }
                    items(uiState.todayHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            }
                        )
                    }
                }

                // Tomorrow section
                if (uiState.tomorrowHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.tomorrow),
                            subtitle = tomorrow.toString()
                        )
                    }
                    items(uiState.tomorrowHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            }
                        )
                    }
                }

                // This Week section
                if (uiState.thisWeekHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.this_week),
                            subtitle = null
                        )
                    }
                    items(uiState.thisWeekHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            }
                        )
                    }
                }

                // Empty state
                if (uiState.todayHabits.isEmpty() &&
                    uiState.tomorrowHabits.isEmpty() &&
                    uiState.thisWeekHabits.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.empty_habits),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, recurrenceType, daysOfWeek, color ->
                viewModel.onEvent(HabitEvent.AddHabit(name, recurrenceType, daysOfWeek, color))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HabitSectionHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
```

Make sure to also add the `TopAppBarDefaults` import:
```kotlin
import androidx.compose.material3.TopAppBarDefaults
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt
git commit -m "refactor(habit): update HabitScreen with design system tokens, remove onSignOut"
```

---

## Task 17: Update AddHabitDialog with Design System Components

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`

- [ ] **Step 1: Replace `OutlinedTextField` with `BasicInput` and `Button` with `PrimaryButton` in AddHabitDialog**

Replace `AddHabitDialog.kt` with:

```kotlin
package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.color_label
import aura_app.composeapp.generated.resources.daily
import aura_app.composeapp.generated.resources.habit_name
import aura_app.composeapp.generated.resources.new_habit
import aura_app.composeapp.generated.resources.repeat_on
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.weekly
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import org.jetbrains.compose.resources.stringResource

private val colorPalette = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"
)

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, recurrenceType: RecurrenceType, daysOfWeek: List<Int>, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableIntStateOf(0) }
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.new_habit),
                    style = AppTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.habit_name),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.daily),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                    Switch(
                        checked = !isDaily,
                        onCheckedChange = { isDaily = !it }
                    )
                    Text(
                        stringResource(Res.string.weekly),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                }

                if (!isDaily) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.repeat_on),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayBit = 1 shl index
                            val isSelected = (selectedDays and dayBit) != 0
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AppTheme.colors.primary
                                        else AppTheme.colors.surface.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        selectedDays = selectedDays xor dayBit
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = AppTheme.typography.labelLarge,
                                    color = if (isSelected) AppTheme.colors.textPrimary
                                    else AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.color_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.border(2.dp, AppTheme.colors.textPrimary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onDismiss,
                        enabled = true
                    )
                    Spacer(modifier = Modifier.weight(1f).size(0.dp))
                    PrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            if (name.isNotBlank()) {
                                val daysOfWeek = if (isDaily) {
                                    emptyList()
                                } else {
                                    (0..6).filter { (selectedDays and (1 shl it)) != 0 }.map { it + 1 }
                                }
                                onSave(name, if (isDaily) RecurrenceType.DAILY else RecurrenceType.WEEKLY, daysOfWeek, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt
git commit -m "refactor(habit): update AddHabitDialog with PrimaryButton, BasicInput, design system tokens"
```

---

## Task 18: Clean Up Empty Directories and Verify Build

**Files:**
- Remove empty directories from deleted files
- Verify build compiles

- [ ] **Step 1: Remove empty `home/data/` directory (ThemeRepository files moved to settings)**

```bash
rmdir composeApp/src/commonMain/kotlin/com/programovil/aura/home/data 2>/dev/null || true
```

- [ ] **Step 2: Verify the build compiles**

```bash
./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -30
```

If there are compilation errors, fix them. Common issues:
- Missing imports from moved packages
- References to deleted classes (TasksScreen, FocusScreen, etc.)
- References to `home.data.ThemeRepository` — should be `settings.domain.repository.ThemeRepository`
- `SettingsViewModel` imports — should be `settings.presentation.viewmodel.SettingsViewModel`
- `onSignOut` parameter removed from `TodoScreen` and `HabitScreen`
- `BasicInput` doesn't support `trailingIcon` — use `OutlinedTextField` with themed colors for the todo input

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve compilation errors after restructure"
```

---

## Task 19: Final Design System Audit

**Files:**
- Search all remaining files for `Color.White` hardcoded usage and `MaterialTheme.colorScheme` references

- [ ] **Step 1: Search for remaining hardcoded `Color.White` in composeApp source**

```bash
grep -rn "Color\.White" composeApp/src/commonMain/kotlin/ designsystem/src/commonMain/kotlin/
```

Any remaining `Color.White` usages in active files should be replaced with `AppTheme.colors.textPrimary` or `AppTheme.colors.textSecondary` as appropriate.

- [ ] **Step 2: Search for remaining `MaterialTheme.colorScheme` usages**

```bash
grep -rn "MaterialTheme\.colorScheme" composeApp/src/commonMain/kotlin/
```

Replace any remaining `MaterialTheme.colorScheme` references with `AppTheme.colors` equivalents.

- [ ] **Step 3: Search for remaining hardcoded `fontSize` that should use design system typography**

```bash
grep -rn "fontSize = " composeApp/src/commonMain/kotlin/ | grep -v "sp)" | head -5
grep -rn "fontSize = [0-9]" composeApp/src/commonMain/kotlin/
```

Replace with appropriate `AppTheme.typography` styles where possible.

- [ ] **Step 4: Commit fixes**

```bash
git add -A
git commit -m "style: replace remaining hardcoded colors and font sizes with design system tokens"
```

---

## Task 20: Verify Build and Run Tests

- [ ] **Step 1: Run full build**

```bash
./gradlew :composeApp:compileKotlinDesktop
```

- [ ] **Step 2: Run unit tests if they exist**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30
```

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: final build fixes"
```

---

## Summary of All Changes

| Task | What |
|------|------|
| 1 | Expand `AppColors` with `error` and `textSecondary` tokens |
| 2 | Expand `AppTypography` with `displayLarge`, `titleMedium`, `bodyLarge`, `labelSmall` |
| 3 | Add `soundsEnabled`/`vibrationEnabled` persistence to `NotificationPreferences` |
| 4 | Extract `settings/` module from `home/`, fix Settings to persist all prefs, expose 5 themes |
| 5 | Create `DashboardData` model and `GetDashboardDataUseCase` |
| 6 | Rewrite `HomeViewModel` to use `GetDashboardDataUseCase` |
| 7 | Create `DashboardCard` composable with design system tokens |
| 8 | Delete FocusScreen, ProgressScreen, TasksScreen, and dead composables |
| 9 | Simplify `NavRoute` to Home/Todo/Habit/Settings |
| 10 | Rewrite `AppNavHost` with 4 routes only |
| 11 | Rewrite `HomeScreen` as dashboard |
| 12 | Rewrite `App.kt` with always-visible bottom nav, Settings tab, design system |
| 13 | Update DI modules (trim HomeModule, add SettingsModule, update InitKoin) |
| 14 | Update `TodoScreen` with design system tokens, remove `onSignOut` |
| 15 | Update `TodoItem` with design system color tokens |
| 16 | Update `HabitScreen` with design system tokens, remove `onSignOut` |
| 17 | Update `AddHabitDialog` with `PrimaryButton`, `BasicInput`, design system tokens |
| 18 | Clean up empty dirs, verify build |
| 19 | Audit remaining hardcoded colors/fontSizes |
| 20 | Final build + tests verification |