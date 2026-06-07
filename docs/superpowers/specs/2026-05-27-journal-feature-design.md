# Journal Feature Design

## Overview

A local-only journal feature for the AURA app. Users can create, view, edit, and delete plain-text journal entries. Data is persisted locally via Room KMP (no Firestore sync). Accessible as a bottom navigation tab gated behind a `JOURNAL_ENABLED` feature flag.

## Architecture Decision

**Feature-scoped JournalDatabase** — the Room database is self-contained within the `journal/` package. This keeps the feature isolated and easily removable. The only shared infrastructure changes are: build dependencies, feature flag enum entry, navigation route, bottom nav tab, and DI module registration.

## Data Model

### JournalEntity (Room)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` | `@PrimaryKey`, UUID string generated on creation |
| `title` | `String` | Entry title |
| `content` | `String` | Plain text body |
| `createdAt` | `Long` | Epoch milliseconds, set on creation, immutable |
| `updatedAt` | `Long` | Epoch milliseconds, updated on every edit |

Table name: `journal_entries`. Database version: 1.

### JournalEntry (Domain Model)

Mirrors `JournalEntity` exactly. Pure data class in `journal/domain/model/`.

## Data Layer

### Package: `journal/data/`

**Entity** (`entity/JournalEntity.kt`): Room `@Entity` annotated data class.

**DAO** (`dao/JournalDao.kt`):
- `@Query("SELECT * FROM journal_entries ORDER BY createdAt DESC") fun getAllEntries(): Flow<List<JournalEntity>>`
- `@Query("SELECT * FROM journal_entries WHERE id = :id") suspend fun getById(id: String): JournalEntity?`
- `@Insert suspend fun insert(entry: JournalEntity)`
- `@Update suspend fun update(entry: JournalEntity)`
- `@Delete suspend fun delete(entry: JournalEntity)`

**Database** (`database/JournalDatabase.kt`):
- `@Database(entities = [JournalEntity::class], version = 1)`
- `@ConstructedBy(JournalDatabaseConstructor::class)`
- `abstract class JournalDatabase : RoomDatabase()` with `abstract fun journalDao(): JournalDao`
- `expect object JournalDatabaseConstructor : RoomDatabaseConstructor<JournalDatabase>`
- `fun getJournalDatabase(builder): JournalDatabase` — common function configuring `BundledSQLiteDriver` + `Dispatchers.IO`

**Platform builders** (`database/`):
- `commonMain`: `expect fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase>`
- `androidMain`: `actual fun getJournalDatabaseBuilder()` — retrieves `Context` from Koin, uses `context.getDatabasePath("journal.db")`
- `iosMain`: `actual fun getJournalDatabaseBuilder()` — uses `NSDocumentDirectory` + `/journal.db`

**Mapper** (`mapper/JournalMapper.kt`): `JournalEntity.toDomain()` and `JournalEntry.toEntity()` extension functions.

**Repository impl** (`repository/JournalRepositoryImpl.kt`): Implements `JournalRepository`, takes `JournalDao`, maps between entity and domain. Lives in `commonMain`.

## Domain Layer

### Package: `journal/domain/`

**Model** (`model/JournalEntry.kt`):
```kotlin
data class JournalEntry(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Repository interface** (`repository/JournalRepository.kt`):
- `fun getEntries(): Flow<List<JournalEntry>>`
- `suspend fun getEntry(id: String): JournalEntry?`
- `suspend fun addEntry(title: String, content: String): Result<Unit>`
- `suspend fun updateEntry(entry: JournalEntry): Result<Unit>`
- `suspend fun deleteEntry(entry: JournalEntry): Result<Unit>`

**Use Cases** (`usecase/`):
- `GetJournalEntriesUseCase(repository)` — `operator fun invoke(): Flow<List<JournalEntry>>`
- `GetJournalEntryUseCase(repository)` — `suspend operator fun invoke(id: String): JournalEntry?`
- `AddJournalEntryUseCase(repository)` — `suspend operator fun invoke(title: String, content: String): Result<Unit>`
- `UpdateJournalEntryUseCase(repository)` — `suspend operator fun invoke(entry: JournalEntry): Result<Unit>`
- `DeleteJournalEntryUseCase(repository)` — `suspend operator fun invoke(entry: JournalEntry): Result<Unit>`

## Presentation Layer

### Package: `journal/presentation/`

**ViewModel** (`viewmodel/JournalViewModel.kt`):
- Dependencies: `GetJournalEntriesUseCase`, `DeleteJournalEntryUseCase`
- State: `JournalUiState(entries: List<JournalEntry>, isLoading: Boolean, error: String?)`
- Collects entries flow in `init`
- Exposes `deleteEntry(entry: JournalEntry)` action

**ViewModel** (`viewmodel/JournalDetailViewModel.kt`):
- Dependencies: `GetJournalEntryUseCase`, `AddJournalEntryUseCase`, `UpdateJournalEntryUseCase`
- State: `JournalDetailUiState(entry: JournalEntry?, title: String, content: String, isLoading: Boolean, isSaved: Boolean)`
- Takes `entryId: String?` — null means new entry
- `saveEntry()` — creates or updates based on whether entryId was provided
- `updateTitle(value: String)`, `updateContent(value: String)`

**Journal List Screen** (`screen/JournalScreen.kt`):
- `Scaffold` with `TopAppBar` (title: "Journal")
- `FloatingActionButton` navigates to `NavRoute.JournalDetail(entryId = null)`
- `LazyColumn` of `JournalCard` composables
- Empty state with prompt to create first entry
- Swipe-to-dismiss on cards for deletion

**Journal Detail Screen** (`screen/JournalDetailScreen.kt`):
- Route parameter: `entryId: String?`
- `Scaffold` with `TopAppBar` containing back arrow, save action, and delete action (edit mode only)
- `OutlinedTextField` for title (single line, `AppTheme.typography.titleMedium`)
- `OutlinedTextField` for content (multi-line, fills remaining space, `AppTheme.typography.bodyLarge`)
- Save button enabled only when title is non-empty

**Composables** (`composable/JournalCard.kt`):
- Card displaying: title, content preview (first 80 chars + ellipsis), formatted date
- Uses `AppTheme.colors.surface` background, `AppTheme.typography.titleMedium` for title, `AppTheme.typography.bodyMedium` for preview, `AppTheme.typography.labelMedium` for date
- `swipeToDismiss` for delete action

## Navigation

**Routes** (added to `NavRoute.kt`):
```kotlin
@Serializable data object Journal : NavRoute()
@Serializable data class JournalDetail(val entryId: String?) : NavRoute()
```

**AppNavHost**: Register `composable<NavRoute.Journal>` and `composable<NavRoute.JournalDetail>` destinations. JournalDetail receives `entryId` from savedStateHandle.

**App.kt**: Add Journal tab to `NavigationBar` with journal icon, gated by `showJournals` feature flag (same pattern as `showTodos`/`showHabits`).

## Feature Flag

Add `JOURNAL_ENABLED` to `FeatureFlags` enum in `shared/FeatureFlags.kt`. Default value: `true`.

## DI Module

`journal/di/JournalModule.kt`:
```kotlin
val journalModule = module {
    single { getJournalDatabase(getJournalDatabaseBuilder()) }
    single { get<JournalDatabase>().journalDao() }
    single<JournalRepository> { JournalRepositoryImpl(get()) }
    factoryOf(::GetJournalEntriesUseCase)
    factoryOf(::GetJournalEntryUseCase)
    factoryOf(::AddJournalEntryUseCase)
    factoryOf(::UpdateJournalEntryUseCase)
    factoryOf(::DeleteJournalEntryUseCase)
    viewModelOf(::JournalViewModel)
    viewModel { (entryId: String?) -> JournalDetailViewModel(entryId, get(), get(), get()) }
}
```

Registered in `InitKoin.kt`'s `getModules()`.

## Build Configuration

### libs.versions.toml additions

```toml
[versions]
room = "2.8.4"
sqlite = "2.6.2"
ksp = "<compatibleKspVersion>"

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
androidx-room = { id = "androidx.room", version.ref = "room" }
```

### composeApp/build.gradle.kts additions

```kotlin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

commonMain.dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
}

// Root-level dependencies block
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

## String Resources

New keys in `strings.xml`:
- `journal_tab` — "Journal"
- `journal_title` — "Journal"
- `journal_empty` — "No journal entries yet"
- `journal_empty_subtitle` — "Tap + to write your first entry"
- `journal_new_entry` — "New Entry"
- `journal_edit_entry` — "Edit Entry"
- `journal_title_hint` — "Title"
- `journal_content_hint` — "Write your thoughts..."
- `journal_save` — "Save"
- `journal_delete` — "Delete"
- `journal_delete_confirm` — "Delete this entry?"
- `journal_back` — "Back"

Spanish translations provided for all keys.

## File Structure

```
journal/
├── data/
│   ├── dao/JournalDao.kt
│   ├── database/
│   │   ├── JournalDatabase.kt          (commonMain)
│   │   ├── JournalDatabase.android.kt  (androidMain)
│   │   └── JournalDatabase.ios.kt      (iosMain)
│   ├── entity/JournalEntity.kt
│   ├── mapper/JournalMapper.kt
│   └── repository/JournalRepositoryImpl.kt
├── di/JournalModule.kt
├── domain/
│   ├── model/JournalEntry.kt
│   ├── repository/JournalRepository.kt
│   └── usecase/
│       ├── GetJournalEntriesUseCase.kt
│       ├── GetJournalEntryUseCase.kt
│       ├── AddJournalEntryUseCase.kt
│       ├── UpdateJournalEntryUseCase.kt
│       └── DeleteJournalEntryUseCase.kt
└── presentation/
    ├── composable/JournalCard.kt
    ├── screen/
    │   ├── JournalScreen.kt
    │   └── JournalDetailScreen.kt
    └── viewmodel/
        ├── JournalViewModel.kt
        └── JournalDetailViewModel.kt
```

## Design System Compliance

- All screens wrapped in `DsTheme` (inherited from App.kt)
- Colors via `AppTheme.colors.*` — no hardcoded colors
- Typography via `AppTheme.typography.*` — no hardcoded font sizes
- Reuses design system components: `PrimaryButton` for empty state CTA
- Uses `AppTheme.colors.surface` for cards, `AppTheme.colors.background` for screens
- Icons use explicit `tint` tokens
