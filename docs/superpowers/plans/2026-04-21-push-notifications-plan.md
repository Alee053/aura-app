# Push Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add push notifications to the Aura todo app — daily summary at configurable time, due date reminders, and Firebase Cloud Messaging for external test notifications via Cloud Functions.

**Architecture:** Local scheduled notifications via WorkManager (reliable, works in Doze mode). External test pushes via FCM Cloud Functions. Notification preferences stored in DataStore.

**Tech Stack:** WorkManager, DataStore Preferences, Firebase Cloud Messaging, Koin DI, Kotlin Multiplatform (Android primary).

---

## File Map

### New Files
```
composeApp/src/commonMain/kotlin/com/programovil/aura/
├── notification/
│   ├── data/
│   │   └── NotificationPreferences.kt
│   ├── di/
│   │   └── NotificationModule.kt
│   ├── presentation/
│   │   ├── viewmodel/
│   │   │   └── NotificationViewModel.kt
│   │   └── worker/
│   │       ├── DueDateNotificationWorker.kt
│   │       └── DailySummaryWorker.kt
│   └── NotificationHelper.kt
functions/
├── src/
│   └── index.ts
├── package.json
└── tsconfig.json
```

### Modified Files
```
composeApp/src/commonMain/kotlin/com/programovil/aura/
├── todo/domain/model/Todo.kt             # Add dueDate field
├── todo/domain/repository/TodoRepository.kt  # Add dueDate param to addTodo
├── todo/data/repository/TodoRepositoryImpl.kt # Persist dueDate to Firestore
├── todo/data/mapper/TodoMapper.kt        # Add dueDate mapping
├── todo/domain/usecase/AddTodoUseCase.kt # Accept dueDate parameter
├── todo/presentation/viewmodel/TodoViewModel.kt # Pass dueDate to use case
├── todo/presentation/screen/TodoScreen.kt # Add notification time picker
├── todo/di/TodoModule.kt                 # Pass NotificationHelper to use cases
├── di/InitKoin.kt                        # Add notificationModule
composeApp/build.gradle.kts               # Add WorkManager, DataStore, Firebase Messaging dependencies
```

---

## Task 1: Add dueDate to Todo Model

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt`

- [ ] **Step 1: Add dueDate field to Todo data class**

```kotlin
package com.programovil.aura.todo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null  // epoch millis, nullable
)
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt
git commit -m "feat: add dueDate field to Todo model"
```

---

## Task 2: Update TodoRepository Interface and AddTodoUseCase

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt`

- [ ] **Step 1: Update TodoRepository to accept dueDate in addTodo**

```kotlin
package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String, dueDate: Long? = null): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}
```

- [ ] **Step 2: Update AddTodoUseCase**

```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(title: String, dueDate: Long? = null): Result<Unit> =
        repository.addTodo(title, dueDate)
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCase.kt
git commit -m "feat: add dueDate param to TodoRepository and AddTodoUseCase"
```

---

## Task 3: Update TodoRepositoryImpl and TodoMapper

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt`

- [ ] **Step 1: Update TodoRepositoryImpl to persist dueDate to Firestore**

```kotlin
package com.programovil.aura.todo.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TodoRepositoryImpl : TodoRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userTodosCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("todos")

    override fun getTodos(): Flow<Result<List<Todo>>> = callbackFlow {
        val listener = userTodosCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val todos = snapshot?.documents?.mapNotNull { doc ->
                    Todo(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        dueDate = doc.getLong("dueDate")
                    )
                } ?: emptyList<Todo>()
                trySend(Result.success(todos))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addTodo(title: String, dueDate: Long?): Result<Unit> = runCatching {
        val data = mutableMapOf(
            "title" to title,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (dueDate != null) {
            data["dueDate"] = dueDate
        }
        userTodosCollection().add(data).await()
    }

    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit> = runCatching {
        userTodosCollection().document(todoId)
            .update("isCompleted", isCompleted).await()
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> = runCatching {
        userTodosCollection().document(todoId).delete().await()
    }
}
```

- [ ] **Step 2: Update TodoMapper to include dueDate**

```kotlin
package com.programovil.aura.todo.data.mapper

import com.programovil.aura.todo.domain.model.Todo

data class TodoData(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val dueDate: Long? = null
)

fun TodoData.toDomain(): Todo = Todo(
    id = id,
    title = title,
    isCompleted = isCompleted,
    dueDate = dueDate
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/mapper/TodoMapper.kt
git commit -m "feat: persist dueDate to Firestore in TodoRepositoryImpl"
```

---

## Task 4: Update TodoViewModel

**Files:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModel.kt`

- [ ] **Step 1: Update TodoViewModel to pass dueDate**

```kotlin
package com.programovil.aura.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    private val getTodosUseCase: GetTodosUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val todos: StateFlow<List<Todo>> = _todos
    val error: StateFlow<String?> = _error
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            getTodosUseCase().collect { result ->
                result.onSuccess { _todos.value = it }
                    .onFailure { _error.value = it.message ?: "Unknown error" }
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String, dueDate: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            addTodoUseCase(title.trim(), dueDate)
                .onFailure { _error.value = "Failed to add todo" }
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleTodoUseCase(todoId, isCompleted)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            deleteTodoUseCase(todoId)
                .onFailure { _error.value = "Failed to delete todo" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModel.kt
git commit -m "feat: add dueDate param to addTodo in TodoViewModel"
```

---

## Task 5: Add Dependencies (WorkManager, DataStore, Firebase Messaging)

**File:** `gradle/libs.versions.toml`

- [ ] **Step 1: Add new library versions**

In `[versions]` section, add:
```toml
workmanager-ktx = "2.9.0"
datastore-preferences = "1.1.0"
firebase-messaging-ktx = "24.0.0"
```

- [ ] **Step 2: Add library entries**

```toml
workmanager-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workmanager-ktx" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore-preferences" }
firebase-messaging-ktx = { module = "com.google.firebase:firebase-messaging-ktx", version.ref = "firebase-messaging-ktx" }
```

- [ ] **Step 3: Update composeApp/build.gradle.kts androidMain dependencies**

Add to `androidMain.dependencies` block:
```kotlin
implementation(libs.workmanager.ktx)
implementation(libs.datastore.preferences)
implementation(libs.firebase.messaging.ktx)
```

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add WorkManager, DataStore, and Firebase Messaging dependencies"
```

---

## Task 6: Create NotificationPreferences (DataStore)

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/data/NotificationPreferences.kt`

- [ ] **Step 1: Create NotificationPreferences**

```kotlin
package com.programovil.aura.notification.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

class NotificationPreferences(private val context: Context) {

    private object Keys {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    val dailySummaryEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] ?: true }

    val notificationHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/data/NotificationPreferences.kt
git commit -m "feat: add NotificationPreferences DataStore for notification settings"
```

---

## Task 7: Create NotificationHelper

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/NotificationHelper.kt`

- [ ] **Step 1: Create NotificationHelper**

```kotlin
package com.programovil.aura.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.programovil.aura.R

object NotificationHelper {

    const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    const val CHANNEL_DUE_DATE_REMINDER = "due_date_reminder"

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dailySummaryChannel = NotificationChannel(
            CHANNEL_DAILY_SUMMARY,
            "Daily Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily summary of your incomplete tasks"
        }

        val dueDateChannel = NotificationChannel(
            CHANNEL_DUE_DATE_REMINDER,
            "Due Date Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for tasks due today"
        }

        notificationManager.createNotificationChannels(listOf(dailySummaryChannel, dueDateChannel))
    }

    fun showDailySummaryNotification(context: Context, incompleteCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val body = if (incompleteCount > 0) {
            "You have $incompleteCount incomplete task${if (incompleteCount > 1) "s" else ""}"
        } else {
            "All tasks complete! Great job!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification) // placeholder - see Step 2 note
            .setContentTitle("Daily Summary")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_SUMMARY, notification)
    }

    fun showDueDateNotification(context: Context, todoTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_DATE_REMINDER)
            .setSmallIcon(R.drawable.ic_notification) // placeholder - see Step 2 note
            .setContentTitle("Task Due Today")
            .setContentText(todoTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DUE_DATE, notification)
    }

    private const val NOTIFICATION_ID_DAILY_SUMMARY = 1001
    private const val NOTIFICATION_ID_DUE_DATE = 1002
}
```

> **Note on icons:** You need a notification icon. Create `composeApp/src/androidMain/res/drawable/ic_notification.xml` (a simple vector drawable) or use an existing icon. The app will compile without it but the notification won't show properly until an icon exists.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/NotificationHelper.kt
git commit -m "feat: add NotificationHelper for creating and showing notifications"
```

---

## Task 8: Create DueDateNotificationWorker

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/worker/DueDateNotificationWorker.kt`

- [ ] **Step 1: Create DueDateNotificationWorker**

```kotlin
package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper

class DueDateNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val todoTitle = inputData.getString(KEY_TODO_TITLE) ?: return Result.failure()
        NotificationHelper.showDueDateNotification(applicationContext, todoTitle)
        return Result.success()
    }

    companion object {
        const val KEY_TODO_TITLE = "todo_title"
        const val WORK_NAME_PREFIX = "due_date_reminder_"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/worker/DueDateNotificationWorker.kt
git commit -m "feat: add DueDateNotificationWorker for due date reminders"
```

---

## Task 9: Create DailySummaryWorker

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/worker/DailySummaryWorker.kt`

- [ ] **Step 1: Create DailySummaryWorker**

```kotlin
package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.tasks.await

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseConfig.auth.currentUser?.uid
            if (userId == null) {
                return Result.failure()
            }

            val snapshot = FirebaseConfig.firestore
                .collection("users").document(userId).collection("todos")
                .whereEqualTo("isCompleted", false)
                .get()
                .await()

            val incompleteCount = snapshot.size()
            NotificationHelper.showDailySummaryNotification(applicationContext, incompleteCount)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "daily_summary_work"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/worker/DailySummaryWorker.kt
git commit -m "feat: add DailySummaryWorker for daily summary notifications"
```

---

## Task 10: Create NotificationViewModel and NotificationModule

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/viewmodel/NotificationViewModel.kt`

- [ ] **Step 1: Create NotificationViewModel**

```kotlin
package com.programovil.aura.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.presentation.worker.DailySummaryWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationViewModel(
    private val notificationPreferences: NotificationPreferences,
    private val workManager: WorkManager
) : ViewModel() {

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _hour = MutableStateFlow(8)
    val hour: StateFlow<Int> = _hour

    private val _minute = MutableStateFlow(0)
    val minute: StateFlow<Int> = _minute

    init {
        viewModelScope.launch {
            notificationPreferences.dailySummaryEnabled.collect { _isEnabled.value = it }
        }
        viewModelScope.launch {
            notificationPreferences.notificationHour.collect { _hour.value = it }
        }
        viewModelScope.launch {
            notificationPreferences.notificationMinute.collect { _minute.value = it }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setDailySummaryEnabled(enabled)
            if (enabled) {
                scheduleDailySummary()
            } else {
                cancelDailySummary()
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            notificationPreferences.setNotificationTime(hour, minute)
            if (_isEnabled.value) {
                scheduleDailySummary()
            }
        }
    }

    fun scheduleDailySummary() {
        viewModelScope.launch {
            val currentHour = _hour.value
            val currentMinute = _minute.value

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, currentHour)
                set(Calendar.MINUTE, currentMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DailySummaryWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_RECREATE,
                workRequest
            )
        }
    }

    private fun cancelDailySummary() {
        workManager.cancelUniqueWork(DailySummaryWorker.WORK_NAME)
    }
}
```

- [ ] **Step 2: Create NotificationModule**

```kotlin
package com.programovil.aura.notification.di

import androidx.work.WorkManager
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    singleOf(::NotificationPreferences).bind<NotificationPreferences>()
    single { WorkManager.getInstance(androidContext()) }
    singleOf(::NotificationHelper)
    viewModel { NotificationViewModel(get(), get()) }
}
```

- [ ] **Step 3: Update InitKoin to include notificationModule**

```kotlin
package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.todo.di.todoModule

fun getModules() = listOf(
    authModule,
    todoModule,
    notificationModule
)
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/notification/presentation/viewmodel/NotificationViewModel.kt composeApp/src/commonMain/kotlin/com/programovil/aura/notification/di/NotificationModule.kt composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "feat: add NotificationViewModel, NotificationModule, and integrate into InitKoin"
```

---

## Task 11: Add Notification Time Picker to TodoScreen

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Read current TodoScreen**

```kotlin
// Read the current TodoScreen to understand its structure
// (You should see the file content before editing)
```

- [ ] **Step 2: Add notification settings UI to TodoScreen**

Add an inline notification settings section at the bottom of the screen. This will include:
- A row with "Daily reminder" label and a toggle switch
- A time display (e.g., "08:00 AM") that opens a time picker when tapped

The implementation uses `kotlinx.datetime` for time handling on commonMain, but the actual time picker uses Android's `TimePickerDialog` via a platform-specific approach or a Compose `TimePicker` composable.

```kotlin
// Add to the bottom of TodoScreen, before the closing brace of the main Column

// Notification settings section
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "Daily reminder",
        style = MaterialTheme.typography.bodyLarge
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%02d:%02d", notificationHour, notificationMinute),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = isNotificationEnabled,
            onCheckedChange = { viewModel.setNotificationEnabled(it) }
        )
    }
}
```

> **Note:** This is a simplified representation. The actual implementation needs to integrate with the NotificationViewModel, handle platform-specific time picker (using AndroidAlertDialog or similar), and use `rememberLauncherForActivityResult` for the time picker result.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt
git commit -m "feat: add notification time picker to TodoScreen"
```

---

## Task 12: Create Firebase Messaging Service

**File:** `composeApp/src/androidMain/kotlin/com/programovil/aura/FirebaseMessagingService.kt`

- [ ] **Step 1: Create Firebase Messaging Service**

```kotlin
package com.programovil.aura

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.programovil.aura.notification.NotificationHelper

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Test Notification"
        val body = remoteMessage.notification?.body ?: "This is a test push notification"

        NotificationHelper.createNotificationChannels(this)
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Optionally: send token to your backend for targeted notifications
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        val notification = androidx.core.app.NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification) // placeholder
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

- [ ] **Step 2: Register in AndroidManifest.xml**

```xml
<!-- Add inside <application> tag -->
<service
    android:name=".FirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

- [ ] **Step 3: Subscribe to test topic on app start (in AndroidApp.kt)**

Add to `AndroidApp.kt` `onCreate()`:
```kotlin
FirebaseConfig.messaging.subscribeToTopic("test-notifications")
    .addOnCompleteListener { /* handle result if needed */ }
```

Note: You'll need to add `firebase-messaging-ktx` to the `FirebaseConfig` object.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/FirebaseMessagingService.kt
git commit -m "feat: add FirebaseMessagingService for handling FCM push notifications"
```

---

## Task 13: Cloud Functions for Test Notifications

**Directory:** `functions/` (create if not existing)

- [ ] **Step 1: Create functions/package.json**

```json
{
  "name": "aura-notification-functions",
  "version": "1.0.0",
  "main": "lib/index.js",
  "scripts": {
    "build": "tsc",
    "serve": "npm run build && firebase emulators:start --only functions",
    "shell": "npm run build && firebase functions:shell",
    "start": "npm run shell"
  },
  "engines": {
    "node": "20"
  },
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^5.0.0"
  },
  "devDependencies": {
    "typescript": "^5.0.0"
  }
}
```

- [ ] **Step 2: Create functions/tsconfig.json**

```json
{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2020"
  },
  "compileOnSave": true,
  "include": ["src"]
}
```

- [ ] **Step 3: Create functions/src/index.ts**

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

export const sendTestNotification = functions.https.onRequest(async (req, res) => {
  // CORS headers for local testing
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  if (req.method !== 'POST') {
    res.status(405).send('Method not allowed');
    return;
  }

  const { title = 'Test Notification', body = 'This is a test push notification' } = req.body;

  try {
    await admin.messaging().send({
      notification: { title, body },
      topic: 'test-notifications'
    });
    res.status(200).json({ success: true, message: 'Notification sent' });
  } catch (error) {
    console.error('Error sending notification:', error);
    res.status(500).json({ success: false, error: 'Failed to send notification' });
  }
});
```

- [ ] **Step 4: Install and build**

```bash
cd functions && npm install && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add functions/
git commit -m "feat: add Cloud Functions for test notification sending"
```

---

## Task 14: Update TodoScreen Add Dialog to Include Due Date Picker

**File:** `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Update TodoScreen to include due date when adding todos**

The add dialog should include an optional due date picker (date only, time uses the user's configured notification time).

This requires:
- Adding a "Due date" row in the add dialog with a date picker button
- Storing the selected date as epoch millis at 12:00 PM (noon) of that day, so the notification fires at the user's configured time on the due date

> **Note:** The TodoScreen will need to be updated to pass dueDate when calling `viewModel.addTodo(title, dueDate)`. The current add dialog is a simple AlertDialog — it needs a new field for due date selection.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt
git commit -m "feat: add due date picker to todo add dialog"
```

---

## Verification

After all tasks:
1. Run `./gradlew build` to verify Android build
2. Test notification permission on a device
3. Test daily summary scheduling by setting time to 1 minute ahead and waiting
4. Test Cloud Function via `firebase functions:shell` → `sendTestNotification({ body: { title: 'Test', body: 'Hello' } })`
5. Verify notification channels appear in Android settings

---

## Spec Coverage Check

| Spec Requirement | Tasks |
|-------------------|-------|
| Daily summary at configurable time | Tasks 5, 6, 9, 10, 11 |
| Due date reminders | Tasks 1-4, 8, 10, 14 |
| External test notification via FCM | Tasks 5, 12, 13 |
| Notification channels | Tasks 7, 12 |
| User-configurable time (inline in TodoScreen) | Tasks 10, 11 |
| Todo model dueDate field | Tasks 1, 2, 3, 4 |
| Cloud Functions for test push | Task 13 |