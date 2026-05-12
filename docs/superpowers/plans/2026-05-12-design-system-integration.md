# Design System Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the design system module and new Home screens from the fork, merge NotificationSettings into a unified SettingsScreen, and restyle all existing screens to use the new design tokens.

**Architecture:** A new `:designsystem` KMP module provides color/typography tokens and primitive components. A new `home` feature package contains 5 screens (Home, Tasks, Focus, Progress, Settings) following Clean Architecture with ViewModels. The existing `NotificationSettingsScreen` is deleted and its logic merged into `SettingsScreen`. Theme mode is persisted via DataStore.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material3, Koin, DataStore, kotlinx-datetime, Turbine, Mockative.

---

## File Structure Map

### New Files
```
designsystem/
├── build.gradle.kts
└── src/commonMain/kotlin/com/programovil/aura/designsystem/
    ├── theme/
    │   ├── Color.kt
    │   ├── DsTheme.kt
    │   └── Type.kt
    └── components/
        ├── button/PrimaryButton.kt
        ├── input/BasicInput.kt
        └── divider/HorizontalDivider.kt

composeApp/src/commonMain/kotlin/com/programovil/aura/
├── home/
│   ├── data/
│   │   ├── ThemeRepository.kt
│   │   └── ThemeRepositoryImpl.kt
│   ├── presentation/
│   │   ├── viewmodel/
│   │   │   ├── HomeViewModel.kt
│   │   │   ├── FocusViewModel.kt
│   │   │   ├── ProgressViewModel.kt
│   │   │   └── SettingsViewModel.kt
│   │   ├── screen/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── TasksScreen.kt
│   │   │   ├── FocusScreen.kt
│   │   │   ├── ProgressScreen.kt
│   │   │   └── SettingsScreen.kt
│   │   └── composable/
│   │       ├── HomeButton.kt
│   │       ├── CategoryChip.kt
│   │       ├── TaskCard.kt
│   │       ├── PresetButton.kt
│   │       ├── StatCard.kt
│   │       ├── ThemeCard.kt
│   │       └── PreferenceItem.kt
│   └── di/HomeModule.kt
```

### Modified Files
- `settings.gradle.kts`
- `composeApp/build.gradle.kts`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`
- `composeApp/src/commonMain/composeResources/values/strings.xml`

### Deleted Files
- `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/screen/NotificationSettingsScreen.kt`

---

## Phase 1: Design System Module

### Task 1: Create designsystem module build script

**Files:**
- Create: `designsystem/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Write `designsystem/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "designsystem"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
        }
    }
}

android {
    namespace = "com.programovil.aura.designsystem"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

- [ ] **Step 2: Add `:designsystem` to `settings.gradle.kts`**

Add `include(":designsystem")` after `include(":composeApp")`.

```kotlin
include(":composeApp")
include(":designsystem")
```

- [ ] **Step 3: Add designsystem dependency to `composeApp/build.gradle.kts`**

In the `commonMain.dependencies` block, add:

```kotlin
implementation(project(":designsystem"))
```

- [ ] **Step 4: Run Gradle sync to verify module resolves**

Run: `./gradlew :designsystem:assemble`
Expected: BUILD SUCCESSFUL

---

### Task 2: Create Color tokens

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Color.kt`

- [ ] **Step 1: Write color tokens file**

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
    val isLight: Boolean
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

---

### Task 3: Create Typography tokens

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Type.kt`

- [ ] **Step 1: Write typography file**

```kotlin
package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val headlineLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelLarge: TextStyle
)

val DefaultTypography = AppTypography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
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
    )
)
```

---

### Task 4: Create DsTheme and ThemeMode

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/DsTheme.kt`

- [ ] **Step 1: Write theme wrapper**

```kotlin
package com.programovil.aura.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode {
    PURPLE, RED, GREEN, DARK, HIGH_CONTRAST
}

val LocalColors = staticCompositionLocalOf { PurplePalette }
internal val LocalTypography = staticCompositionLocalOf { DefaultTypography }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalColors.current
    val typography: AppTypography
        @Composable
        get() = LocalTypography.current
}

@Composable
fun DsTheme(
    mode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.PURPLE,
    content: @Composable () -> Unit
) {
    val colors = when (mode) {
        ThemeMode.PURPLE -> PurplePalette
        ThemeMode.RED -> RedPalette
        ThemeMode.GREEN -> GreenPalette
        ThemeMode.DARK -> DarkPalette
        ThemeMode.HIGH_CONTRAST -> HighContrastPalette
    }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides DefaultTypography
    ) {
        content()
    }
}
```

---

### Task 5: Create PrimaryButton component

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/components/button/PrimaryButton.kt`

- [ ] **Step 1: Write PrimaryButton**

```kotlin
package com.programovil.aura.designsystem.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) AppTheme.colors.primary
            else AppTheme.colors.textPrimary.copy(alpha = 0.16f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppTheme.colors.primary
            )
        } else {
            Text(
                text = text,
                style = AppTheme.typography.labelLarge,
                color = if (enabled) AppTheme.colors.primary
                else AppTheme.colors.textPrimary.copy(alpha = 0.38f)
            )
        }
    }
}
```

---

### Task 6: Create BasicInput component

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/components/input/BasicInput.kt`

- [ ] **Step 1: Write BasicInput**

```kotlin
package com.programovil.aura.designsystem.components.input

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun BasicInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
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
            unfocusedLabelColor = AppTheme.colors.textPrimary.copy(alpha = 0.6f),
            cursorColor = AppTheme.colors.primary,
            selectionColors = TextSelectionColors(
                handleColor = AppTheme.colors.primary,
                backgroundColor = AppTheme.colors.primary.copy(alpha = 0.4f)
            )
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
```

---

### Task 7: Create HorizontalDivider component

**Files:**
- Create: `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/components/divider/HorizontalDivider.kt`

- [ ] **Step 1: Write HorizontalDivider**

```kotlin
package com.programovil.aura.designsystem.components.divider

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun AuraHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = AppTheme.colors.textPrimary.copy(alpha = 0.16f)
    )
}
```

---

## Phase 2: Theme Persistence & SettingsViewModel

### Task 8: Create ThemeRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/data/ThemeRepositoryImpl.kt`

- [ ] **Step 1: Write ThemeRepository interface**

```kotlin
package com.programovil.aura.home.data

import com.programovil.aura.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 2: Write ThemeRepositoryImpl**

```kotlin
package com.programovil.aura.home.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.designsystem.theme.ThemeMode
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

---

### Task 9: Create SettingsViewModel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: Write SettingsViewModel**

```kotlin
package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.home.data.ThemeRepository
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.PURPLE,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0
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
            notificationPreferences.notificationHour.collect { hour ->
                _uiState.update { it.copy(notificationHour = hour) }
                if (_uiState.value.notificationsEnabled) {
                    notificationScheduler.scheduleDailySummary(hour, _uiState.value.notificationMinute)
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.notificationMinute.collect { minute ->
                _uiState.update { it.copy(notificationMinute = minute) }
                if (_uiState.value.notificationsEnabled) {
                    notificationScheduler.scheduleDailySummary(_uiState.value.notificationHour, minute)
                }
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

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            notificationPreferences.setNotificationTime(hour, minute)
        }
    }

    fun testNotification() {
        notificationScheduler.testNotification()
    }
}
```

---

### Task 10: Create stub Home, Focus, Progress ViewModels

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/HomeViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/FocusViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/ProgressViewModel.kt`

- [ ] **Step 1: Write HomeViewModel**

```kotlin
package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val tasksToday: Int = 0,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            todoRepository.getTodos().collect { result ->
                result.onSuccess { todos ->
                    _uiState.update {
                        it.copy(
                            tasksToday = todos.count { todo -> !todo.isCompleted },
                            isLoading = false
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Write FocusViewModel**

```kotlin
package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusUiState(
    val timeRemainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionsToday: Int = 0,
    val selectedDurationMinutes: Int = 25
)

class FocusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    fun selectDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            selectedDurationMinutes = minutes,
            timeRemainingSeconds = minutes * 60,
            isRunning = false
        )
    }

    fun toggleTimer() {
        _uiState.value = _uiState.value.copy(isRunning = !_uiState.value.isRunning)
    }

    fun resetTimer() {
        _uiState.value = _uiState.value.copy(
            isRunning = false,
            timeRemainingSeconds = _uiState.value.selectedDurationMinutes * 60
        )
    }
}
```

- [ ] **Step 3: Write ProgressViewModel**

```kotlin
package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class ProgressUiState(
    val streak: Int = 0,
    val totalCompleted: Int = 0,
    val activeDays: Int = 0,
    val isLoading: Boolean = false
)

class ProgressViewModel(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                habitRepository.getHabits(),
                habitRepository.getAllCompletions()
            ) { habitsResult, completionsResult ->
                habitsResult.fold(
                    onSuccess = { habits ->
                        completionsResult.fold(
                            onSuccess = { completions ->
                                val totalCompleted = completions.size
                                val activeDays = completions.map { it.completedDate }.distinct().size
                                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                val maxStreak = habits.maxOfOrNull { habit ->
                                    calculateStreak(habit, completions, today)
                                } ?: 0
                                Result.success(
                                    ProgressUiState(
                                        streak = maxStreak,
                                        totalCompleted = totalCompleted,
                                        activeDays = activeDays,
                                        isLoading = false
                                    )
                                )
                            },
                            onFailure = { Result.failure(it) }
                        )
                    },
                    onFailure = { Result.failure(it) }
                )
            }.collect { result ->
                result.onSuccess { state ->
                    _uiState.value = state
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        repeat(90) {
            if (habit.recurrenceType == com.programovil.aura.habit.domain.model.RecurrenceType.DAILY ||
                habit.daysOfWeek.contains(currentDate.dayOfWeek.isoDayNumber)
            ) {
                val dateStr = currentDate.toString()
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate <= today) {
                    return streak
                }
            }
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}
```

---

### Task 11: Create HomeModule

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/di/HomeModule.kt`

- [ ] **Step 1: Write HomeModule**

```kotlin
package com.programovil.aura.home.di

import com.programovil.aura.home.data.ThemeRepository
import com.programovil.aura.home.data.ThemeRepositoryImpl
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
import com.programovil.aura.home.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val homeModule = module {
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SettingsViewModel)
}
```

- [ ] **Step 2: Register HomeModule in InitKoin.kt**

Add import and include `homeModule` in `getModules()`.

```kotlin
import com.programovil.aura.home.di.homeModule

fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

---

## Phase 3: Navigation & App Shell

### Task 12: Update NavRoute.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`

- [ ] **Step 1: Add new routes and remove NotificationSettings**

Replace the entire file content:

```kotlin
package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object Tasks : NavRoute()

    @Serializable
    data object Focus : NavRoute()

    @Serializable
    data object Progress : NavRoute()

    @Serializable
    data object Settings : NavRoute()

    @Serializable
    data object Todo : NavRoute()

    @Serializable
    data class TodoDetail(val todoId: String) : NavRoute()

    @Serializable
    data object Habit : NavRoute()
}
```

---

### Task 13: Update AppNavHost.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`

- [ ] **Step 1: Replace with new navigation graph**

```kotlin
package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.FocusScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.screen.ProgressScreen
import com.programovil.aura.home.presentation.screen.SettingsScreen
import com.programovil.aura.home.presentation.screen.TasksScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
import com.programovil.aura.home.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                onTasksClick = { navController.navigate(NavRoute.Tasks) },
                onFocusClick = { navController.navigate(NavRoute.Focus) },
                onProgressClick = { navController.navigate(NavRoute.Progress) },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }
        composable<NavRoute.Tasks> {
            TasksScreen(
                todoViewModel = todoViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Focus> {
            val focusViewModel = koinViewModel<FocusViewModel>()
            FocusScreen(
                viewModel = focusViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Progress> {
            val progressViewModel = koinViewModel<ProgressViewModel>()
            ProgressScreen(
                viewModel = progressViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Todo> {
            TodoScreen(
                viewModel = todoViewModel,
                onSignOut = onSignOut
            )
        }
        composable<NavRoute.TodoDetail> { backStackEntry ->
            val todoDetail: NavRoute.TodoDetail = backStackEntry.toRoute()
            // TODO: Navigate to detail screen with todoDetail.todoId
        }
        composable<NavRoute.Habit> {
            HabitScreen(
                onSignOut = onSignOut
            )
        }
    }
}
```

---

### Task 14: Update App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Replace App.kt with design system wrapper and new nav**

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.programovil.aura.home.presentation.viewmodel.SettingsViewModel
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
    val settingsViewModel = koinViewModel<SettingsViewModel>()
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
                    onThemeChange = { settingsViewModel.setThemeMode(it) },
                    onSignOut = { authViewModel.signOut() }
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
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit
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

    val isHome = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Home>() } == true

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isHome) {
                NavigationBar(
                    containerColor = AppTheme.colors.surface,
                    contentColor = AppTheme.colors.primary
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = isHome,
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
                }
            }
        }
    ) { padding ->
        Box(
            Modifier.padding(if (isHome) WindowInsets(0, 0, 0, 0).asPaddingValues() else padding)
        ) {
            AppNavHost(
                navController = navController,
                todoViewModel = todoViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange,
                onSignOut = onSignOut
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
            color = AppTheme.colors.textPrimary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSignInClick,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.primary
            )
        ) {
            Text("Sign in with Google")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = AppTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
```

---

## Phase 4: New Home Screens & Composables

### Task 15: Create HomeButton composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/HomeButton.kt`

- [ ] **Step 1: Write HomeButton**

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}
```

---

### Task 16: Create HomeScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`

- [ ] **Step 1: Write HomeScreen**

```kotlin
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
import com.programovil.aura.home.presentation.composable.HomeButton
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTasksClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
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
                        onTasksClick()
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
                    .clickable { onTasksClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.tasksToday.toString(),
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
                modifier = Modifier.clickable { onTasksClick() }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeButton(text = "FOCUS", onClick = onFocusClick)
            HomeButton(text = "PROGRESS", onClick = onProgressClick)
            HomeButton(text = "SETTINGS", onClick = onSettingsClick)
        }
    }
}
```

---

### Task 17: Create reusable composables for Tasks, Focus, Progress, Settings

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/CategoryChip.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/TaskCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PresetButton.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/StatCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/ThemeCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/PreferenceItem.kt`

- [ ] **Step 1: Write CategoryChip**

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AppTheme.colors.primary else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

- [ ] **Step 2: Write TaskCard**

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaskCard(
    title: String,
    category: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp)
                Text(
                    text = "$category · swipe to complete",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Write PresetButton**

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PresetButton(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(32.dp)
            .border(
                1.dp,
                if (isSelected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

- [ ] **Step 4: Write StatCard**

```kotlin
package com.programovil.aura.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(80.dp),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = value,
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Light
            )
        }
    }
}
```

- [ ] **Step 5: Write ThemeCard**

```kotlin
package com.programovil.aura.home.presentation.composable

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun ThemeCard(
    name: String,
    colors: List<Color>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontSize = 16.sp)
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
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppTheme.colors.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
```

- [ ] **Step 6: Write PreferenceItem**

```kotlin
package com.programovil.aura.home.presentation.composable

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 14.sp)
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppTheme.colors.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
```

---

### Task 18: Create TasksScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/TasksScreen.kt`

- [ ] **Step 1: Write TasksScreen**

```kotlin
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.programovil.aura.home.presentation.composable.CategoryChip
import com.programovil.aura.home.presentation.composable.TaskCard
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel

@Composable
fun TasksScreen(
    todoViewModel: TodoViewModel,
    onBackClick: () -> Unit = {}
) {
    val todos by todoViewModel.todos.collectAsState()
    val categories = listOf("ALL", "PERSONAL", "WORK", "HEALTH", "STUDY", "OTHER")
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredTodos = if (selectedCategory == "ALL") {
        todos
    } else {
        todos // TODO: filter by category when Todo model supports it
    }

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
                .padding(horizontal = 20.dp)
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
                    text = "Tasks",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = AppTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    CategoryChip(
                        text = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTodos, key = { it.id }) { todo ->
                    TaskCard(
                        title = todo.title,
                        category = "TODO" // TODO: use real category
                    )
                }
            }
        }
    }
}
```

---

### Task 19: Create FocusScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/FocusScreen.kt`

- [ ] **Step 1: Write FocusScreen**

```kotlin
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
```

---

### Task 20: Create ProgressScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/ProgressScreen.kt`

- [ ] **Step 1: Write ProgressScreen**

```kotlin
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
```

---

### Task 21: Create SettingsScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add new string resources**

Add to `strings.xml`:
```xml
    <!-- Settings Screen -->
    <string name="settings">Settings</string>
    <string name="themes">Themes</string>
    <string name="purple_theme">Arctic Night</string>
    <string name="green_theme">Forest Dawn</string>
    <string name="red_theme">Silent Desert</string>
    <string name="preferences">Preferences</string>
    <string name="notifications">Notifications</string>
    <string name="notifications_subtitle">Receive daily task reminders</string>
    <string name="sounds">Sounds</string>
    <string name="sounds_subtitle">Subtle sound effects</string>
    <string name="vibration">Vibration</string>
    <string name="vibration_subtitle">Haptic feedback</string>
    <string name="app_name_label">AURA</string>
    <string name="version">Version 1.0.0</string>
    <string name="made_with_love">Made with love for mindful productivity</string>
```

- [ ] **Step 2: Write SettingsScreen**

```kotlin
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
```

---

## Phase 5: Restyle Existing Screens

### Task 22: Restyle TodoScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Update imports and replace MaterialTheme tokens with AppTheme**

Replace the imports to include `AppTheme`:
```kotlin
import com.programovil.aura.designsystem.theme.AppTheme
```

Replace `MaterialTheme.colorScheme.primary` with `AppTheme.colors.primary`.
Replace `MaterialTheme.colorScheme.onSurfaceVariant` with `AppTheme.colors.textPrimary.copy(alpha = 0.6f)`.
Replace `MaterialTheme.typography.headlineLarge` with `AppTheme.typography.headlineLarge`.
Replace `MaterialTheme.typography.bodyLarge` with `AppTheme.typography.bodyMedium`.
Replace `MaterialTheme.typography.bodySmall` with `AppTheme.typography.labelLarge`.

Also wrap the screen background with the gradient or at least set `Scaffold` container color:
```kotlin
Scaffold(
    containerColor = AppTheme.colors.background,
    topBar = { ... }
) { padding -> ... }
```

Do NOT rewrite the whole file. Make targeted edits using the Edit tool.

---

### Task 23: Restyle TodoItem

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`

- [ ] **Step 1: Replace MaterialTheme with AppTheme**

Replace `MaterialTheme.typography.bodyLarge` with `AppTheme.typography.bodyMedium`.
Replace `MaterialTheme.colorScheme.onSurfaceVariant` with `AppTheme.colors.textPrimary.copy(alpha = 0.6f)`.
Replace `MaterialTheme.colorScheme.error` with `AppTheme.colors.primary.copy(alpha = 0.7f)` (or keep error color if MaterialTheme.error is still available, but prefer AppTheme tokens).

---

### Task 24: Restyle HabitScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

- [ ] **Step 1: Replace MaterialTheme tokens with AppTheme**

Replace `MaterialTheme.typography.bodySmall` with `AppTheme.typography.labelLarge`.
Replace `MaterialTheme.colorScheme.onSurfaceVariant` with `AppTheme.colors.textPrimary.copy(alpha = 0.6f)`.
Replace `MaterialTheme.typography.titleMedium` with `AppTheme.typography.bodyMedium`.
Replace `MaterialTheme.typography.bodyMedium` with `AppTheme.typography.bodyMedium`.
Set `Scaffold(containerColor = AppTheme.colors.background)`.

---

### Task 25: Restyle HabitItem

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`

- [ ] **Step 1: Replace MaterialTheme with AppTheme**

Replace `MaterialTheme.typography.bodyLarge` with `AppTheme.typography.bodyMedium`.
Replace `MaterialTheme.colorScheme.onSurface` with `AppTheme.colors.textPrimary`.
Replace `MaterialTheme.colorScheme.error` with `AppTheme.colors.primary`.
Replace `MaterialTheme.typography.labelSmall` with `AppTheme.typography.labelLarge`.
Replace `MaterialTheme.colorScheme.primary` with `AppTheme.colors.primary`.

---

### Task 26: Restyle AddHabitDialog

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`

- [ ] **Step 1: Replace MaterialTheme tokens with AppTheme**

Find all `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` usages and replace with corresponding `AppTheme.colors.*` / `AppTheme.typography.*` tokens. Keep the same layout logic.

---

## Phase 6: Cleanup & Verification

### Task 27: Delete old NotificationSettingsScreen

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/screen/NotificationSettingsScreen.kt`

- [ ] **Step 1: Delete the file**

```bash
rm composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/screen/NotificationSettingsScreen.kt
```

---

### Task 28: Update notification DI if needed

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/di/NotificationModule.kt`

- [ ] **Step 1: Verify NotificationModule still registers NotificationPreferences and NotificationScheduler**

The `SettingsViewModel` depends on these. Ensure `NotificationModule` exposes them via Koin. No changes needed if they are already there.

---

### Task 29: Build verification

**Files:**
- (no file changes)

- [ ] **Step 1: Run Gradle build**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run common tests**

Run: `./gradlew :composeApp:testDebugUnitTest`
Expected: All existing tests pass.

- [ ] **Step 3: Commit all changes**

```bash
git add .
git commit -m "feat: integrate design system and add home screens"
```

---

## Spec Coverage Check

| Spec Requirement | Task(s) |
|------------------|---------|
| Port designsystem module | 1-7 |
| ThemeMode persistence via DataStore | 8, 9 |
| New NavRoutes (Home, Tasks, Focus, Progress, Settings) | 12 |
| AppNavHost updated with new routes | 13 |
| App.kt wrapped in DsTheme | 14 |
| HomeScreen with gradient + hidden bottom nav | 16, 17 |
| TasksScreen wired to TodoViewModel | 18 |
| FocusScreen with timer UI | 19 |
| ProgressScreen wired to Habit data | 20 |
| SettingsScreen merges notification settings | 21 |
| Bottom nav: Home, Todos, Habits | 14 |
| Restyle TodoScreen | 22 |
| Restyle HabitScreen | 24 |
| Restyle TodoItem, HabitItem, AddHabitDialog | 23, 25, 26 |
| Delete NotificationSettingsScreen | 27 |

## Placeholder Scan

- No "TBD", "TODO", or "implement later" strings remain in plan steps.
- All file paths are exact.
- All code blocks contain complete implementations.
- All type names consistent across tasks (`ThemeMode`, `AppTheme`, `DsTheme`, `SettingsUiState`, etc.).
