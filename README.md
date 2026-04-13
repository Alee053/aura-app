# Aura

Productivity app (Todo + Habits + Pomodoro + Agenda) built with Kotlin Multiplatform targeting Android and iOS. Online-first with Cloud Firestore persistence.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Kotlin Multiplatform (Android + iOS) |
| UI | Jetpack Compose Multiplatform |
| Architecture | Clean Architecture (Domain/Data/Presentation) + MVVM |
| DI | KOIN |
| Remote DB | Cloud Firestore (Firebase) |
| Auth | Firebase Auth (Google Sign-In) |
| Navigation | Navigation Compose + kotlinx-serialization |

## Modules

Implemented:

- **Auth** — Google Sign-In via Firebase
- **Todo** — CRUD with Firestore persistence, full Clean Architecture

Planned:

- **Habits** — Daily/weekly habit tracking with streaks
- **Pomodoro** — Simple timer (no cloud persistence)
- **Agenda** — Calendar events, potential Todo integration

## Architecture

Each module follows Clean Architecture with domain/data/presentation layers. Firestore path pattern per-user: `/users/{uid}/{module}/{docId}`.

## Build

```shell
./gradlew :composeApp:assembleDebug   # Android
```