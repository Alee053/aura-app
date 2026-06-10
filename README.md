# Aura

Aplicación de productividad (Todo + Hábitos + Dashboard + Pomodoro + Ajustes) construida con **Kotlin Multiplatform**, con soporte para Android

> Documentación adicional en la carpeta [`docs/`](docs/) (guías técnicas detalladas).

## Características y módulos

### Implementadas

- **Autenticación** — Inicio de sesión con Google y sesión persistente. Abstraído para SDKs nativos de cada plataforma (Android/iOS).
- **Home / Dashboard** — Pantalla de inicio con tarjetas KPI y acceso rápido a todas las funciones.
- **Todo** — CRUD completo con soporte de fecha de vencimiento. Respaldado por **Cloud Firestore** para sincronización entre dispositivos.
- **Hábitos** — Seguimiento estricto de hábitos con rachas (Hoy, Mañana, Esta semana). Respaldado por **Cloud Firestore** para sincronización entre dispositivos.
- **Ajustes** — Selector de tema con 5 paletas (Morado, Verde, Rojo, Oscuro, Alto contraste) persistido con DataStore KMP.
- **Notificaciones** — Programación de notificaciones locales para resúmenes diarios y recordatorios de fecha de vencimiento. Abstraído para multiplataforma.
- **Feature Flags** — Toggles mediante Firebase Remote Config para visibilidad condicional de funciones (Todos, Hábitos, Notificaciones, Journal, Pomodoro, Premium).
- **Navegación** — Bottom Navigation con tipos seguros (Home, Todo, Hábitos, Ajustes, Journal, Pomodoro) usando `kotlinx-serialization`.
- **Sistema de diseño** — Módulo propio `designsystem` con tokens de tema (`AppTheme.colors`, `AppTheme.typography`), componentes reutilizables (`PrimaryButton`, `BasicInput`, `AuraHorizontalDivider`) y 5 paletas de color.
- **Onboarding** — Flujo inicial basado en JSON por idioma, localizado dinámicamente.
- **Journal** — Entradas de diario con sincronización en Firestore.
- **Pomodoro** — Temporizador de enfoque con tres modos (Pomodoro / Pausa corta / Pausa larga) y persistencia del estado.
- **Experimentos A/B** — Infraestructura de experimentación (`Free` vs `Premium`), variantes de Home y frases de motivación.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| **Framework** | Kotlin Multiplatform (Android + iOS) |
| **UI** | Compose Multiplatform |
| **Arquitectura** | Clean Architecture + MVVM/MVI |
| **DI** | Koin 4.1.1 (módulos por feature) |
| **Base de datos remota** | Cloud Firestore + Firebase Realtime Database |
| **Autenticación** | Firebase Auth (Google Sign-In) |
| **Navegación** | Navigation Compose + kotlinx-serialization |
| **Fecha / hora** | kotlinx-datetime |
| **Preferencias** | DataStore KMP |
| **Feature Flags** | Firebase Remote Config |
| **Notificaciones push** | Firebase Cloud Messaging + WorkManager |
| **Internacionalización** | Compose `composeResources` + [Loco](https://localise.biz) |
| **Tareas en segundo plano** | WorkManager (Android)|
| **Testing** | kotlin-test + Turbine + Mockative |

## Estructura del proyecto

```
aura-app/
├── composeApp/           # Módulo KMP: código compartido y específico por plataforma
│   ├── src/
│   │   ├── commonMain/   # Kotlin neutro de plataforma
│   │   ├── commonTest/   # Tests unitarios
│   │   ├── androidMain/  # Implementaciones Android
│   │   └── iosMain/      # Implementaciones iOS
│   └── build.gradle.kts
├── designsystem/         # Módulo de sistema de diseño (colores, tipografía, componentes)
├── iosApp/               # Host nativo iOS (Swift / Xcode)
├── functions/            # Firebase Cloud Functions (TypeScript)
├── docs/                 # Guías y especificaciones técnicas
├── gradle/               # Catálogo de versiones
└── README.md
```

### Features dentro de `composeApp/src/commonMain/`

Cada feature sigue Clean Architecture con sus capas `domain/`, `data/`, `presentation/` y `di/`:

| Feature | Responsabilidad |
|---|---|
| `auth/` | Inicio de sesión con Google y sesión persistente |
| `home/` | Dashboard con KPIs y motivación diaria |
| `todo/` | Lista de tareas con CRUD y fecha de vencimiento |
| `habit/` | Seguimiento de hábitos con rachas y grilla de 7 días |
| `settings/` | Tema, preferencias de notificación y logout |
| `notification/` | Programación de notificaciones locales |
| `onboarding/` | Flujo inicial localizado por idioma |
| `journal/` | Entradas de diario con sincronización en Firestore |
| `pomodoro/` | Temporizador de enfoque con tres modos |
| `experiments/` | A/B testing y plan de usuario (Free / Premium) |
| `shared/` | Infraestructura transversal: feature flags, remote config, DataStore, utilidades de color |

## Arquitectura

El proyecto sigue **Clean Architecture** con modularidad por feature. Cada feature contiene sus propias capas `Domain`, `Data` y `Presentation`. La lógica compartida vive en `commonMain`, con código específico de plataforma limitado a declaraciones `expect`/`actual`.

Reglas clave:
- **Sin imports de `java.*` en `commonMain`**.
- **Fechas exclusivamente con `kotlinx-datetime`**.
- Constructores específicos de plataforma (Room, DataStore, Firebase) mediante `expect`/`actual`.
- **Toda la UI consume `AppTheme.colors` y `AppTheme.typography`** — sin colores ni tamaños de fuente hardcodeados.
- **Sin strings hardcodeados** — todo texto visible al usuario se resuelve desde `strings.xml`.

Guías detalladas disponibles en `docs/`:

- [`docs/KMP_ARCHITECTURE.md`](docs/KMP_ARCHITECTURE.md) — modelo de compilación KMP, source sets, `expect`/`actual`
- [`docs/KOIN_IN_KMP.md`](docs/KOIN_IN_KMP.md) — Inyección de dependencias
- [`docs/NAVIGATION_IN_KMP.md`](docs/NAVIGATION_IN_KMP.md) — Routing con tipos seguros
- [`docs/FIREBASE_IN_KMP.md`](docs/FIREBASE_IN_KMP.md) — Servicios de Firebase
- [`docs/WORKMANAGER_IN_KMP.md`](docs/WORKMANAGER_IN_KMP.md) — Tareas en segundo plano

## Desarrollo

### Requisitos

- **JDK 11** o superior
- **Android SDK** con `compileSdk = 36`, `minSdk = 24`
- **Node 20** (para Firebase Cloud Functions)
- **`curl`** disponible en el PATH (lo usan los scripts de Loco)

### Comandos de build y test

#### Android

```shell
./gradlew :composeApp:assembleDebug           # Build debug
./gradlew :composeApp:testDebugUnitTest       # Tests unitarios
./gradlew :composeApp:connectedAndroidTest    # Tests instrumentados
```

#### Limpieza

```shell
./gradlew clean
```

## Testing

El módulo `composeApp` incluye una suite de tests unitarios JVM en `composeApp/src/commonTest`. La suite completa son **45 clases de test, 178 tests**.

### Cómo correr los tests

```shell
# Toda la suite
./gradlew :composeApp:testDebugUnitTest

# Una sola clase
./gradlew :composeApp:testDebugUnitTest \
    --tests "com.programovil.aura.pomodoro.presentation.PomodoroViewModelTest"

# Un solo método de test
./gradlew :composeApp:testDebugUnitTest \
    --tests "com.programovil.aura.pomodoro.presentation.PomodoroViewModelTest.initial state is a 25-minute idle pomodoro"

# Varias clases (wildcard)
./gradlew :composeApp:testDebugUnitTest \
    --tests "com.programovil.aura.todo.domain.usecase.*"
```

Para corridas locales más rápidas, salta el pull de traducciones de Loco y el hook de `preBuild`:

```shell
./gradlew :composeApp:testDebugUnitTest -x pullTranslations -x preBuild --offline
```

Los reportes HTML quedan en `composeApp/build/reports/tests/testDebugUnitTest/`. Los resultados JUnit XML viven al lado, en `composeApp/build/test-results/testDebugUnitTest/`, y son aptos para que los consuma CI.

### Distribución de los tests

| Capa | Clases de test | Tests |
|---|---|---|
| Presentation — ViewModels | 9 | 66 |
| Domain — use cases | 20 | 50 |
| Máquinas de estado puras / modelos | 5 | 32 |
| Presentation — mappers | 4 | 18 |
| Infraestructura compartida (`*Manager`) | 4 | 11 |
| Contratos de repositorio (sólo Android) | 1 | 1 |
| Helpers compartidos (`FakeRemoteConfigService`) | 2 | — |
| **Total** | **45** | **178** |

La suite cubre todos los use cases de dominio, todos los contratos de repositorio `@Mockable` (vía su use case), todos los ViewModel de presentation, todos los data mappers y los helpers de infraestructura compartida (`MotivationPhraseManager`, `UserPlanManager`, `RemoteConfigValueManager`, `FeatureFlagManager`, `ColorUtils`).

### Convenciones

- **Test doubles** — Las interfaces `@Mockable` se mockean con `mock(of<T>())`; todo lo demás usa fakes hechos a mano (p. ej. `FakeJournalRepository`, `FakeAuthService`, `InMemoryPreferenceDataStore`).
- **Coroutines** — Los tests de ViewModel usan `StandardTestDispatcher` + `Dispatchers.setMain`; los use cases usan `runTest { ... }`. Un `@AfterTest` cancela cada `viewModelScope` creado para que los tickers colgados no bloqueen `runTest`.
- **Flows** — El patrón estándar es `app.cash.turbine.test { awaitItem(); awaitComplete() }`.
- **Naming** — `comportamiento bajo condición` entre comillas invertidas (p. ej. `` `successful dashboard emission clears loading and updates data` ``).

### Cómo agregar un test nuevo

1. Reflejá el layout de paquetes del código de producción (p. ej. fuente en `…/todo/domain/usecase/` → test en `…/todo/domain/usecase/`).
2. Si la dependencia es `@Mockable`, usá `mock(of<T>())`. Si no, extendé la interfaz o escribí un fake chico.
3. Registrá cualquier `ViewModel` creado en el test en una lista dentro de `@AfterTest` y llamá `viewModel.viewModelScope.cancel()` — si no, el test se cuelga en `runTest` esperando un ticker.

## Localización (Loco)

Las traducciones se gestionan en [Loco](https://localise.biz) y se sincronizan con el repositorio mediante dos tareas de Gradle. El proyecto tiene tres idiomas: **`en`** (fuente, commiteado en git), **`es`** y **`fr`**.

Los archivos viven en `composeApp/src/commonMain/composeResources/`:

- `values/strings.xml` — fuente en inglés
- `values-es/strings.xml` — español (generado)
- `values-fr/strings.xml` — francés (generado)

### API key

La variable `LOCO_API_KEY` se resuelve en este orden:

1. Variable de entorno del shell
2. Archivo `.env` en la raíz del repo
3. `gradle.properties`

Una clave de **Export** de sólo lectura alcanza para los `pull`; los `push` requieren una clave de **Full Access**. Conseguila en [Developer Tools → API Keys](https://localise.biz).

### Tareas de Gradle

Ambas viven bajo el grupo `localization` (visible con `./gradlew tasks --group localization`).

| Tarea | Qué hace | Cuándo se ejecuta |
|---|---|---|
| `:composeApp:pullTranslations` | Descarga `es` y `fr` desde Loco y los escribe en `values-es/strings.xml` y `values-fr/strings.xml`. Usa `curl` con un archivo `.tmp` y sólo renombra si el contenido cambió. | **Automática**, está enganchada a `preBuild`, así que corre en cada `./gradlew assemble*`, test o sync del IDE. |
| `:composeApp:pushTranslations` | Sube `values/strings.xml` a Loco como fuente `en`. Las claves nuevas se taggean con `new`; las actualizadas con `source-changed` para detectar drift en el dashboard. Las traducciones existentes de `es`/`fr` **nunca** se eliminan. | Manual, después de editar inglés. |

```shell
./gradlew :composeApp:pushTranslations   # subir inglés a Loco
./gradlew assembleDebug                  # pull automático de es/fr y luego build
```

### Flujo de trabajo para agregar o cambiar un string

1. Edita el texto en inglés en `composeApp/src/commonMain/composeResources/values/strings.xml`.
2. Ejecuta `./gradlew :composeApp:pushTranslations` para subirlo a Loco.
3. En el dashboard de Loco, traduce manualmente o activa la auto-traducción. Las claves nuevas y modificadas ya están pre-tageadas para filtrarlas fácilmente.
4. Ejecuta `./gradlew assembleDebug` (o cualquier otro build) para bajar las últimas traducciones de `es`/`fr` al repo.

### Restricciones importantes

- **Los placeholders printf de Android** (`%1$s`, `%1$d`, `%2$s`, etc.) deben sobrevivir todas las idas y vueltas. Si usás la auto-traducción de Loco, configurá el system prompt de Gemini para que nunca altere ni reordene esos tokens.
- **La fuente en inglés es la fuente de verdad en git**. Los archivos `es` y `fr` commiteados se sobrescriben en cada build, así que cualquier edición manual de esos archivos se perderá.
- Las **claves en inglés faltantes** en el archivo fuente **no se eliminan** de Loco por `pushTranslations` — sólo agrega y actualiza. Esto protege las traducciones a mano de `es`/`fr` contra borrados accidentales.


