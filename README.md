# Aura

Productivity app (Todo + Habits + Pomodoro + Agenda) built with Kotlin Multiplatform targeting Android and iOS. 

## Features & Modules

### Implemented:

- **Auth** — Google Sign-In with persistent session support. Abstracted to support platform-native SDKs.
- **Todo** — Full CRUD UI with **Cloud Firestore** persistence. Includes due-date support.
- **Habits** — Strict habit tracking with streak logic and sections (Today, Tomorrow, This Week). Powered by **Room KMP (SQLite)** for high-performance local-only persistence.
- **Notifications** — Local notification scheduling for daily summaries. Abstracted architecture for multi-platform scheduling.
- **Navigation** — Type-safe navigation with **Bottom Navigation** for easy feature access.

### Planned:

- **Pomodoro** — Simple timer for deep focus.
- **Agenda** — Calendar-based overview combining Todos and Habits.

## Tech Stack

| Layer | Technology |
|---|---|
| **Framework** | Kotlin Multiplatform (Android + iOS) |
| **UI** | Compose Multiplatform |
| **Architecture** | Clean Architecture + MVVM |
| **Dependency Injection**| Koin |
| **Local Persistence** | Room Multiplatform (KMP) |
| **Remote Database** | Cloud Firestore |
| **Authentication** | Firebase Auth (Google Sign-In) |
| **Navigation** | Navigation Compose + kotlinx-serialization |
| **Dates & Time** | kotlinx-datetime |
| **Preferences** | DataStore Multiplatform |

## Architecture

The project follows a **Feature-Folder** structure where each feature contains its own Clean Architecture layers. This ensures high modularity and KMP compliance. Shared logic is prioritized in `commonMain`, with platform-specific code limited to minimal `expect`/`actual` declarations.

## Development

### Android
```shell
./gradlew :composeApp:assembleDebug
```

### iOS
Open `iosApp/iosApp.xcworkspace` in Xcode or run via Gradle (if configured).

### Multiplatform Compliance
- No `java.*` imports in `commonMain`.
- Strictly use `kotlinx-datetime` for all date operations.
- Platform-specific builders for Room and DataStore.
