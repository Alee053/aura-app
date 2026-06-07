# Journal Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local-only journal feature with Room KMP persistence, list/detail screens, and feature flag gating.

**Architecture:** Feature-scoped `JournalDatabase` using Room KMP. Entity, DAO, and Database in `commonMain`; platform-specific `getJournalDatabaseBuilder()` via `expect`/`actual`. Repository implementation in `commonMain` since Room works cross-platform. Clean architecture: domain → data → presentation → DI.

**Tech Stack:** Room 2.8.4, SQLite Bundled 2.6.2, KSP 2.2.10-2.0.2, Compose Multiplatform, Koin, kotlinx-datetime, kotlinx-serialization.

---

### Task 1: Build Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add Room, SQLite, and KSP versions to `libs.versions.toml`**

In the `[versions]` section, add:
```toml
room = "2.8.4"
sqlite = "2.6.2"
ksp = "2.2.10-2.0.2"
```

In the `[libraries]` section, add:
```toml
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
```

In the `[plugins]` section, add:
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
androidx-room = { id = "androidx.room", version.ref = "room" }
```

- [ ] **Step 2: Add plugins to root `build.gradle.kts`**

Add these two lines to the `plugins` block:
```kotlin
alias(libs.plugins.ksp) apply false
alias(libs.plugins.androidx.room) apply false
```

- [ ] **Step 3: Add plugins and dependencies to `composeApp/build.gradle.kts`**

Add to the `plugins` block:
```kotlin
alias(libs.plugins.ksp)
alias(libs.plugins.androidx.room)
```

Add to `commonMain.dependencies`:
```kotlin
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.sqlite.bundled)
```

Add the `room` block and KSP processor dependencies. Replace the existing `dependencies` block at the bottom:
```kotlin
dependencies {
    debugImplementation(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

- [ ] **Step 4: Verify build configuration compiles**

Run: `./gradlew :composeApp:dependencies --configuration commonMainImplementation`
Expected: Build succeeds, `androidx.room:room-runtime:2.8.4` appears in output.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts composeApp/build.gradle.kts
git commit -m "build: add Room KMP, SQLite, and KSP dependencies"
```

---

### Task 2: Room Data Layer (Entity + DAO + Database)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/entity/JournalEntity.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/dao/JournalDao.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/database/JournalDatabase.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/journal/data/database/JournalDatabase.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/journal/data/database/JournalDatabase.ios.kt`

- [ ] **Step 1: Create `JournalEntity.kt`**

```kotlin
package com.programovil.aura.journal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

- [ ] **Step 2: Create `JournalDao.kt`**

```kotlin
package com.programovil.aura.journal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.programovil.aura.journal.data.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntity?

    @Insert
    suspend fun insert(entry: JournalEntity)

    @Update
    suspend fun update(entry: JournalEntity)

    @Delete
    suspend fun delete(entry: JournalEntity)
}
```

- [ ] **Step 3: Create `JournalDatabase.kt` (commonMain)**

```kotlin
package com.programovil.aura.journal.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programovil.aura.journal.data.dao.JournalDao
import com.programovil.aura.journal.data.entity.JournalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [JournalEntity::class], version = 1)
@ConstructedBy(JournalDatabaseConstructor::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}

@Suppress("KotlinNoActualForExpect")
expect object JournalDatabaseConstructor : RoomDatabaseConstructor<JournalDatabase> {
    override fun initialize(): JournalDatabase
}

expect fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase>

fun getJournalDatabase(builder: RoomDatabase.Builder<JournalDatabase>): JournalDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

- [ ] **Step 4: Create `JournalDatabase.android.kt` (androidMain)**

```kotlin
package com.programovil.aura.journal.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase> {
    val context: Context = object : KoinComponent {
        val ctx: Context = get()
    }.ctx
    val dbFile = context.applicationContext.getDatabasePath("journal.db")
    return Room.databaseBuilder<JournalDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}
```

- [ ] **Step 5: Create `JournalDatabase.ios.kt` (iosMain)**

```kotlin
package com.programovil.aura.journal.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase> {
    val dbFilePath = documentDirectory() + "/journal.db"
    return Room.databaseBuilder<JournalDatabase>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
```

- [ ] **Step 6: Verify KSP generates Room code**

Run: `./gradlew :composeApp:kspDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. KSP generates the `JournalDatabase_Impl` class.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/entity/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/dao/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/database/
git add composeApp/src/androidMain/kotlin/com/programovil/aura/journal/data/database/
git add composeApp/src/iosMain/kotlin/com/programovil/aura/journal/data/database/
git commit -m "feat(journal): add Room entity, DAO, and database with platform builders"
```

---

### Task 3: Domain Layer (Model + Mapper + Repository + Use Cases)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/model/JournalEntry.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/mapper/JournalMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/repository/JournalRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/repository/JournalRepositoryImpl.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/usecase/GetJournalEntriesUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/usecase/GetJournalEntryUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/usecase/AddJournalEntryUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/usecase/UpdateJournalEntryUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/usecase/DeleteJournalEntryUseCase.kt`

- [ ] **Step 1: Create `JournalEntry.kt` (domain model)**

```kotlin
package com.programovil.aura.journal.domain.model

data class JournalEntry(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

- [ ] **Step 2: Create `JournalMapper.kt`**

```kotlin
package com.programovil.aura.journal.data.mapper

import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.domain.model.JournalEntry

fun JournalEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun JournalEntry.toEntity(): JournalEntity = JournalEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
```

- [ ] **Step 3: Create `JournalRepository.kt` (interface)**

```kotlin
package com.programovil.aura.journal.domain.repository

import com.programovil.aura.journal.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getEntries(): Flow<List<JournalEntry>>
    suspend fun getEntry(id: String): JournalEntry?
    suspend fun addEntry(title: String, content: String): Result<Unit>
    suspend fun updateEntry(entry: JournalEntry): Result<Unit>
    suspend fun deleteEntry(entry: JournalEntry): Result<Unit>
}
```

- [ ] **Step 4: Create `JournalRepositoryImpl.kt`**

```kotlin
package com.programovil.aura.journal.data.repository

import com.programovil.aura.journal.data.dao.JournalDao
import com.programovil.aura.journal.data.mapper.toDomain
import com.programovil.aura.journal.data.mapper.toEntity
import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class JournalRepositoryImpl(
    private val dao: JournalDao
) : JournalRepository {

    override fun getEntries(): Flow<List<JournalEntry>> =
        dao.getAllEntries().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEntry(id: String): JournalEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun addEntry(title: String, content: String): Result<Unit> =
        runCatching {
            val now = Clock.System.now().toEpochMilliseconds()
            val entity = JournalEntity(
                id = generateJournalId(),
                title = title,
                content = content,
                createdAt = now,
                updatedAt = now
            )
            dao.insert(entity)
        }

    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> =
        runCatching {
            dao.update(entry.copy(updatedAt = Clock.System.now().toEpochMilliseconds()).toEntity())
        }

    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> =
        runCatching { dao.delete(entry.toEntity()) }
}

private fun generateJournalId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..16).map { chars.random() }.joinToString("")
}
```

- [ ] **Step 5: Create `GetJournalEntriesUseCase.kt`**

```kotlin
package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class GetJournalEntriesUseCase(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.getEntries()
}
```

- [ ] **Step 6: Create `GetJournalEntryUseCase.kt`**

```kotlin
package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository

class GetJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: String): JournalEntry? = repository.getEntry(id)
}
```

- [ ] **Step 7: Create `AddJournalEntryUseCase.kt`**

```kotlin
package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.repository.JournalRepository

class AddJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(title: String, content: String): Result<Unit> =
        repository.addEntry(title, content)
}
```

- [ ] **Step 8: Create `UpdateJournalEntryUseCase.kt`**

```kotlin
package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository

class UpdateJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Unit> =
        repository.updateEntry(entry)
}
```

- [ ] **Step 9: Create `DeleteJournalEntryUseCase.kt`**

```kotlin
package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository

class DeleteJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Unit> =
        repository.deleteEntry(entry)
}
```

- [ ] **Step 10: Verify domain layer compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/domain/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/mapper/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/data/repository/
git commit -m "feat(journal): add domain model, mapper, repository, and use cases"
```

---

### Task 4: DI Module + Registration

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/di/JournalModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

- [ ] **Step 1: Create `JournalModule.kt`**

```kotlin
package com.programovil.aura.journal.di

import com.programovil.aura.journal.data.database.JournalDatabase
import com.programovil.aura.journal.data.database.getJournalDatabase
import com.programovil.aura.journal.data.database.getJournalDatabaseBuilder
import com.programovil.aura.journal.data.repository.JournalRepositoryImpl
import com.programovil.aura.journal.domain.repository.JournalRepository
import com.programovil.aura.journal.domain.usecase.AddJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.DeleteJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntriesUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.UpdateJournalEntryUseCase
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

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
    viewModel { (entryId: String?) ->
        JournalDetailViewModel(entryId, get(), get(), get())
    }
}
```

- [ ] **Step 2: Register `journalModule` in `InitKoin.kt`**

Add import:
```kotlin
import com.programovil.aura.journal.di.journalModule
```

Add `journalModule` to the list in `getModules()`:
```kotlin
fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    journalModule,
    module {
        single { createDataStore() }
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/di/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "feat(journal): add DI module and register in Koin"
```

---

### Task 5: Journal List Presentation (ViewModel + Card + Screen)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/viewmodel/JournalViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/composable/JournalCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/screen/JournalScreen.kt`

- [ ] **Step 1: Create `JournalViewModel.kt`**

```kotlin
package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.DeleteJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class JournalViewModel(
    private val getEntriesUseCase: GetJournalEntriesUseCase,
    private val deleteEntryUseCase: DeleteJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            getEntriesUseCase().collect { entries ->
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            deleteEntryUseCase(entry).onFailure {
                _uiState.value = _uiState.value.copy(error = "Failed to delete entry")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

- [ ] **Step 2: Create `JournalCard.kt`**

```kotlin
package com.programovil.aura.journal.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.journal.domain.model.JournalEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val date = Instant.fromEpochMilliseconds(entry.createdAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            Text(
                text = date.toString(),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Create `JournalScreen.kt`**

```kotlin
package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.journal.presentation.composable.JournalCard
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import com.programovil.aura.shared.FeatureFlag
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.journal_add_entry
import aura_app.composeapp.generated.resources.journal_empty
import aura_app.composeapp.generated.resources.journal_empty_subtitle
import aura_app.composeapp.generated.resources.journal_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigateToDetail: (String?) -> Unit,
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.JOURNAL_ENABLED] == false) {
            onFeatureDisabled()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.journal_title),
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
                onClick = { onNavigateToDetail(null) },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.journal_add_entry),
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
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                uiState.entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(Res.string.journal_empty),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                stringResource(Res.string.journal_empty_subtitle),
                                style = AppTheme.typography.labelLarge,
                                color = AppTheme.colors.textSecondary
                            )
                            PrimaryButton(
                                text = stringResource(Res.string.journal_add_entry),
                                onClick = { onNavigateToDetail(null) }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        items(uiState.entries, key = { it.id }) { entry ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteEntry(entry)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.journal_delete),
                                            color = AppTheme.colors.error,
                                            style = AppTheme.typography.labelLarge,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                JournalCard(
                                    entry = entry,
                                    onClick = { onNavigateToDetail(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/viewmodel/JournalViewModel.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/composable/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/screen/JournalScreen.kt
git commit -m "feat(journal): add list screen with ViewModel, JournalCard, and swipe-to-delete"
```

---

### Task 6: Journal Detail Presentation (ViewModel + Screen)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/viewmodel/JournalDetailViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/screen/JournalDetailScreen.kt`

- [ ] **Step 1: Create `JournalDetailViewModel.kt`**

```kotlin
package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.AddJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.UpdateJournalEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalDetailUiState(
    val entry: JournalEntry? = null,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class JournalDetailViewModel(
    private val entryId: String?,
    private val getEntryUseCase: GetJournalEntryUseCase,
    private val addEntryUseCase: AddJournalEntryUseCase,
    private val updateEntryUseCase: UpdateJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    init {
        if (entryId != null) {
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val entry = getEntryUseCase(id)
            if (entry != null) {
                _uiState.value = JournalDetailUiState(
                    entry = entry,
                    title = entry.title,
                    content = entry.content,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Entry not found"
                )
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun updateContent(value: String) {
        _uiState.value = _uiState.value.copy(content = value)
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val result = if (state.entry != null) {
                updateEntryUseCase(
                    state.entry.copy(
                        title = state.title.trim(),
                        content = state.content.trim()
                    )
                )
            } else {
                addEntryUseCase(state.title.trim(), state.content.trim())
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaved = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Failed to save entry")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

- [ ] **Step 2: Create `JournalDetailScreen.kt`**

```kotlin
package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.journal_back
import aura_app.composeapp.generated.resources.journal_content_hint
import aura_app.composeapp.generated.resources.journal_delete
import aura_app.composeapp.generated.resources.journal_edit_entry
import aura_app.composeapp.generated.resources.journal_new_entry
import aura_app.composeapp.generated.resources.journal_save
import aura_app.composeapp.generated.resources.journal_title_hint
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(
    viewModel: JournalDetailViewModel,
    onNavigateBack: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isNewEntry = uiState.entry == null

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isNewEntry) Res.string.journal_new_entry
                            else Res.string.journal_edit_entry
                        ),
                        style = AppTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.journal_back),
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    if (!isNewEntry && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.journal_delete),
                                tint = AppTheme.colors.textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.saveEntry() },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(Res.string.journal_save),
                            tint = if (uiState.title.isNotBlank()) AppTheme.colors.primary
                            else AppTheme.colors.textSecondary
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(Res.string.journal_title_hint)) },
                singleLine = true,
                textStyle = AppTheme.typography.titleMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textSecondary,
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text(stringResource(Res.string.journal_content_hint)) },
                textStyle = AppTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textSecondary,
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .defaultMinSize(minHeight = 300.dp),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}
```

**Note:** Add the missing import at the top of the file:
```kotlin
import androidx.compose.foundation.layout.defaultMinSize
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/viewmodel/JournalDetailViewModel.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/screen/JournalDetailScreen.kt
git commit -m "feat(journal): add detail screen with ViewModel for create/edit"
```

---

### Task 7: Navigation + Feature Flag + Strings

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-es/strings.xml`

- [ ] **Step 1: Add `JOURNAL_ENABLED` to `FeatureFlags.kt`**

Add to the enum:
```kotlin
JOURNAL_ENABLED("journal_enabled", true),
```

- [ ] **Step 2: Add routes to `NavRoute.kt`**

Add inside the `sealed class NavRoute`:
```kotlin
@Serializable
data object Journal : NavRoute()

@Serializable
data class JournalDetail(val entryId: String? = null) : NavRoute()
```

- [ ] **Step 3: Register destinations in `AppNavHost.kt`**

Add imports:
```kotlin
import com.programovil.aura.journal.presentation.screen.JournalScreen
import com.programovil.aura.journal.presentation.screen.JournalDetailScreen
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
```

Add after the Habits composable block (before Settings):
```kotlin
if (featureFlags[FeatureFlag.JOURNAL_ENABLED] != false) {
    composable<NavRoute.Journal> {
        val journalViewModel = koinViewModel<JournalViewModel>()
        JournalScreen(
            viewModel = journalViewModel,
            onNavigateToDetail = { entryId ->
                navController.navigate(NavRoute.JournalDetail(entryId = entryId))
            },
            featureFlags = featureFlags,
            onFeatureDisabled = {
                navController.popBackStack(NavRoute.Home, inclusive = false)
            }
        )
    }

    composable<NavRoute.JournalDetail> { backStackEntry ->
        val entryId = backStackEntry.toRoute<NavRoute.JournalDetail>().entryId
        val detailViewModel = koinViewModel<JournalDetailViewModel>(
            parameters = { parametersOf(entryId) }
        )
        val journalViewModel = koinViewModel<JournalViewModel>()
        JournalDetailScreen(
            viewModel = detailViewModel,
            onNavigateBack = { navController.popBackStack() },
            onDelete = {
                detailViewModel.uiState.value.entry?.let { entry ->
                    journalViewModel.deleteEntry(entry)
                }
                navController.popBackStack()
            }
        )
    }
}
```

Also add the import for `JournalViewModel`:
```kotlin
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
```

- [ ] **Step 4: Add Journal tab to `App.kt`**

Add import for the journal icon and string:
```kotlin
import androidx.compose.material.icons.filled.Book
import aura_app.composeapp.generated.resources.nav_journal
```

Add after `showHabits` declaration:
```kotlin
val showJournals by remember(featureFlags) {
    mutableStateOf(featureFlags[FeatureFlag.JOURNAL_ENABLED] ?: true)
}
```

Add a new `NavigationBarItem` block after the Habits block and before the Settings block:
```kotlin
if (showJournals) {
    NavigationBarItem(
        icon = { Icon(Icons.Default.Book, contentDescription = "Journal") },
        label = { Text(stringResource(Res.string.nav_journal)) },
        selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Journal>() } == true,
        onClick = {
            navController.navigate(NavRoute.Journal) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        },
        colors = navItemColors
    )
}
```

- [ ] **Step 5: Add string resources to `strings.xml` (English)**

Add before the closing `</resources>` tag:
```xml
<!-- Journal -->
<string name="nav_journal">Journal</string>
<string name="journal_title">Journal</string>
<string name="journal_empty">No journal entries yet</string>
<string name="journal_empty_subtitle">Tap + to write your first entry</string>
<string name="journal_new_entry">New Entry</string>
<string name="journal_edit_entry">Edit Entry</string>
<string name="journal_title_hint">Title</string>
<string name="journal_content_hint">Write your thoughts...</string>
<string name="journal_save">Save</string>
<string name="journal_delete">Delete</string>
<string name="journal_back">Back</string>
<string name="journal_add_entry">Add entry</string>
```

- [ ] **Step 6: Add string resources to `strings.xml` (Spanish)**

Add before the closing `</resources>` tag in `values-es/strings.xml`:
```xml
<!-- Journal -->
<string name="nav_journal">Diario</string>
<string name="journal_title">Diario</string>
<string name="journal_empty">Aún no hay entradas</string>
<string name="journal_empty_subtitle">Toca + para escribir tu primera entrada</string>
<string name="journal_new_entry">Nueva entrada</string>
<string name="journal_edit_entry">Editar entrada</string>
<string name="journal_title_hint">Título</string>
<string name="journal_content_hint">Escribe tus pensamientos...</string>
<string name="journal_save">Guardar</string>
<string name="journal_delete">Eliminar</string>
<string name="journal_back">Volver</string>
<string name="journal_add_entry">Agregar entrada</string>
```

- [ ] **Step 7: Verify full build compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(journal): add navigation, feature flag, and string resources"
```

---

### Task 8: Final Verification

- [ ] **Step 1: Run full Android build**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run existing unit tests**

Run: `./gradlew :composeApp:testDebugUnitTest`
Expected: All existing tests pass. No regressions.

- [ ] **Step 3: Verify Room schema was generated**

Check: `composeApp/schemas/` directory should contain a JSON schema file for `JournalDatabase`.

- [ ] **Step 4: Final commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix(journal): resolve build issues from integration"
```
