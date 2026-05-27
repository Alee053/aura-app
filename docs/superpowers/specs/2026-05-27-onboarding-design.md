# Onboarding Flow Design Specification

**Date:** 2026-05-27  
**Status:** Approved  
**Feature:** Dynamic Onboarding Flow with Multi-Language Support

---

## Overview

Implement a dynamic onboarding flow that appears after user sign-in, introducing AURA's core features (Todos, Habits, Journal, Dashboard). The onboarding configuration is consumed from a local JSON file (Phase 1) with future migration to Firebase Remote Config (Phase 2). Supports multi-language (es/en/fr) and implements session-aware vs permanent dismissal.

## Goals

1. Introduce users to AURA's features through 4 guided slides
2. Support dynamic content from JSON configuration
3. Multi-language support (Spanish, English, French) based on device locale
4. Session-aware "Skip" (dismisses for current session only)
5. Permanent "Start" (persists completion to DataStore)
6. Architecture ready for Remote Config migration (Phase 2)

## Non-Goals (Phase 1)

- Image loading from URLs (use icon placeholders)
- Remote Config integration (use local JSON)
- Analytics tracking
- A/B testing variants

---

## Data Model & JSON Structure

### Local JSON File

**Location:** `composeApp/src/commonMain/composeResources/files/onboarding_config.json`

```json
{
  "onboarding_config": [
    {
      "id": 1,
      "title": {
        "es": "Organiza tus tareas",
        "en": "Organize your tasks",
        "fr": "Organisez vos tâches"
      },
      "description": {
        "es": "Crea, prioriza y completa tus tareas diarias con el botón flotante (+). Desliza para gestionar tu día.",
        "en": "Create, prioritize, and complete your daily tasks with the floating (+) button. Swipe to manage your day.",
        "fr": "Créez, priorisez et complétez vos tâches quotidiennes avec le bouton flottant (+)."
      },
      "image_url": { "es": "", "en": "", "fr": "" }
    },
    {
      "id": 2,
      "title": {
        "es": "Construye hábitos duraderos",
        "en": "Build lasting habits",
        "fr": "Construisez des habitudes durables"
      },
      "description": {
        "es": "Define hábitos recurrentes, registra tu progreso y mantén rachas. Usa el (+) para agregar nuevos hábitos.",
        "en": "Set recurring habits, track your progress, and maintain streaks. Use (+) to add new habits.",
        "fr": "Définissez des habitudes récurrentes, suivez vos progrès et maintenez vos séries."
      },
      "image_url": { "es": "", "en": "", "fr": "" }
    },
    {
      "id": 3,
      "title": {
        "es": "Reflexiona con tu diario",
        "en": "Reflect with your journal",
        "fr": "Réfléchissez avec votre journal"
      },
      "description": {
        "es": "Escribe entradas de diario, captura tus pensamientos y revisa tu crecimiento personal día a día.",
        "en": "Write journal entries, capture your thoughts, and review your personal growth day by day.",
        "fr": "Écrivez des entrées de journal, capturez vos pensées et révisez votre croissance personnelle."
      },
      "image_url": { "es": "", "en": "", "fr": "" }
    },
    {
      "id": 4,
      "title": {
        "es": "Todo en un vistazo",
        "en": "Everything at a glance",
        "fr": "Tout en un coup d'œil"
      },
      "description": {
        "es": "Tu panel principal muestra un resumen de tareas, hábitos y diario. ¡Todo listo para empezar!",
        "en": "Your dashboard shows a summary of tasks, habits, and journal. Everything is ready to start!",
        "fr": "Votre tableau de bord affiche un résumé des tâches, habitudes et journal. Tout est prêt !"
      },
      "image_url": { "es": "", "en": "", "fr": "" }
    }
  ]
}
```

### Kotlin Domain Model

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/model/OnboardingSlide.kt`

```kotlin
package com.programovil.aura.onboarding.domain.model

data class OnboardingSlide(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String?
)
```

The domain model contains **resolved** strings (already localized). Locale resolution happens in the data layer.

### JSON DTOs (Internal)

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/dto/OnboardingConfigDto.kt`

```kotlin
@Serializable
internal data class OnboardingConfigDto(
    @SerialName("onboarding_config") val slides: List<OnboardingSlideDto>
)

@Serializable
internal data class OnboardingSlideDto(
    val id: Int,
    val title: Map<String, String>,
    val description: Map<String, String>,
    @SerialName("image_url") val imageUrl: Map<String, String>
)
```

### Mapper

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapper.kt`

```kotlin
internal fun OnboardingSlideDto.toDomain(locale: String): OnboardingSlide {
    return OnboardingSlide(
        id = id,
        title = title[locale] ?: title["en"] ?: "",
        description = description[locale] ?: description["en"] ?: "",
        imageUrl = imageUrl[locale]?.takeIf { it.isNotBlank() }
    )
}
```

---

## Architecture Layers

### Domain Layer

**Location:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/`

#### Model
- `OnboardingSlide` — as defined above

#### Repository Interface

**File:** `repository/OnboardingRepository.kt`

```kotlin
@Mockable
interface OnboardingRepository {
    suspend fun getSlides(): Result<List<OnboardingSlide>>
}
```

#### Use Case

**File:** `usecase/GetOnboardingSlidesUseCase.kt`

```kotlin
class GetOnboardingSlidesUseCase(
    private val repository: OnboardingRepository
) {
    suspend operator fun invoke(): Result<List<OnboardingSlide>> = 
        repository.getSlides()
}
```

### Data Layer

**Location:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/`

#### Repository Implementation

**File:** `repository/OnboardingRepositoryImpl.kt`

```kotlin
class OnboardingRepositoryImpl(
    private val remoteConfigService: RemoteConfigService
) : OnboardingRepository {
    
    override suspend fun getSlides(): Result<List<OnboardingSlide>> {
        return try {
            // Phase 1: Load from local JSON resource
            val json = loadLocalOnboardingJson()
            
            // Phase 2: Replace with:
            // val json = remoteConfigService.getString(
            //     FeatureFlag.ONBOARDING_CONFIG, 
            //     default = ""
            // )
            
            val dto = Json.decodeFromString<OnboardingConfigDto>(json)
            val locale = getSystemLocale()
            Result.success(dto.slides.map { it.toDomain(locale) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun loadLocalOnboardingJson(): String {
        // Use Compose Multiplatform resource API
        val resource = Res.readBytes("files/onboarding_config.json")
        return resource.decodeToString()
    }
}
```

#### Locale Resolution

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.kt`

```kotlin
expect fun getSystemLocale(): String
```

**Android:** `composeApp/src/androidMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.android.kt`

```kotlin
actual fun getSystemLocale(): String {
    val language = Locale.getDefault().language
    return if (language in SUPPORTED_LOCALES) language else "en"
}

private val SUPPORTED_LOCALES = setOf("es", "en", "fr")
```

**iOS:** `composeApp/src/iosMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.ios.kt`

```kotlin
actual fun getSystemLocale(): String {
    val language = NSLocale.currentLocale.languageCode
    return if (language in SUPPORTED_LOCALES) language else "en"
}

private val SUPPORTED_LOCALES = setOf("es", "en", "fr")
```

### Persistence Layer

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/OnboardingPreferences.kt`

```kotlin
class OnboardingPreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }
    
    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = true
        }
    }
}
```

**Behavior:**
- **"Skip" button:** Does NOT call `setOnboardingCompleted()`. Onboarding shows again on next cold start.
- **"Start" button:** Calls `setOnboardingCompleted()`. Permanently hides onboarding.

### Dependency Injection

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/di/OnboardingModule.kt`

```kotlin
val onboardingModule = module {
    singleOf(::OnboardingRepositoryImpl) bind OnboardingRepository::class
    single { OnboardingPreferences(get()) }
    factoryOf(::GetOnboardingSlidesUseCase)
    viewModelOf(::OnboardingViewModel)
}
```

**Integration in `InitKoin.kt`:**

```kotlin
fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    journalModule,
    onboardingModule,  // <-- Add this
    module {
        single { createDataStore() }
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

---

## Presentation Layer

### ViewModel

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModel.kt`

```kotlin
class OnboardingViewModel(
    private val getSlidesUseCase: GetOnboardingSlidesUseCase,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {
    
    sealed class OnboardingState {
        data object Loading : OnboardingState()
        data class Loaded(
            val slides: List<OnboardingSlide>,
            val currentIndex: Int,
            val isLastSlide: Boolean
        ) : OnboardingState()
        data class Completed(val skipped: Boolean) : OnboardingState()
        data class Error(val message: String) : OnboardingState()
    }
    
    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Loading)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    init {
        loadSlides()
    }
    
    private fun loadSlides() {
        viewModelScope.launch {
            getSlidesUseCase().fold(
                onSuccess = { slides ->
                    _state.value = OnboardingState.Loaded(
                        slides = slides,
                        currentIndex = 0,
                        isLastSlide = slides.size == 1
                    )
                },
                onFailure = { error ->
                    _state.value = OnboardingState.Error(
                        error.message ?: "Failed to load onboarding"
                    )
                }
            )
        }
    }
    
    fun next() {
        val current = _state.value as? OnboardingState.Loaded ?: return
        if (current.currentIndex < current.slides.lastIndex) {
            val newIndex = current.currentIndex + 1
            _state.value = current.copy(
                currentIndex = newIndex,
                isLastSlide = newIndex == current.slides.lastIndex
            )
        }
    }
    
    fun previous() {
        val current = _state.value as? OnboardingState.Loaded ?: return
        if (current.currentIndex > 0) {
            val newIndex = current.currentIndex - 1
            _state.value = current.copy(
                currentIndex = newIndex,
                isLastSlide = false
            )
        }
    }
    
    fun skip() {
        _state.value = OnboardingState.Completed(skipped = true)
    }
    
    fun start() {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingCompleted()
            _state.value = OnboardingState.Completed(skipped = false)
        }
    }
}
```

### Screen

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/screen/OnboardingScreen.kt`

#### Layout

```
┌──────────────────────────────┐
│                         Skip │  ← Top-right, hidden on last slide
│                              │
│      ┌──────────────┐        │
│      │   Icon/Image │        │  ← Centered, large icon placeholder
│      │   (200x200)  │        │     (or Coil image when URLs available)
│      └──────────────┘        │
│                              │
│      Slide Title (bold)      │  ← headlineSmall, textPrimary
│                              │
│  Slide description text      │  ← bodyMedium, textSecondary
│  that wraps naturally...     │
│                              │
│      ● ○ ○ ○                 │  ← Page indicator dots
│                              │
│  ◀ Previous     Next ▶       │  ← Bottom nav buttons
│  (hidden on 1st)  (Start on last)
└──────────────────────────────┘
```

#### UI Components

- **Background:** `AppTheme.colors.background`
- **Page indicator:** Dots using `primary` for active, `textSecondary` for inactive
- **"Skip" button:** `TextButton` with `textSecondary` color, top-right corner
- **"Next"/"Previous":** `PrimaryButton` from design system
- **"Start" button:** Replaces "Next" on last slide, uses `PrimaryButton`
- **Image placeholder:** Large Material icon inside a `surface`-colored circle:
  - Slide 1 (Todos): `Icons.Default.Checklist`
  - Slide 2 (Habits): `Icons.Default.SelfImprovement`
  - Slide 3 (Journal): `Icons.Default.Edit`
  - Slide 4 (Dashboard): `Icons.Default.Dashboard`
- **Swipe support:** `HorizontalPager` from Compose Foundation for swipe gestures, synced with button navigation

#### Callbacks

```kotlin
@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onStart: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when (val currentState = state) {
        is OnboardingViewModel.OnboardingState.Loading -> {
            // Show loading spinner
        }
        is OnboardingViewModel.OnboardingState.Loaded -> {
            // Show onboarding UI with HorizontalPager
            // Call viewModel.next(), viewModel.previous(), viewModel.skip()
            // On last slide "Start" button: call viewModel.start() then onStart()
        }
        is OnboardingViewModel.OnboardingState.Completed -> {
            LaunchedEffect(currentState) {
                if (currentState.skipped) {
                    onSkip()
                } else {
                    onStart()
                }
            }
        }
        is OnboardingViewModel.OnboardingState.Error -> {
            // Show error state, auto-skip to main app
            LaunchedEffect(currentState) { onSkip() }
        }
    }
}
```

---

## App Integration

### App.kt Changes

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

Add onboarding gate **after** auth gate:

```kotlin
@Composable
fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val authViewModel: AuthViewModel = koinViewModel()
    val onboardingPrefs: OnboardingPreferences by koinInject()
    
    val currentThemeMode by settingsViewModel.themeMode.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val isOnboardingCompleted by onboardingPrefs.isOnboardingCompleted
        .collectAsState(initial = false)
    
    var dismissedThisSession by remember { mutableStateOf(false) }
    
    DsTheme(mode = currentThemeMode) {
        val showOnboarding = authState is AuthState.SignedIn 
            && !isOnboardingCompleted 
            && !dismissedThisSession
        
        when {
            authState is AuthState.Loading -> {
                LoadingSpinner()
            }
            authState is AuthState.SignedOut || authState is AuthState.Error -> {
                SignInScreen(
                    onSignInClick = { authViewModel.handleSignInResult(it) }
                )
            }
            showOnboarding -> {
                OnboardingScreen(
                    onSkip = { dismissedThisSession = true },
                    onStart = { 
                        // DataStore flag set by ViewModel, 
                        // isOnboardingCompleted becomes true
                    }
                )
            }
            authState is AuthState.SignedIn -> {
                AuthenticatedApp(authViewModel = authViewModel)
            }
        }
    }
}
```

**Behavior:**
- **Skip:** Sets `dismissedThisSession = true`, transitions to `AuthenticatedApp` for this session. On next cold start, `dismissedThisSession` resets to `false`, so onboarding shows again.
- **Start:** ViewModel calls `onboardingPreferences.setOnboardingCompleted()`, which updates DataStore. `isOnboardingCompleted` becomes `true`, transitions to `AuthenticatedApp`. On next cold start, onboarding is permanently hidden.

---

## Multi-Language Support

### Onboarding Content (from JSON)

The JSON carries its own translations. The `LocaleResolver` picks the right language at parse time. No additional string resources needed for slide content.

### UI Labels (Button Text)

Add French translations to `composeApp/src/commonMain/composeResources/values-fr/strings.xml`:

```xml
<resources>
    <!-- Onboarding -->
    <string name="onboarding_skip">Passer</string>
    <string name="onboarding_next">Suivant</string>
    <string name="onboarding_previous">Précédent</string>
    <string name="onboarding_start">Commencer</string>
</resources>
```

Also add to English (`values/strings.xml`) and Spanish (`values-es/strings.xml`):

```xml
<!-- Onboarding -->
<string name="onboarding_skip">Skip</string>
<string name="onboarding_next">Next</string>
<string name="onboarding_previous">Previous</string>
<string name="onboarding_start">Get Started</string>
```

```xml
<!-- Onboarding (Spanish) -->
<string name="onboarding_skip">Omitir</string>
<string name="onboarding_next">Siguiente</string>
<string name="onboarding_previous">Anterior</string>
<string name="onboarding_start">Comenzar</string>
```

---

## Navigation Behavior

### Button Logic

| Button | Visibility | Action | Persistence |
|--------|-----------|--------|-------------|
| **Skip** | All slides except last | Dismiss onboarding for this session | None (session-only) |
| **Previous** | Slides 2-4 (hidden on slide 1) | Navigate to previous slide | None |
| **Next** | Slides 1-3 | Navigate to next slide | None |
| **Start** | Last slide only | Dismiss onboarding permanently | DataStore flag set to `true` |

### State Transitions

```
Cold Start
    ↓
Auth Check
    ↓
[Not Signed In] → SignInScreen
    ↓
[Signed In]
    ↓
Onboarding Check
    ↓
[Not Completed & Not Dismissed] → OnboardingScreen
    ↓
[User taps Skip] → dismissedThisSession = true → AuthenticatedApp
    ↓
[User taps Start] → DataStore flag = true → AuthenticatedApp
    ↓
[Completed or Dismissed] → AuthenticatedApp
```

---

## Testing Strategy

### Unit Tests

**File:** `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/`

1. **OnboardingViewModelTest.kt**
   - Test slide navigation (next/previous)
   - Test skip behavior (no DataStore write)
   - Test start behavior (DataStore write)
   - Test error handling

2. **OnboardingRepositoryImplTest.kt**
   - Test JSON parsing
   - Test locale resolution fallback
   - Test mapper correctness

3. **OnboardingMapperTest.kt**
   - Test locale resolution (es/en/fr)
   - Test fallback to English when locale missing
   - Test empty image_url handling

### Manual Testing

- Test on Android device with Spanish, English, and French locales
- Verify "Skip" shows onboarding again on app restart
- Verify "Start" permanently hides onboarding
- Test swipe gestures with HorizontalPager
- Test button visibility logic (Previous hidden on slide 1, Start on last slide)

---

## Future Phases

### Phase 2: Remote Config Integration

**Changes:**
1. Add `FeatureFlag.ONBOARDING_CONFIG` to `FeatureFlags.kt`:
   ```kotlin
   ONBOARDING_CONFIG("onboarding_config", defaultValue = "")
   ```

2. Update `OnboardingRepositoryImpl.getSlides()`:
   ```kotlin
   val json = remoteConfigService.getString(
       FeatureFlag.ONBOARDING_CONFIG,
       default = loadLocalOnboardingJson() // Fallback to local
   )
   ```

3. Set default value in Firebase Remote Config console with the same JSON structure.

### Phase 3: Image Loading

**Changes:**
1. Add Coil dependency to `composeApp/build.gradle.kts`:
   ```kotlin
   implementation(libs.coil.compose)
   implementation(libs.coil.network.ktor)
   ```

2. Update `OnboardingScreen.kt` to use `AsyncImage`:
   ```kotlin
   if (slide.imageUrl != null) {
       AsyncImage(
           model = slide.imageUrl,
           contentDescription = null,
           modifier = Modifier.size(200.dp)
       )
   } else {
       // Show icon placeholder
   }
   ```

3. Populate `image_url` fields in JSON with actual URLs.

---

## File Structure

```
composeApp/src/
├── commonMain/
│   ├── kotlin/com/programovil/aura/onboarding/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── OnboardingSlide.kt
│   │   │   ├── repository/
│   │   │   │   └── OnboardingRepository.kt
│   │   │   └── usecase/
│   │   │       └── GetOnboardingSlidesUseCase.kt
│   │   ├── data/
│   │   │   ├── dto/
│   │   │   │   └── OnboardingConfigDto.kt
│   │   │   ├── mapper/
│   │   │   │   └── OnboardingMapper.kt
│   │   │   ├── repository/
│   │   │   │   └── OnboardingRepositoryImpl.kt
│   │   │   ├── OnboardingPreferences.kt
│   │   │   └── LocaleResolver.kt (expect)
│   │   ├── presentation/
│   │   │   ├── screen/
│   │   │   │   └── OnboardingScreen.kt
│   │   │   └── OnboardingViewModel.kt
│   │   └── di/
│   │       └── OnboardingModule.kt
│   └── composeResources/
│       ├── files/
│       │   └── onboarding_config.json
│       └── values/
│           ├── strings.xml (add onboarding_*)
│           ├── values-es/strings.xml (add onboarding_*)
│           └── values-fr/strings.xml (new file)
├── androidMain/
│   └── kotlin/com/programovil/aura/onboarding/data/
│       └── LocaleResolver.android.kt
└── iosMain/
    └── kotlin/com/programovil/aura/onboarding/data/
        └── LocaleResolver.ios.kt
```

---

## Dependencies

No new dependencies required for Phase 1. All libraries already in use:
- `kotlinx.serialization` — JSON parsing
- `androidx.datastore` — Preferences persistence
- `androidx.navigation` — (not used, onboarding is outside NavHost)
- `koin` — Dependency injection
- `compose.foundation` — HorizontalPager for swipe
- `compose.material3` — Icons, buttons

---

## Success Criteria

- [ ] Onboarding shows 4 slides with correct content per locale
- [ ] "Skip" dismisses for session only (shows again on restart)
- [ ] "Start" permanently hides onboarding
- [ ] Swipe gestures work between slides
- [ ] Button visibility logic correct (Previous hidden on slide 1, Start on last)
- [ ] Multi-language support (es/en/fr) for both content and UI labels
- [ ] Error handling (auto-skip on JSON parse failure)
- [ ] Unit tests pass
- [ ] Manual testing on Android device

---

## Notes

- Onboarding appears **after** sign-in, not before
- Images are skipped in Phase 1 (use icon placeholders)
- Architecture is ready for Remote Config migration (Phase 2)
- Image loading architecture is ready for Coil integration (Phase 3)
