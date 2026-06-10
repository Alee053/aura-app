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

### Localization (Loco)

Translations are managed in [Loco](https://localise.biz) and synced to the repo via two Gradle tasks. The project has three locales: **`en`** (source, in git), **`es`**, and **`fr`**.

#### API key

Set `LOCO_API_KEY` once, picked up in this order:
1. Shell environment variable
2. `.env` file at the repo root (gitignored — copy `.env.example` to get started)
3. `gradle.properties`

A read-only **Export** key is sufficient for pulls; pushes need a **Full Access** key. Get one under [Developer Tools → API Keys](https://localise.biz).

#### Tasks

| Task | What it does | When to run |
|---|---|---|
| `:composeApp:pullTranslations` | Downloads `es` and `fr` from Loco into `values-es/strings.xml` and `values-fr/strings.xml`. Runs automatically on every `preBuild` (i.e. every `./gradlew assemble*`, test, IDE sync). | Automatic. |
| `:composeApp:pushTranslations` | Uploads `values/strings.xml` to Loco as the `en` source. New keys get tagged `new`; updated keys get tagged `source-changed` so you can spot drift in the Loco dashboard. Existing `es`/`fr` translations are never deleted. | Run manually after editing English. |

```shell
./gradlew :composeApp:pushTranslations   # upload English to Loco
./gradlew assembleDebug                  # pulls es/fr automatically, then builds
```

#### Workflow for adding or changing a string

1. Edit the English text in `composeApp/src/commonMain/composeResources/values/strings.xml`.
2. Run `./gradlew :composeApp:pushTranslations` to upload it to Loco.
3. In the Loco dashboard, either translate manually or trigger auto-translation. New and changed keys are pre-tagged for easy filtering.
4. Run `./gradlew assembleDebug` (or any other build) to pull the latest `es`/`fr` translations back into the repo.

#### Important constraints

- **Android printf placeholders** (`%1$s`, `%1$d`, `%2$s`, etc.) must survive every round trip. If you use Loco's machine translation, configure the Gemini system prompt to never alter or reorder these tokens.
- The **English source is the source of truth** in git. `es` and `fr` files in git are overwritten on every build, so any hand-edits to those files will be lost.
- Missing English keys in the source file are **not** deleted from Loco by `pushTranslations` — it only adds and updates. This protects your hand-tuned `es`/`fr` translations from accidental wipes.
