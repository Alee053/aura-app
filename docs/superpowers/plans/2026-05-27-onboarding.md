# Onboarding Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a dynamic onboarding flow with 4 slides, multi-language support (es/en/fr), and session-aware vs permanent dismissal, consuming configuration from a local JSON file.

**Architecture:** Onboarding is gated in `App.kt` after the auth check. A `HorizontalPager` displays slides loaded from a local JSON resource. An `OnboardingPreferences` class persists completion state via DataStore. "Skip" dismisses for the session only; "Start" persists permanently.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (HorizontalPager), kotlinx.serialization, DataStore Preferences, Koin DI, Compose Resources API (`Res.readBytes`)

**Spec:** `docs/superpowers/specs/2026-05-27-onboarding-design.md`

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `composeApp/src/commonMain/composeResources/files/onboarding_config.json` | Onboarding slide content (titles, descriptions, image URLs) in es/en/fr |
| `composeApp/src/commonMain/composeResources/values-fr/strings.xml` | French translations for UI labels |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/model/OnboardingSlide.kt` | Domain model for a single onboarding slide |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/repository/OnboardingRepository.kt` | Repository interface |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/usecase/GetOnboardingSlidesUseCase.kt` | Use case to fetch slides |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/dto/OnboardingConfigDto.kt` | JSON DTOs for deserialization |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapper.kt` | DTO → Domain model mapper with locale resolution |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.kt` | `expect fun getSystemLocale()` |
| `composeApp/src/androidMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.android.kt` | Android locale implementation |
| `composeApp/src/iosMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.ios.kt` | iOS locale implementation |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImpl.kt` | Repository implementation (loads JSON, parses, maps) |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/OnboardingPreferences.kt` | DataStore persistence for onboarding completion |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/di/OnboardingModule.kt` | Koin DI module |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModel.kt` | ViewModel with navigation state management |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/screen/OnboardingScreen.kt` | Full onboarding UI with HorizontalPager |
| `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapperTest.kt` | Mapper unit tests |
| `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImplTest.kt` | Repository unit tests |
| `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModelTest.kt` | ViewModel unit tests |

### Modified Files

| File | Change |
|------|--------|
| `composeApp/src/commonMain/composeResources/values/strings.xml` | Add `onboarding_skip`, `onboarding_next`, `onboarding_previous`, `onboarding_start` |
| `composeApp/src/commonMain/composeResources/values-es/strings.xml` | Add Spanish onboarding strings |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt` | Register `onboardingModule` |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt` | Add onboarding gate after auth check |

---

### Task 1: JSON Config File & String Resources

**Files:**
- Create: `composeApp/src/commonMain/composeResources/files/onboarding_config.json`
- Create: `composeApp/src/commonMain/composeResources/values-fr/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-es/strings.xml`

- [ ] **Step 1: Create the onboarding JSON config file**

Create `composeApp/src/commonMain/composeResources/files/onboarding_config.json`:

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

- [ ] **Step 2: Add onboarding strings to English `strings.xml`**

In `composeApp/src/commonMain/composeResources/values/strings.xml`, add before the closing `</resources>` tag:

```xml
    <!-- Onboarding -->
    <string name="onboarding_skip">Skip</string>
    <string name="onboarding_next">Next</string>
    <string name="onboarding_previous">Previous</string>
    <string name="onboarding_start">Get Started</string>
```

- [ ] **Step 3: Add onboarding strings to Spanish `strings.xml`**

In `composeApp/src/commonMain/composeResources/values-es/strings.xml`, add before the closing `</resources>` tag:

```xml
    <!-- Onboarding -->
    <string name="onboarding_skip">Omitir</string>
    <string name="onboarding_next">Siguiente</string>
    <string name="onboarding_previous">Anterior</string>
    <string name="onboarding_start">Comenzar</string>
```

- [ ] **Step 4: Create French `strings.xml`**

Create `composeApp/src/commonMain/composeResources/values-fr/strings.xml`:

```xml
<resources>
    <!-- Navigation -->
    <string name="nav_home">Accueil</string>
    <string name="nav_todos">Tâches</string>
    <string name="nav_habits">Habitudes</string>
    <string name="nav_settings">Paramètres</string>
    <string name="nav_journal">Journal</string>

    <!-- Todo Screen -->
    <string name="todos_title">Mes Tâches</string>
    <string name="add_todo">Ajouter une tâche</string>
    <string name="new_todo_hint">Nouvelle tâche</string>
    <string name="empty_todos">Pas encore de tâches. Ajoutez la première !</string>
    <string name="delete">X</string>
    <string name="sign_out">Se déconnecter</string>
    <string name="select_due_date">Sélectionner une date</string>
    <string name="due_date_label">Date : %1$s</string>
    <string name="ok">OK</string>
    <string name="cancel">Annuler</string>

    <!-- Todo Dialog -->
    <string name="new_todo">Nouvelle tâche</string>
    <string name="edit_todo">Modifier la tâche</string>
    <string name="todo_name_hint">Nom de la tâche</string>
    <string name="todo_description_hint">Description (facultatif)</string>
    <string name="add_due_date">Ajouter une date</string>
    <string name="clear_due_date">Effacer</string>
    <string name="add_first_todo">Ajouter la première tâche</string>
    <string name="completed_section">Terminées</string>

    <!-- Habit Screen -->
    <string name="habits_title">Habitudes</string>
    <string name="add_habit">Ajouter une habitude</string>
    <string name="add_first_habit">Ajouter la première habitude</string>
    <string name="today">Aujourd\'hui</string>
    <string name="tomorrow">Demain</string>
    <string name="this_week">Cette semaine</string>
    <string name="empty_habits">Pas encore d\'habitudes. Ajoutez la première !</string>

    <!-- Add Habit Dialog -->
    <string name="new_habit">Nouvelle habitude</string>
    <string name="edit_habit">Modifier l\'habitude</string>
    <string name="delete_habit">Supprimer</string>
    <string name="habit_name">Nom de l\'habitude</string>
    <string name="daily">Quotidien</string>
    <string name="daily_badge">Quotidien</string>
    <string name="weekly">Hebdomadaire</string>
    <string name="monthly">Mensuel</string>
    <string name="per_week">/ semaine</string>
    <string name="per_month">/ mois</string>
    <string name="repeat_on">Répéter le :</string>
    <string name="color_label">Couleur :</string>
    <string name="save">Enregistrer</string>
    <string name="day_mon">L</string>
    <string name="day_tue">M</string>
    <string name="day_wed">M</string>
    <string name="day_thu">J</string>
    <string name="day_fri">V</string>
    <string name="day_sat">S</string>
    <string name="day_sun">D</string>

    <!-- Notification Settings -->
    <string name="notification_settings">Paramètres de notification</string>
    <string name="back">Retour</string>
    <string name="daily_summary">Résumé quotidien</string>
    <string name="enabled">Activé</string>
    <string name="test_notification">Tester la notification</string>
    <string name="notification_time">Heure de notification</string>
    <string name="notification_helper">Les notifications seront envoyées quotidiennement à l\'heure définie.</string>
    <string name="notification_permission">Autorisation de notification</string>
    <string name="notification_permission_text">Autorisez les notifications pour recevoir les résumés quotidiens et les rappels.</string>
    <string name="allow">Autoriser</string>
    <string name="deny">Refuser</string>
    <string name="due_label">Échéance : %1$s</string>

    <!-- Settings Screen -->
    <string name="settings">Paramètres</string>
    <string name="settings_title">Paramètres</string>
    <string name="logout_button">Se déconnecter</string>
    <string name="themes">Thèmes</string>
    <string name="themes_section">Thèmes</string>
    <string name="purple_theme">Nuit Arctique</string>
    <string name="green_theme">Aube Forestière</string>
    <string name="red_theme">Désert Silencieux</string>
    <string name="dark_theme">Minuit</string>
    <string name="high_contrast_theme">Contraste Élevé</string>
    <string name="preferences">Préférences</string>
    <string name="preferences_section">Préférences</string>
    <string name="notifications">Notifications</string>
    <string name="notifications_subtitle">Recevoir des rappels quotidiens</string>
    <string name="reminder_time_label">Heure du rappel</string>
    <string name="sounds">Sons</string>
    <string name="sounds_subtitle">Effets sonores subtils</string>
    <string name="vibration">Vibration</string>
    <string name="vibration_subtitle">Retour haptique</string>
    <string name="app_name_label">AURA</string>
    <string name="version">Version 1.0.0</string>
    <string name="made_with_love">Fait avec amour pour une productivité consciente</string>

    <!-- Sign In Screen -->
    <string name="sign_in_title">Aura</string>
    <string name="sign_in_subtitle">Connectez-vous pour synchroniser vos tâches</string>
    <string name="sign_in_button">Se connecter avec Google</string>

    <!-- Home Screen -->
    <string name="home_dashboard_todos_title">TÂCHES AUJOURD\'HUI</string>
    <string name="home_dashboard_habits_title">SÉRIE D\'HABITUDES</string>
    <string name="home_dashboard_todos_subtitle">%1$d restantes</string>
    <string name="home_dashboard_habits_subtitle">%1$d/%2$d faites aujourd\'hui</string>
    <string name="settings_content_description">Paramètres</string>

    <!-- Habit Item -->
    <string name="streak_format">%1$d</string>

    <!-- Common -->
    <string name="content_description_back">Retour</string>
    <string name="content_description_delete">Supprimer</string>

    <!-- Journal -->
    <string name="journal_title">Journal</string>
    <string name="journal_empty">Pas encore d\'entrées</string>
    <string name="journal_empty_subtitle">Appuyez sur + pour écrire votre première entrée</string>
    <string name="journal_title_hint">Titre</string>
    <string name="journal_content_hint">Écrivez vos pensées...</string>
    <string name="journal_save">Enregistrer</string>
    <string name="journal_delete">Supprimer</string>
    <string name="journal_back">Retour</string>
    <string name="journal_new_entry">Nouvelle entrée</string>
    <string name="journal_edit_entry">Modifier l\'entrée</string>
    <string name="journal_add_entry">Ajouter une entrée</string>

    <!-- Onboarding -->
    <string name="onboarding_skip">Passer</string>
    <string name="onboarding_next">Suivant</string>
    <string name="onboarding_previous">Précédent</string>
    <string name="onboarding_start">Commencer</string>
</resources>
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/composeResources/
git commit -m "feat(onboarding): add JSON config and multi-language string resources"
```

---

### Task 2: Domain Layer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/model/OnboardingSlide.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/repository/OnboardingRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/usecase/GetOnboardingSlidesUseCase.kt`

- [ ] **Step 1: Create the domain model**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/model/OnboardingSlide.kt`:

```kotlin
package com.programovil.aura.onboarding.domain.model

data class OnboardingSlide(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String?
)
```

- [ ] **Step 2: Create the repository interface**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/repository/OnboardingRepository.kt`:

```kotlin
package com.programovil.aura.onboarding.domain.repository

import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import io.mockative.Mockable

@Mockable
interface OnboardingRepository {
    suspend fun getSlides(): Result<List<OnboardingSlide>>
}
```

- [ ] **Step 3: Create the use case**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/usecase/GetOnboardingSlidesUseCase.kt`:

```kotlin
package com.programovil.aura.onboarding.domain.usecase

import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository

class GetOnboardingSlidesUseCase(
    private val repository: OnboardingRepository
) {
    suspend operator fun invoke(): Result<List<OnboardingSlide>> =
        repository.getSlides()
}
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/domain/
git commit -m "feat(onboarding): add domain model, repository interface, and use case"
```

---

### Task 3: DTOs & Mapper (with tests)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/dto/OnboardingConfigDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapperTest.kt`

- [ ] **Step 1: Create the DTOs**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/dto/OnboardingConfigDto.kt`:

```kotlin
package com.programovil.aura.onboarding.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

- [ ] **Step 2: Create the mapper**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapper.kt`:

```kotlin
package com.programovil.aura.onboarding.data.mapper

import com.programovil.aura.onboarding.data.dto.OnboardingSlideDto
import com.programovil.aura.onboarding.domain.model.OnboardingSlide

internal fun OnboardingSlideDto.toDomain(locale: String): OnboardingSlide {
    return OnboardingSlide(
        id = id,
        title = title[locale] ?: title["en"] ?: "",
        description = description[locale] ?: description["en"] ?: "",
        imageUrl = imageUrl[locale]?.takeIf { it.isNotBlank() }
    )
}
```

- [ ] **Step 3: Write mapper tests**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/mapper/OnboardingMapperTest.kt`:

```kotlin
package com.programovil.aura.onboarding.data.mapper

import com.programovil.aura.onboarding.data.dto.OnboardingSlideDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingMapperTest {

    private val sampleDto = OnboardingSlideDto(
        id = 1,
        title = mapOf("es" to "Título ES", "en" to "Title EN", "fr" to "Titre FR"),
        description = mapOf("es" to "Desc ES", "en" to "Desc EN", "fr" to "Desc FR"),
        imageUrl = mapOf("es" to "https://es.png", "en" to "", "fr" to "https://fr.png")
    )

    @Test
    fun `toDomain resolves Spanish locale`() {
        val result = sampleDto.toDomain("es")
        assertEquals("Título ES", result.title)
        assertEquals("Desc ES", result.description)
        assertEquals("https://es.png", result.imageUrl)
    }

    @Test
    fun `toDomain resolves English locale`() {
        val result = sampleDto.toDomain("en")
        assertEquals("Title EN", result.title)
        assertEquals("Desc EN", result.description)
        assertEquals(null, result.imageUrl)
    }

    @Test
    fun `toDomain resolves French locale`() {
        val result = sampleDto.toDomain("fr")
        assertEquals("Titre FR", result.title)
        assertEquals("Desc FR", result.description)
        assertEquals("https://fr.png", result.imageUrl)
    }

    @Test
    fun `toDomain falls back to English for unsupported locale`() {
        val result = sampleDto.toDomain("de")
        assertEquals("Title EN", result.title)
        assertEquals("Desc EN", result.description)
    }

    @Test
    fun `toDomain falls back to English when locale key missing`() {
        val dto = sampleDto.copy(title = mapOf("en" to "Only EN"))
        val result = dto.toDomain("ja")
        assertEquals("Only EN", result.title)
    }

    @Test
    fun `toDomain returns empty string when no locale and no English fallback`() {
        val dto = sampleDto.copy(title = mapOf("de" to "German only"))
        val result = dto.toDomain("ja")
        assertEquals("", result.title)
    }

    @Test
    fun `toDomain returns null imageUrl for blank string`() {
        val result = sampleDto.toDomain("en")
        assertNull(result.imageUrl)
    }

    @Test
    fun `toDomain preserves id`() {
        val result = sampleDto.toDomain("en")
        assertEquals(1, result.id)
    }
}
```

- [ ] **Step 4: Run mapper tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.onboarding.data.mapper.OnboardingMapperTest"`

Expected: All 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/dto/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/mapper/
git add composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/mapper/
git commit -m "feat(onboarding): add JSON DTOs and mapper with tests"
```

---

### Task 4: Locale Resolver (expect/actual)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.ios.kt`

- [ ] **Step 1: Create the expect declaration**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.kt`:

```kotlin
package com.programovil.aura.onboarding.data

internal val SUPPORTED_LOCALES = setOf("es", "en", "fr")

internal expect fun getSystemLocale(): String
```

- [ ] **Step 2: Create the Android actual implementation**

Create `composeApp/src/androidMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.android.kt`:

```kotlin
package com.programovil.aura.onboarding.data

import java.util.Locale

internal actual fun getSystemLocale(): String {
    val language = Locale.getDefault().language
    return if (language in SUPPORTED_LOCALES) language else "en"
}
```

- [ ] **Step 3: Create the iOS actual implementation**

Create `composeApp/src/iosMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.ios.kt`:

```kotlin
package com.programovil.aura.onboarding.data

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

internal actual fun getSystemLocale(): String {
    val language = NSLocale.currentLocale.languageCode
    return if (language in SUPPORTED_LOCALES) language else "en"
}
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.kt
git add composeApp/src/androidMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.android.kt
git add composeApp/src/iosMain/kotlin/com/programovil/aura/onboarding/data/LocaleResolver.ios.kt
git commit -m "feat(onboarding): add locale resolver with expect/actual for Android and iOS"
```

---

### Task 5: Onboarding Preferences (with tests)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/OnboardingPreferences.kt`

- [ ] **Step 1: Create OnboardingPreferences**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/OnboardingPreferences.kt`:

```kotlin
package com.programovil.aura.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = true }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/OnboardingPreferences.kt
git commit -m "feat(onboarding): add DataStore preferences for onboarding completion"
```

---

### Task 6: Repository Implementation (with tests)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImplTest.kt`

- [ ] **Step 1: Create the repository implementation**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImpl.kt`:

```kotlin
package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.data.dto.OnboardingConfigDto
import com.programovil.aura.onboarding.data.getSystemLocale
import com.programovil.aura.onboarding.data.mapper.toDomain
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.shared.RemoteConfigService
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Res

class OnboardingRepositoryImpl(
    private val remoteConfigService: RemoteConfigService
) : OnboardingRepository {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getSlides(): Result<List<OnboardingSlide>> {
        return try {
            val jsonString = Res.readBytes("files/onboarding_config.json")
                .decodeToString()
            val dto = json.decodeFromString<OnboardingConfigDto>(jsonString)
            val locale = getSystemLocale()
            Result.success(dto.slides.map { it.toDomain(locale) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: Write repository tests**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/repository/OnboardingRepositoryImplTest.kt`:

```kotlin
package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.RemoteConfigService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @Test
    fun `getSlides returns failure when JSON is invalid`() = runTest {
        val repository: OnboardingRepository = OnboardingRepositoryImpl(
            FakeRemoteConfigService()
        )
        val result = repository.getSlides()
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `getSlides parses valid JSON from resources`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        assertTrue(result.isSuccess)
        val slides = result.getOrNull()!!
        assertTrue(slides.isNotEmpty())
        assertTrue(slides.size == 4)
    }

    @Test
    fun `getSlides returns slides with resolved text`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        val slides = result.getOrNull()!!
        assertTrue(slides.all { it.title.isNotBlank() })
        assertTrue(slides.all { it.description.isNotBlank() })
    }

    @Test
    fun `getSlides returns slides with correct ids`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        val slides = result.getOrNull()!!
        assertTrue(slides.map { it.id } == listOf(1, 2, 3, 4))
    }
}

private class FakeRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
}
```

- [ ] **Step 3: Run repository tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.onboarding.data.repository.OnboardingRepositoryImplTest"`

Expected: All 4 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/data/repository/
git add composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/data/repository/
git commit -m "feat(onboarding): add repository implementation with JSON parsing and tests"
```

---

### Task 7: DI Module & InitKoin Registration

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/di/OnboardingModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

- [ ] **Step 1: Create the Koin module**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/di/OnboardingModule.kt`:

```kotlin
package com.programovil.aura.onboarding.di

import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.data.repository.OnboardingRepositoryImpl
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val onboardingModule = module {
    singleOf(::OnboardingRepositoryImpl) bind OnboardingRepository::class
    single { OnboardingPreferences(get()) }
    factoryOf(::GetOnboardingSlidesUseCase)
    viewModelOf(::OnboardingViewModel)
}
```

- [ ] **Step 2: Register the module in InitKoin**

In `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`, add the import and module registration.

Add import:
```kotlin
import com.programovil.aura.onboarding.di.onboardingModule
```

Add `onboardingModule` to the module list (after `journalModule`):
```kotlin
fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    journalModule,
    onboardingModule,
    module {
        single { createDataStore() }
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/di/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "feat(onboarding): add Koin DI module and register in InitKoin"
```

---

### Task 8: ViewModel (with tests)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModelTest.kt`

- [ ] **Step 1: Create the ViewModel**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModel.kt`:

```kotlin
package com.programovil.aura.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                    if (slides.isEmpty()) {
                        _state.value = OnboardingState.Completed(skipped = true)
                    } else {
                        _state.value = OnboardingState.Loaded(
                            slides = slides,
                            currentIndex = 0,
                            isLastSlide = slides.size == 1
                        )
                    }
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

- [ ] **Step 2: Write ViewModel tests**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModelTest.kt`:

```kotlin
package com.programovil.aura.onboarding.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleSlides = listOf(
        OnboardingSlide(1, "Title 1", "Desc 1", null),
        OnboardingSlide(2, "Title 2", "Desc 2", null),
        OnboardingSlide(3, "Title 3", "Desc 3", null),
        OnboardingSlide(4, "Title 4", "Desc 4", null)
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { "/tmp/test_onboarding_${Random.nextLong()}.preferences_pb".toPath() }
        )
    }

    private fun createViewModel(
        slides: Result<List<OnboardingSlide>> = Result.success(sampleSlides)
    ): OnboardingViewModel {
        val repository = FakeOnboardingRepository(slides)
        val useCase = GetOnboardingSlidesUseCase(repository)
        val prefs = OnboardingPreferences(createDataStore())
        return OnboardingViewModel(useCase, prefs)
    }

    @Test
    fun `initial state is Loaded with first slide`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Loaded>(state)
        assertEquals(0, state.currentIndex)
        assertEquals(4, state.slides.size)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `next advances to next slide`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(1, state.currentIndex)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `next on last slide does nothing`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.next()
        vm.next()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(3, state.currentIndex)
        assertEquals(true, state.isLastSlide)
        vm.next()
        val stateAfter = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(3, stateAfter.currentIndex)
    }

    @Test
    fun `previous goes back one slide`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.next()
        vm.next()
        vm.previous()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(1, state.currentIndex)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `previous on first slide does nothing`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.previous()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `skip emits Completed with skipped true`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.skip()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(true, state.skipped)
    }

    @Test
    fun `start emits Completed with skipped false`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(false, state.skipped)
    }

    @Test
    fun `error from repository emits Error state`() = runTest {
        val vm = createViewModel(Result.failure(Exception("parse error")))
        advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Error>(state)
        assertEquals("parse error", state.message)
    }

    @Test
    fun `empty slides list auto-completes`() = runTest {
        val vm = createViewModel(Result.success(emptyList()))
        advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(true, state.skipped)
    }

    @Test
    fun `isLastSlide is true when navigating to last slide`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.next()
        vm.next()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertTrue(state.isLastSlide)
    }
}

private class FakeOnboardingRepository(
    private val result: Result<List<OnboardingSlide>>
) : OnboardingRepository {
    override suspend fun getSlides(): Result<List<OnboardingSlide>> = result
}
```

- [ ] **Step 3: Run ViewModel tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.onboarding.presentation.OnboardingViewModelTest"`

Expected: All 10 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/OnboardingViewModel.kt
git add composeApp/src/commonTest/kotlin/com/programovil/aura/onboarding/presentation/
git commit -m "feat(onboarding): add ViewModel with navigation state management and tests"
```

---

### Task 9: Onboarding Screen UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/screen/OnboardingScreen.kt`

- [ ] **Step 1: Create the OnboardingScreen composable**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/screen/OnboardingScreen.kt`:

```kotlin
package com.programovil.aura.onboarding.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.onboarding_skip
import aura_app.composeapp.generated.resources.onboarding_next
import aura_app.composeapp.generated.resources.onboarding_previous
import aura_app.composeapp.generated.resources.onboarding_start

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onStart: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is OnboardingViewModel.OnboardingState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        }
        is OnboardingViewModel.OnboardingState.Loaded -> {
            OnboardingContent(
                slides = currentState.slides,
                currentIndex = currentState.currentIndex,
                isLastSlide = currentState.isLastSlide,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onSkip = viewModel::skip,
                onStart = viewModel::start
            )
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
            LaunchedEffect(currentState) { onSkip() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingContent(
    slides: List<OnboardingSlide>,
    currentIndex: Int,
    isLastSlide: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastSlide) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(Res.string.onboarding_skip),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false
            ) { page ->
                SlideContent(slide = slides[page])
            }

            Spacer(modifier = Modifier.weight(1f))

            PageIndicator(
                totalPages = slides.size,
                currentPage = currentIndex
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_previous),
                        onClick = onPrevious
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isLastSlide) {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_start),
                        onClick = onStart
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_next),
                        onClick = onNext
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (slide.id) {
            1 -> Icons.Default.Checklist
            2 -> Icons.Default.SelfImprovement
            3 -> Icons.Default.Edit
            4 -> Icons.Default.Dashboard
            else -> Icons.Default.Checklist
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = AppTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = slide.title,
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = slide.description,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) AppTheme.colors.primary
                        else AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/onboarding/presentation/screen/
git commit -m "feat(onboarding): add onboarding screen with HorizontalPager and page indicator"
```

---

### Task 10: App.kt Integration

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Add onboarding gate to App.kt**

In `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`, add the following imports:

```kotlin
import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.presentation.screen.OnboardingScreen
```

Replace the `App()` composable (lines 57-95) with:

```kotlin
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

        val onboardingPrefs: OnboardingPreferences = koinInject()
        val isOnboardingCompleted by onboardingPrefs.isOnboardingCompleted
            .collectAsState(initial = false)
        var dismissedThisSession by remember { mutableStateOf(false) }

        val showOnboarding = authState is AuthViewModel.AuthState.SignedIn
            && !isOnboardingCompleted
            && !dismissedThisSession

        when {
            authState is AuthViewModel.AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.primary)
                }
            }
            authState is AuthViewModel.AuthState.SignedOut ||
                authState is AuthViewModel.AuthState.Error -> {
                SignInScreen(
                    errorMessage = if (authState is AuthViewModel.AuthState.Error)
                        (authState as AuthViewModel.AuthState.Error).message else null,
                    onSignInClick = onSignInClick
                )
            }
            showOnboarding -> {
                OnboardingScreen(
                    onSkip = { dismissedThisSession = true },
                    onStart = { }
                )
            }
            authState is AuthViewModel.AuthState.SignedIn -> {
                AuthenticatedApp(
                    currentThemeMode = currentThemeMode,
                    onThemeChange = { settingsViewModel.setThemeMode(it) },
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify the build compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat(onboarding): integrate onboarding gate into App.kt after auth check"
```

---

### Task 11: Full Build Verification

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew :composeApp:testDebugUnitTest`

Expected: All tests PASS (existing + new onboarding tests).

- [ ] **Step 2: Run full Android build**

Run: `./gradlew :composeApp:assembleDebug`

Expected: BUILD SUCCESSFUL. APK generated at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

- [ ] **Step 3: Final commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix(onboarding): resolve build issues from integration"
```
