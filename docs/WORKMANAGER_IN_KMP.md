# WorkManager for Background Tasks in KMP

WorkManager is the recommended solution for deferrable, reliable background work on Android.

## Dependencies

```toml
# libs.versions.toml
[versions]
workRuntimeKtx = "2.9.0"

[libraries]
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workRuntimeKtx" }
```

```kotlin
// androidMain.dependencies
implementation(libs.androidx.work.runtime.ktx)
```

## Worker Types

| Type | Use Case |
|------|----------|
| **Immediate** | Tasks that must start now and complete quickly |
| **Long-running** | Tasks lasting >10 minutes |
| **Deferrable** | Scheduled tasks that start later or run periodically |

## Worker Implementation

Use `CoroutineWorker` for suspend-based work:

```kotlin
class LogUploadWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        // Perform background work
        return Result.success() // or Result.failure(), Result.retry()
    }
}
```

## Scheduling with Koin DI

Inject use cases via Koin's `KoinComponent`:

```kotlin
class LogUploadWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val useCase: MyUseCase by inject()

    override suspend fun doWork(): Result {
        val response = useCase.invoke()
        return response.fold(
            onFailure = { Result.failure() },
            onSuccess = { Result.success() }
        )
    }
}
```

## Periodic Work with Constraints

```kotlin
class LogScheduler(private val context: Context) {
    fun schedulePeriodicUpload() {
        val request = PeriodicWorkRequest.Builder(
            LogUploadWorker::class.java,
            15L, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                "logUploadWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}
```

## API Comparison

| API | Best For | Notes |
|-----|----------|-------|
| **Coroutines** | Async work that doesn't need to persist if app is closed | Standard for leaving main thread; stops when app closes |
| **AlarmManager** | Exact alarms only | Wakes device from Doze; inefficient for recurring work |
| **WorkManager** | Deferrable reliable work | Persists across app restarts and device reboots |
