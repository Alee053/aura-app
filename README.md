# Aura

Productivity app (Todo + Habits + Dashboard + Settings) built with Kotlin Multiplatform targeting Android and iOS.

## Features & Modules

### Implemented

- **Auth** — Google Sign-In with persistent session. Abstracted for platform-native SDKs (Android/iOS).
- **Home/Dashboard** — Overview screen with KPI cards and quick access to all features.
- **Todo** — Full CRUD with due-date support. Backed by **Cloud Firestore** for cross-device sync.
- **Habits** — Strict habit tracking with streaks (Today, Tomorrow, This Week). Backed by **Room KMP (SQLite)**.
- **Settings** — Theme switcher with 5 palettes (Purple, Green, Red, Dark, High Contrast) persisted via DataStore KMP.
- **Notifications** — Local notification scheduling for daily summaries and due-date reminders. Abstracted for multi-platform.
- **Feature Flags** — Firebase Remote Config toggles for conditional feature visibility (Todos, Habits, Notifications).
- **Navigation** — Type-safe Bottom Navigation (Home, Todo, Habit, Settings) with `kotlinx-serialization`.
- **Design System** — Custom `designsystem` module with theme tokens (`AppTheme.colors`, `AppTheme.typography`), reusable components (PrimaryButton, BasicInput, AuraHorizontalDivider), and 5 color palettes.

### Planned

- **Pomodoro** — Focus timer
- **Agenda** — Calendar overview combining Todos and Habits

## Tech Stack

| Layer | Technology |
|---|---|
| **Framework** | Kotlin Multiplatform (Android + iOS) |
| **UI** | Compose Multiplatform |
| **Architecture** | Clean Architecture + MVVM |
| **DI** | Koin (per-feature modules) |
| **Local DB** | Room KMP (SQLite) |
| **Remote DB** | Cloud Firestore |
| **Auth** | Firebase Auth (Google Sign-In) |
| **Navigation** | Navigation Compose + kotlinx-serialization |
| **Date/Time** | kotlinx-datetime |
| **Preferences** | DataStore KMP |
| **Feature Flags** | Firebase Remote Config |
| **Push Notifications** | Firebase Cloud Messaging + WorkManager |
| **Error Tracking** | Sentry |
| **Testing** | kotlin-test + Turbine + Mockative |

## Architecture

The project follows **Clean Architecture** with per-feature modularity. Each feature contains its own Domain, Data, and Presentation layers. Shared logic lives in `commonMain`, with platform-specific code limited to `expect`/`actual` declarations.

See [`AGENTS.md`](AGENTS.md) for the full architecture specification and [`docs/`](docs/) for detailed guides:
- [`docs/KMP_ARCHITECTURE.md`](docs/KMP_ARCHITECTURE.md) — KMP compilation model, source sets, `expect`/`actual`
- [`docs/guides/KOIN_IN_KMP.md`](docs/guides/KOIN_IN_KMP.md) — Dependency injection
- [`docs/guides/NAVIGATION_IN_KMP.md`](docs/guides/NAVIGATION_IN_KMP.md) — Type-safe routing
- [`docs/guides/FIREBASE_IN_KMP.md`](docs/guides/FIREBASE_IN_KMP.md) — Firebase services
- [`docs/guides/WORKMANAGER_IN_KMP.md`](docs/guides/WORKMANAGER_IN_KMP.md) — Background tasks

## Development

### Android
```shell
./gradlew :composeApp:assembleDebug           # Build
./gradlew :composeApp:testDebugUnitTest       # Unit tests
./gradlew :composeApp:connectedAndroidTest    # Instrumented tests
```

### iOS
Open `iosApp/iosApp.xcworkspace` in Xcode.

### Firebase Functions
```shell
cd functions && npm run build && firebase deploy --only functions
```

### Multiplatform Compliance
- No `java.*` imports in `commonMain`.
- Strictly use `kotlinx-datetime` for all date operations.
- Platform-specific builders for Room, DataStore, and Firebase via `expect`/`actual`.
- All UI consumes `AppTheme.colors` and `AppTheme.typography` tokens — no hardcoded colors or font sizes.
