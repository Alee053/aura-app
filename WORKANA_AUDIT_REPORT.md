# Aura — Workana Portfolio Audit

**Fecha de auditoría:** 2026-09-03  
**Repositorio auditado:** C:\Proyectos\aura-app  
**Rama observada:** master  
**HEAD observado:** 5e29bc0 (fix: versioning)  
**Alcance:** código y configuración presentes en el checkout actual, incluyendo cambios locales sin commit.

## Criterio de lectura

Este documento parte de una auditoría estática: se inspeccionaron estructura, código Kotlin, Compose, expect/actual, Gradle, recursos, Manifest, configuración Firebase visible, Functions, tests y Git. Después se hizo una validación operativa acotada del wrapper/daemon de Gradle y de la configuración del proyecto; la compilación Android no pudo completarse porque el entorno actual no tiene un Android SDK instalado en la ruta declarada. No se ejecutaron tests, lint, emuladores, Xcode, tareas de traducción ni servicios externos; por tanto, una implementación localizada en el código se describe como evidencia estática y no como una verificación de ejecución en dispositivo.

Se utilizan estas distinciones:

- **Implementado/verificado en código:** existe una ruta funcional identificable en el código fuente.
- **Configurado:** aparece una dependencia, plugin o archivo de configuración, pero no se encontró uso funcional suficiente.
- **Parcial:** hay una parte real y otra ausente, stub o no conectada.
- **No evidenciado:** la auditoría no encontró implementación en el repositorio; no significa que no exista fuera de él.

El informe evita convertir una dependencia en una funcionalidad, y evita atribuir usuarios, clientes, métricas, resultados de negocio o pruebas exitosas que no están demostrados por el repositorio.

## 1. Resumen ejecutivo

Aura sí contiene evidencia de una aplicación Android real y de alcance mayor que una pantalla de demostración. El núcleo observable es una aplicación de productividad Android-first construida con Kotlin Multiplatform y Compose Multiplatform, organizada por features y capas, con:

- autenticación Android mediante Google Credential Manager, Google ID Token y Firebase Authentication;
- datos de usuario en Cloud Firestore para tareas, hábitos y diario, usando listeners reactivos;
- dashboard con agregación de tareas, hábitos y rachas;
- onboarding localizado desde JSON;
- tareas, hábitos recurrentes, diario y temporizador Pomodoro;
- preferencias locales mediante DataStore;
- notificaciones programadas con WorkManager, notificaciones locales y recepción de FCM;
- feature flags y variantes Free/Premium mediante Remote Config;
- una capa de diseño compartida con cinco paletas y tipografía propia;
- una suite estática de 45 clases y 178 anotaciones @Test en commonTest.

La formulación comercial defendible es: **“MVP/prototipo funcional Android-first de productividad, con arquitectura multiplataforma preparada, Firebase y lógica de estado persistente”**. La evidencia permite presentar trabajo de arquitectura, integración, persistencia y lifecycle; no permite presentarlo como producto de producción validado, plataforma iOS terminada u offline-first.

La principal conclusión técnica es que el trabajo fuerte está en Android y en el código compartido. El target iOS existe como host y framework, pero autenticación, repositorios de tareas/hábitos/diario, Remote Config y experimentos tienen stubs o implementaciones no persistentes. En iOS, además, el flujo inicial de autenticación puede permanecer en Loading porque el listener es no-op y getCurrentAuthState() no se invoca.

### Nivel de evidencia para portfolio

| Área | Lectura estática | Lectura comercial recomendada |
|---|---|---|
| Producto Android | Alto: hay navegación, estados, datos, servicios y UI de varias features | Presentarlo como aplicación Android real, no como mockup |
| Arquitectura | Alto: KMP, CMP, DI, capas y expect/actual visibles | Destacar diseño técnico y separación de responsabilidades |
| Integración cloud | Medio-alto en Android: Auth, Firestore, RTDB, Remote Config y FCM aparecen conectados | Hablar de integración implementada en Android; no prometer backend auditado |
| Persistencia | Medio-alto: Firestore y DataStore están implementados en Android | No llamarla offline-first ni atribuirle Room |
| Calidad automatizada | Medio: muchos unit tests, sin ejecución en esta auditoría | Decir “incluye suite unitaria”; no decir “178 tests pasando” |
| iOS | Bajo/parcial: host y notificaciones reales, datos/auth principales en stubs | Declarar “arquitectura preparada / implementación iOS parcial” |
| Release/producción | Bajo o no verificable | No usar “production-ready” sin validación externa |

## 2. Problema y propósito de la aplicación

El propósito se infiere de los modelos, pantallas, textos de onboarding y casos de uso: ayudar a una persona a organizar tareas, mantener hábitos, escribir reflexiones, trabajar por intervalos y consultar un resumen de actividad desde un panel central.

La propuesta funcional observable es:

1. capturar trabajo pendiente en tareas;
2. convertir comportamientos repetidos en hábitos con recurrencia y progreso;
3. registrar entradas de diario;
4. enfocar sesiones de trabajo con Pomodoro;
5. revisar una síntesis de tareas, hábitos y rachas en Home;
6. recibir recordatorios o resúmenes locales;
7. adaptar contenido, tema y visibilidad de features por configuración remota.

Esto da una narrativa comercial coherente de productividad personal. No se encontró evidencia de un brief de cliente, usuarios reales, métricas de retención, validación de mercado, monetización implementada, cuentas de empresa, colaboración entre usuarios o un módulo independiente de objetivos/metas. El término “objetivos” no debe usarse como feature implementada: no hay entidad, repositorio, pantalla ni caso de uso Goal/Objective.

## 3. Funcionalidades para el usuario

### Funcionalidades verificadas en código

| Funcionalidad | Qué puede hacer el usuario según el código | Estado y límites observables |
|---|---|---|
| Acceso | Iniciar sesión con Google en Android | Flujo real Android; iOS no implementado |
| Sesión | Ver una pantalla de carga, acceso, error o app autenticada | La persistencia la administra Firebase Auth; no hay modelo propio de sesión |
| Onboarding | Recorrer cuatro slides: tareas, hábitos, diario y dashboard; avanzar, volver, saltar o comenzar | Configuración JSON localizada; image_url está vacío y se usan iconos por ID |
| Home/dashboard | Ver tareas incompletas, resumen de hábitos y mayor racha; variante de motivación para Premium | El dashboard reduce fallos de fuentes a valores por defecto y no muestra un estado de error propio |
| Tareas | Crear, editar, completar/descompletar, borrar, añadir descripción opcional y fecha de vencimiento | No hay prioridad, etiquetas, subtareas, búsqueda, filtros, orden configurable ni calendario |
| Hábitos | Crear/editar, seleccionar Daily/Weekly/Monthly, objetivo semanal/mensual, color, completar fechas y ver últimos siete días/racha | El acceso exige HABITS_ENABLED y plan Premium; el plan por defecto es Free |
| Diario | Crear, editar, borrar, listar por createdAt, abrir detalle y borrar con swipe | Texto plano; no hay adjuntos, rich text, búsqueda, exportación, confirmación o undo |
| Pomodoro | Elegir 5/10/25 minutos, iniciar, pausar, reiniciar, saltar sesión y encadenar descansos | La máquina de estado persiste y restaura el temporizador; no asocia sesiones a tareas ni genera estadísticas |
| Notificaciones | Activar resumen diario, configurar hora, probar notificación y recibir alertas de Pomodoro/fecha | Textos del helper Android están en inglés y la frecuencia Premium depende de configuración/plan |
| Temas | Seleccionar Morado, Verde, Rojo, Oscuro o Alto contraste | Persistidos en DataStore; no existe selector de modo claro separado |
| Localización | Recursos en, es, fr; onboarding resuelve el locale del sistema y hace fallback a inglés | Las claves de los tres XML son equivalentes; la auditoría no ejecutó la UI para validar traducciones visuales |
| Feature flags | Activar/desactivar Todo, Journal, Pomodoro, Hábitos y marcar Premium | Remote Config real en Android; el usuario no tiene compra ni entitlement de tienda |

### Lo que no debe presentarse como implementado

- objetivos/metas independientes;
- calendario del dispositivo o sincronización con Google Calendar;
- búsqueda global o búsqueda dentro de tareas/diario;
- filtros avanzados, etiquetas o prioridad de tareas;
- pantalla de estadísticas históricas, gráficos de productividad o analytics de producto;
- pagos, suscripciones, billing o gestión segura de entitlement Premium;
- colaboración, compartir, perfiles públicos o multiusuario;
- sincronización offline-first explícita, resolución de conflictos o cola de operaciones;
- paridad funcional completa en iOS.

## 4. Stack tecnológico verificado

La tabla siguiente distingue el stack declarado del uso real localizado.

| Tecnología | Versión/configuración observada | Dónde aparece | Uso verificable |
|---|---|---|---|
| Kotlin | 2.2.10 | gradle/libs.versions.toml | Lenguaje de commonMain, androidMain, iosMain, tests y capa Compose compartida |
| Kotlin Multiplatform | Plugin KMP; targets Android e iOS | composeApp/build.gradle.kts, settings.gradle.kts | Comparte dominio, presentación Compose, modelos, casos de uso y DataStore; usa expect/actual |
| Compose Multiplatform | 1.10.3 | composeApp y designsystem | Toda la UI común se implementa con composables; no se localizaron layouts XML |
| Material 3 | 1.10.0-alpha05 | catálogo y archivos de UI | Scaffold, TopAppBar, NavigationBar, Card, Dialog, DatePicker, Snackbar, Switch, Checkbox, etc. |
| Navigation Compose | 2.9.2 | AppNavHost.kt, NavRoute.kt | Navegación tipada con clases serializables, NavHost y rutas condicionales |
| Kotlin Serialization | 1.8.0 | rutas, onboarding, Remote Config | Serializa rutas y decodifica onboarding_config.json y frases JSON |
| Koin | 4.1.1 | módulos */di, InitKoin.kt | Inyección de repositorios, casos de uso, servicios y ViewModels |
| Firebase Auth | BOM 34.12.0, Auth 23.2.1 | FirebaseConfig.kt, AndroidAuthService.kt | Credencial Google convertida en GoogleAuthProvider y observación de currentUser |
| Google Credential Manager / Google ID | Credentials 1.3.0, Google ID 1.1.1 | MainActivity.kt | Obtiene la credencial/token de Google en Android |
| Cloud Firestore | Firestore 25.1.4 bajo BOM | repositorios Android de Todo, Habit y Journal | CRUD, consultas y addSnapshotListener por usuario |
| Firebase Realtime Database | Database 21.0.0 | FirebaseExperimentRepositoryImpl.kt | Guarda eventos de experimentación por UID |
| Firebase Remote Config | Config KTX 22.1.2 bajo BOM | FirebaseRemoteConfigService.kt, managers shared | Defaults, fetch/activate, listener de cambios y flags de visibilidad/plan |
| Firebase Cloud Messaging | Messaging 24.0.0 bajo BOM | AndroidApp.kt, FirebaseMessagingService.kt | Suscripción a tópico de prueba y recepción de mensajes |
| WorkManager | 2.9.0 | scheduler y workers Android | Resumen diario, notificación Pomodoro y heartbeat de experimentos |
| DataStore Preferences | 1.1.0 | factorías y repositorios shared | Tema, onboarding, notificaciones, estado Pomodoro y override local de plan |
| Room / SQLite bundled | Room 2.8.4, SQLite 2.6.2 | dependencias, KSP y bloque room | **Sólo configurados**: no hay @Entity, @Dao, RoomDatabase, builder ni consultas Room |
| Coroutines / Flow / Turbine | Coroutines runtime/test y Turbine 1.1.0 | ViewModels, repositorios, workers y tests | Flujos reactivos, viewModelScope, callbackFlow, runTest y assertions de Flow |
| Loco / Localise.biz | tareas Gradle manuales | composeApp/build.gradle.kts, .env.example | Descarga/subida de XML mediante curl; no se ejecutó ninguna tarea |
| Firebase Cloud Functions | firebase-admin ^12, firebase-functions ^5, TypeScript ^5, Node 20 | functions/src/index.ts y functions/lib | Endpoint HTTP de prueba que publica a un tópico FCM; no es un backend de negocio de usuarios |
| Testing | kotlin-test, Mockative 3.2.3, Turbine | composeApp/src/commonTest | Suite unitaria compartida declarada; no se ejecutó en esta auditoría |

El catálogo contiene varias versiones recientes y una dependencia Material 3 alpha, pero la compatibilidad efectiva no se certifica aquí porque ejecutar Gradle o instalar componentes estaba fuera del alcance autorizado.

## 5. Arquitectura Android

### Estructura observada

El proyecto raíz tiene dos módulos Gradle Kotlin Multiplatform: :composeApp y :designsystem. Además, contiene un host iOS Swift/Xcode y una carpeta independiente de Firebase Functions.

En composeApp/src/commonMain/kotlin/com/programovil/aura se observan las features:

auth, experiments, habit, home, journal, navigation, notification, onboarding, pomodoro, settings, shared y todo.

La mayoría de las features separan:

- domain/model para entidades y estados de negocio;
- domain/repository para contratos;
- domain/usecase para operaciones;
- data/mapper, data/repository o data/entity cuando corresponde;
- presentation/screen, presentation/composable y presentation/viewmodel;
- di para registro en Koin.

### Flujo principal

    AndroidApp / MainActivity
            ↓
    FirebaseConfig + Koin + lifecycle del proceso
            ↓
    App
            ├─ AuthViewModel → SignInScreen
            ├─ onboarding no completado → OnboardingScreen
            └─ AuthenticatedApp
                 ├─ FeatureFlagManager / UserPlanManager
                 ├─ NavController + AppNavHost
                 └─ Screen → ViewModel → UseCase → Repository
                                          ├─ Firestore / RTDB
                                          └─ DataStore / WorkManager / OS

### Aciertos arquitectónicos observables

- La UI y gran parte de la lógica viven en commonMain, lo que reduce duplicación entre targets.
- Los servicios dependientes de plataforma se abstraen con expect/actual: autenticación, repositorios cloud, DataStore, locale, scheduler y permisos de notificación.
- Los ViewModels exponen StateFlow o estados inmutables y delegan operaciones a casos de uso.
- Las implementaciones Android de Firestore convierten listeners en Flow con callbackFlow y limpian el listener en awaitClose.
- Koin concentra el ensamblaje de dependencias por feature.
- El módulo designsystem es reutilizable entre los targets y no está mezclado con la lógica de Firebase.

### Límites de la etiqueta “Clean Architecture + MVVM/MVI”

Se observa claramente MVVM con ViewModel, StateFlow, estados puntuales y casos de uso. No se encontró un reducer, store, intent/effect o ciclo MVI formal; por eso conviene decir **MVVM con flujo unidireccional de estado en varias features**, no afirmar MVI completo.

También hay inconsistencias de límites:

- AuthService está declarado en auth/domain, pero sus callbacks usan AuthViewModel.AuthState; el dominio depende de una clase de presentación.
- La implementación Android de Todo se encuentra en un paquete domain/repository, mientras Habit y Journal usan data/repository.
- HabitViewModel recibe un HabitRepository que no usa directamente.
- Auth no tiene un caso de uso propio; el ViewModel invoca el servicio de dominio de forma directa.
- Todo usa varios StateFlow independientes (todos, error, isLoading), mientras Habit y Journal modelan un UiState compuesto.

Estas observaciones no eliminan el valor arquitectónico; indican que el código es un MVP estructurado y no una implementación de referencia completamente uniforme.

## 6. Autenticación

### Flujo Android verificado

1. MainActivity.launchGoogleSignIn() crea un GetGoogleIdOption.
2. Usa CredentialManager.getCredential() para obtener una credencial.
3. Comprueba GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.
4. Extrae el ID token.
5. Entrega el token a AuthViewModel.handleSignInResult().
6. AndroidAuthService construye GoogleAuthProvider.getCredential(idToken, null).
7. FirebaseAuth.signInWithCredential() completa el acceso.
8. La app observa FirebaseAuth.currentUser mediante addAuthStateListener y cambia a SignedIn.

Esto es una integración real en Android, no solamente una dependencia declarada. La configuración google-services.json contiene proyecto, paquete Android com.programovil.aura y clientes OAuth compatibles con el flujo; los valores sensibles no se reproducen en este informe.

### Estado de sesión

La persistencia de sesión se delega al SDK de Firebase Auth: AndroidAuthService consulta currentUser y registra un AuthStateListener. No hay un token guardado manualmente en DataStore, ni una clase de perfil, refresh token propio, account deletion, email/password, Apple Sign-In o gestión de credenciales local.

### Estados y errores

AuthViewModel expone Loading, SignedIn, SignedOut y Error. AuthError diferencia ausencia de credencial, ausencia de token y excepción. En el estado actual del working tree, además, el flujo distingue NoCredentialException y el servicio registra longitud del token, no el token.

El problema observable es que las excepciones de Firebase pueden llegar como texto dinámico a la pantalla de login a través de AuthError.Exception. Eso puede ser útil durante diagnóstico, pero puede exponer detalles del backend y producir mensajes poco amigables.

### Riesgos y límites

- El serverClientId de Google está hardcodeado en MainActivity.kt; aunque no es un secreto, crea acoplamiento a un proyecto/configuración concreta.
- No se observan pruebas del flujo Credential Manager, Firebase Auth real, SHA-1/SHA-256 de release o credenciales de producción.
- AuthService declara getCurrentAuthState(), pero no se invoca en el código productivo; Android depende de que el listener de Firebase entregue el estado inicial.
- No existe API visible para remover el listener al destruir el consumidor.
- El target iOS implementa IosAuthService como stub: devuelve SignedOut, responde “Not implemented on iOS” al login y no notifica cambios.
- MainViewController() llama App() sin onSignInClick; aunque se mostrara el login, el callback de autenticación iOS queda vacío.
- En iOS, el AuthViewModel inicia en Loading, el listener no emite y getCurrentAuthState() no se usa: el flujo puede quedar bloqueado en esa pantalla.

## 7. Persistencia y gestión de datos

### Persistencia remota Android

Las colecciones Firestore se organizan bajo el UID autenticado:

    users/{uid}/todos
    users/{uid}/habits
    users/{uid}/completions
    users/{uid}/journals

#### Todo

TodoRepositoryImpl usa listeners de colección para emitir cambios reactivos. Al crear una tarea guarda title, isCompleted, createdAt con FieldValue.serverTimestamp(), y opcionalmente description y dueDate. La actualización usa update() y borra campos opcionales con FieldValue.delete().

#### Hábitos

El repositorio guarda nombre, tipo de recurrencia, objetivo, color y createdAt. Las completions se guardan como documentos separados con habitId, fecha YYYY-MM-DD y completedAt. El toggle consulta por hábito/fecha y borra el primer resultado o agrega una nueva completion.

Observaciones técnicas:

- El toggle consulta y luego escribe; no es una transacción, por lo que dos acciones concurrentes podrían producir duplicados.
- Borrar un hábito sólo borra el documento del hábito; no elimina sus completions, que pueden quedar huérfanas.
- RecurrenceType.valueOf() y el parseo de fechas pueden fallar ante datos remotos corruptos.

#### Diario

El repositorio ordena por createdAt descendente y mapea documentos que tengan título, contenido y fecha de creación. Las fechas se guardan como epoch milliseconds del sistema; las operaciones son set() y delete() por documento.

JournalEntity es un data class de mapeo; no es una entidad Room. Esta distinción es importante porque el README menciona Room como parte de la arquitectura, pero no se encontró una base Room real.

### Persistencia local DataStore

La factoría Android usa preferencesDataStore(name = "aura_preferences"); iOS crea un archivo compartido en Document Directory. En el código se guardan:

- onboarding_completed;
- theme_mode;
- daily_summary_enabled, notification_hour, notification_minute;
- estado Pomodoro: tiempo restante, tiempo inicial, ejecución, modo, sesiones, opción, endsAtEpochMillis, mensaje de finalización y modo completado;
- override local de is_premium en Android.

Esto demuestra persistencia de preferencias y estado de aplicación. No demuestra almacenamiento local de tareas/hábitos/diario ni sincronización offline propia.

### Room

Room está presente en el catálogo, en commonMain.dependencies, en KSP para Android/iOS y en room { schemaDirectory(...) }. Sin embargo, la inspección no encontró @Entity, @Dao, RoomDatabase, databaseBuilder, consultas Room ni archivos de esquema visibles. El uso real de datos es Firestore + DataStore. Room debe describirse como **configurado/no integrado**.

### Offline, caché y conflictos

No se encontró una capa repository cache, cola de operaciones, política de sincronización, merge de conflictos ni estados de conectividad. El SDK de Firestore puede tener comportamiento interno propio, pero eso no equivale a una estrategia offline-first de Aura y no debe afirmarse.

### iOS

Las implementaciones iOS de Todo, Habit y Journal devuelven listas vacías o Result.success(Unit) sin persistir. DataStore y notificaciones sí tienen implementación iOS, pero el dominio principal no tiene paridad de datos.

## 8. Gestión de estado y lifecycle

### Estado

- AuthViewModel, TodoViewModel, HabitViewModel, HomeViewModel, JournalViewModel, JournalDetailViewModel, OnboardingViewModel, PomodoroViewModel y SettingsViewModel extienden ViewModel.
- La UI recoge estado con collectAsState().
- Las operaciones suspendidas se lanzan con viewModelScope.
- Repositorios Firestore exponen Flow<Result<...>> y usan callbackFlow.
- Los estados de error de las features se convierten en claves localizadas (UiText.Resource) y se muestran mediante Snackbars.

### Lifecycle Android

AndroidApp registra un DefaultLifecycleObserver en ProcessLifecycleOwner y actualiza AppVisibilityTracker al entrar/salir de foreground. El temporizador Pomodoro usa el tiempo absoluto endsAtEpochMillis, lo que permite recalcular el tiempo tras volver a la app o recrear el ViewModel. MainActivity también procesa intents de notificaciones en onCreate() y onNewIntent().

### Pomodoro y segundo plano

El ViewModel:

- cancela/reprograma la notificación al seleccionar, pausar o resetear;
- calcula segundos restantes desde endsAtEpochMillis;
- mantiene un ticker de un segundo mientras está en foreground;
- cambia a descanso corto o largo al completar;
- persiste el estado y muestra overlay en primer plano;
- pide una notificación al estar en background.

La cobertura unitaria de esta máquina de estado es una de las partes más fuertes del repositorio. No obstante, no se verificó el comportamiento en un dispositivo bloqueado, Doze, reinicio de proceso o cambios de zona horaria.

### Puntos de lifecycle a vigilar

- AuthService no expone remove listener; no hay cleanup visible del listener Firebase.
- FeatureFlagManager y MotivationPhraseManager son singletons y AuthenticatedApp llama initialize() en cada montaje. No se observa una guarda de inicialización; al entrar/salir varias veces podrían acumular polling/collectors.
- FeatureFlagManager documenta internamente “no polling loop”, pero su código sí lanza polling cada cinco segundos.
- Remote Config configura minimumFetchIntervalInSeconds = 0 y además hay polling de cinco segundos. Esto es apropiado como mecanismo de demo, pero es costoso e inadecuado como supuesto de producción sin límites, caché o backoff.
- Los métodos de guardado de Todo, Habit, Journal y Settings no tienen estados de operación isSaving/isMutating; una acción repetida rápidamente puede lanzar varias operaciones.
- JournalDetailUiState modela isLoading, pero JournalDetailScreen no muestra un indicador específico mientras carga una entrada existente.
- GetDashboardDataUseCase devuelve Result.success incluso cuando una fuente falla, usando datos nulos/ceros; el usuario puede ver un dashboard vacío en lugar de un error de carga.
- Los enums leídos desde DataStore (ThemeMode, PomodoroMode) usan valueOf; una preferencia corrupta puede interrumpir el flujo en vez de aplicar fallback.
- El skip de onboarding sólo cambia dismissedThisSession; no marca la preferencia persistente. Un usuario que salta vuelve a ver el onboarding en la próxima sesión.
- onStart se pasa vacío desde App; el avance funciona porque OnboardingScreen llama directamente a viewModel.start(), pero el callback de host no expresa una transición propia.

## 9. Navegación y estructura de pantallas

### Pantallas localizadas

1. SignInScreen
2. OnboardingScreen
3. HomeScreen
4. TodoScreen
5. HabitScreen
6. JournalScreen
7. JournalDetailScreen
8. PomodoroScreen
9. SettingsScreen
10. PomodoroCompletionOverlay como estado superpuesto de finalización.

No se encontró pantalla de perfil, gestión de cuenta, compra Premium, calendario, estadísticas históricas ni objetivos.

### Rutas

NavRoute es una jerarquía sealed serializable con Home, Todo, Habit, Settings, Journal, Pomodoro y JournalDetail(entryId). AppNavHost usa navegación tipada y pasa entryId a un ViewModel de detalle mediante parámetros Koin.

### Barra de navegación

El orden actual en el código es:

1. Todo, si TODOS_ENABLED;
2. Hábitos, si la feature está habilitada y el plan es Premium;
3. Home, siempre;
4. Pomodoro, si POMODORO_ENABLED;
5. Journal, si JOURNAL_ENABLED.

Settings no está en la barra: se abre desde el icono de configuración de Home. Esto debe reflejarse en capturas y en la descripción del portfolio; no conviene decir que Settings es una pestaña inferior.

### Flags y navegación

El grafo registra rutas condicionalmente y cada screen también comprueba su flag y hace popBackStack si se deshabilita. Es una estrategia válida de kill switch, aunque el cambio de flags mientras el usuario está en una ruta requiere validación de runtime por la recomposición del grafo.

### Onboarding

El flujo de navegación está fuera del NavHost y se decide en App con el estado de autenticación y DataStore. La configuración tiene cuatro slides y fallback de locale. El texto menciona “desliza”, pero HorizontalPager tiene userScrollEnabled = false; el usuario sólo puede usar botones. Es una inconsistencia pequeña pero visible en una demo.

## 10. Integraciones externas

| Integración | Evidencia | Qué sí permite afirmar |
|---|---|---|
| Google Identity | MainActivity.kt usa Credential Manager y Google ID Token | Login Google integrado en Android |
| Firebase Auth | AndroidAuthService.kt usa FirebaseAuth y GoogleAuthProvider | Sesión autenticada Android basada en Firebase |
| Firestore | Repositorios Android por UID | Persistencia/escucha reactiva de Todo, Habit y Journal en Android |
| Realtime Database | FirebaseExperimentRepositoryImpl.kt | Registro de eventos de experimento por usuario autenticado |
| Remote Config | FirebaseRemoteConfigService.kt y managers | Defaults, fetch, activación y cambios de flags en Android |
| FCM | AndroidApp.kt, Manifest y FirebaseMessagingService.kt | Recepción de mensajes y tópico de prueba |
| Cloud Functions | functions/src/index.ts | Endpoint de prueba que envía a test-notifications |
| WorkManager | AndroidNotificationScheduler.kt y workers | Programación de resúmenes, Pomodoro y heartbeat |
| UserNotifications iOS | IosNotificationScheduler.kt | Scheduler local iOS, aunque el resto del flujo iOS esté incompleto |
| Localise.biz/Loco | tareas pullTranslations/pushTranslations y .env.example | Flujo de mantenimiento de traducciones, no ejecutado durante la auditoría |
| Xcode/SwiftUI | iosApp/ContentView.swift y project.pbxproj | Host iOS que embebe el framework Compose |

La aplicación Android no llama desde su código a sendTestNotification; el endpoint Functions y la suscripción Android comparten el tópico de prueba, por lo que la integración localizada es de demostración/QA, no un sistema de notificaciones dirigidas por usuario.

No se encontraron archivos de reglas Firestore/Realtime Database, firebase.json, .firebaserc, índices o configuración de despliegue. No es posible auditar desde este repositorio si el backend está desplegado, qué reglas aplica o qué índices requiere.

## 11. UX y diseño móvil

### Diseño visual

El módulo designsystem define:

- AppColors con primary, background, surface, textPrimary, accent, error y textSecondary;
- cinco paletas: Purple, Red, Green, Dark y High Contrast;
- tipografía compartida con estilos display/headline/title/body/label;
- DsTheme mediante CompositionLocal;
- componentes reutilizables PrimaryButton, BasicInput y divisor horizontal.

Las pantallas aplican tarjetas redondeadas, gradientes verticales, botones flotantes, listas, diálogos, indicadores y una pantalla circular de Pomodoro. La dirección visual es consistente y diferenciable, y el modo High Contrast es una buena pieza para mostrar en portfolio.

### Material Design

Se usan componentes Material 3 reales y colores derivados de AppTheme en la mayoría de la UI. Hay Scaffold, NavigationBar, TopAppBar, FloatingActionButton, SnackbarHost, DatePicker, Card, Switch, Checkbox, OutlinedTextField e iconografía Material.

La afirmación del README de que no hay tamaños hardcodeados no coincide literalmente con el código: abundan tamaños y espaciados dp/sp en las pantallas y componentes, aunque los colores principales y la tipografía estén centralizados. El claim correcto es **“hay un design system de colores y tipografía; la composición todavía usa dimensiones locales”**.

### Estados UX positivos

- carga inicial de aplicación y de listas;
- estados vacíos para tareas, hábitos y diario;
- CTA de “agregar el primero”;
- errores de operaciones mostrados como Snackbar;
- campos obligatorios de título/nombre;
- DatePicker para fecha de tarea;
- swipe-to-dismiss en Journal;
- overlay dedicado al completar Pomodoro;
- selección de tema persistida;
- etiquetas de accesibilidad en varios iconos y acciones.

### Fricciones o estados incompletos

- onboarding silenciosamente abandona la pantalla si falla la carga; no muestra el error al usuario;
- borrar tareas, hábitos y diarios no tiene confirmación ni undo visible;
- no hay loading de guardado, por lo que el usuario no recibe feedback de una mutación en curso;
- fecha y hora se presentan con frecuencia en formato ISO (YYYY-MM-DD) en vez de una representación localizada;
- notificaciones Android y iOS contienen textos hardcodeados en inglés;
- hay recursos para sonidos y vibración, pero SettingsScreen no muestra controles funcionales para ellos;
- la acción llamada onLongClick en Habit se conecta con Modifier.clickable, por lo que en realidad se edita con un tap normal;
- colores, stepper y círculos de días de Habit tienen contentDescription = null o no tienen semántica explícita;
- varios controles táctiles visualmente pequeños usan 24/32 dp y deben revisarse con TalkBack/VoiceOver;
- el icono/contenedor central de play del Pomodoro usa clickable sobre un Box en vez de un componente semántico de botón;
- image_url está en el contrato de onboarding, pero todos los valores del JSON están vacíos y no se carga una imagen externa.

### Responsive y adaptación

La UI usa fillMaxSize, fillMaxWidth, listas desplazables y columnas que pueden crecer. Sin embargo, no se encontraron WindowSizeClass, layouts adaptativos, navegación rail, panel dual, breakpoints, soporte específico de tablet, landscape o desktop. iosApp/ContentView.swift aplica ignoresSafeArea(), y la pantalla de autenticación/onboarding no muestra un manejo explícito propio de safe areas pese a que Android habilita edge-to-edge. La adaptación debe presentarse como **responsive básico por Compose**, no como diseño adaptativo certificado.

## 12. Seguridad observable

Esta sección evalúa únicamente señales visibles en el repositorio. No sustituye una auditoría de reglas Firebase, secretos, tráfico, infraestructura o privacidad.

### Señales positivas

- Las operaciones Firestore Android construyen rutas bajo users/{uid} del usuario autenticado.
- El token Google no se guarda manualmente en DataStore.
- En los cambios locales observados se registra longitud del idToken, no su valor.
- El servicio FCM está declarado android:exported="false".
- Los PendingIntent de notificación usan FLAG_IMMUTABLE.
- .env está ignorado y .env.example sólo contiene el nombre de la variable LOCO_API_KEY sin valor.
- google-services.json es configuración cliente; su presencia no equivale por sí sola a una filtración de una clave de servidor. El informe no reproduce la API key.

### Riesgos o aspectos no demostrados

- No hay reglas firestore.rules, database.rules.json o equivalentes en el checkout; la seguridad real de lectura/escritura no puede verificarse.
- No hay App Check, control de autenticación ni rate limiting visible en la Cloud Function sendTestNotification.
- La Function acepta cualquier POST, habilita Access-Control-Allow-Origin: * y publica al tópico global test-notifications; si está desplegada públicamente, puede convertirse en un broadcast de prueba no autenticado.
- AndroidApp suscribe todos los dispositivos al tópico de prueba al iniciar.
- El plan Premium se deriva de Remote Config y además puede ser sobrescrito localmente en DataStore; eso es una variante de demo, no un entitlement seguro.
- No hay Google Play Billing, verificación de recibo ni fuente de verdad de suscripción.
- android:allowBackup="true" deja abierta la posibilidad de incluir preferencias locales en backup, entre ellas estado Pomodoro, onboarding, notificaciones y override de plan; la política de datos no está documentada en el repositorio.
- Release tiene isMinifyEnabled = false; no se observa signing config, flavors, protección específica ni pipeline de distribución.
- Los mensajes de excepción de Auth pueden llegar a la UI; los logs incluyen UID y mensajes/clases de excepción.
- No hay política de privacidad, consentimiento, retención/borrado de datos ni declaración de tratamiento de diario/tareas.
- No se observa network security config, pinning o controles de transporte propios; tampoco se debe afirmar que sean necesarios o innecesarios sin una revisión de la distribución final.

## 13. Complejidades técnicas

Las siguientes piezas hacen que Aura sea más que una app Android básica y son buenas candidatas para la narrativa de Workana:

1. **Arquitectura multiplataforma:** el mismo dominio y UI Compose viven en commonMain, con implementaciones por plataforma mediante expect/actual.
2. **Integración de identidad moderna:** Credential Manager + Google ID Token + Firebase Auth, incluyendo estados de carga, error y sesión.
3. **Persistencia remota reactiva:** tres dominios de usuario se conectan a listeners Firestore y se convierten en Flow con cleanup.
4. **Dominio de hábitos:** recurrencia diaria/semanal/mensual, objetivos, completions por fecha, progreso actual, siete días y cálculo de racha.
5. **Máquina de estado Pomodoro:** sesiones, descansos, cuarto ciclo, persistencia, rehidratación desde epoch y coordinación con notificaciones.
6. **Lifecycle y segundo plano:** ProcessLifecycle, WorkManager, intents desde notificaciones y sincronización del temporizador al volver a foreground.
7. **Feature delivery configurable:** flags globales, plan Free/Premium, variantes de Home/notificaciones y Remote Config con listener/polling.
8. **Design system propio:** cinco paletas, alto contraste, tokens de color/tipografía y componentes compartidos.
9. **Localización estructurada:** recursos Compose, locale del sistema, fallback y automatización Loco.
10. **Pruebas de lógica:** ViewModels, casos de uso, mappers, modelos, managers y estado Pomodoro están cubiertos por tests unitarios declarados.

### Qué limita la complejidad que se puede reclamar

No se observa un backend de negocio completo, autorización basada en reglas dentro del repositorio, billing, sincronización offline, migraciones de datos, observabilidad productiva, multi-tenant, analytics de producto, integración de calendario, pruebas end-to-end o paridad iOS. La complejidad es real, pero corresponde a un MVP/prototipo técnico con foco Android.

## 14. Calidad de implementación observable

### Fortalezas

- El código de producción compartido tiene 124 archivos Kotlin; designsystem encapsula tokens y componentes.
- Hay modularidad Gradle y organización por feature, no una única Activity monolítica.
- Se utilizan interfaces de repositorio, casos de uso y mappers, lo que facilita tests y sustitución de plataforma.
- Los repositorios Android encapsulan resultados en Result y limpian listeners de Firestore con awaitClose.
- Los ViewModels exponen estado observable y localizan errores mediante UiText.
- La máquina Pomodoro tiene una separación particularmente clara entre cálculo puro (PomodoroCompletionHandler) y efectos (PomodoroViewModel/scheduler).
- La suite contiene 45 clases y 178 @Test, y el historial Git incluye commits específicos de cobertura y correcciones de tests.
- Los tres XML de strings tienen 167 claves cada uno; estáticamente no hay claves faltantes o extra entre en, es y fr.

### Hallazgos de calidad y coherencia

1. **No hay verificación de ejecución en esta auditoría.** La presencia de tests no demuestra que compile o pase en el checkout actual.
2. **No hay tests UI/Compose, integración Firebase, WorkManager o E2E.** El único dependency de UI test está declarada para androidInstrumentedTest, pero no hay fuentes de tests allí.
3. **No hay tests iOS.** Tampoco hay verificación de la integración Xcode en este entorno.
4. **No hay CI/lint/detekt/ktlint/coverage gate visible.** No se encontraron workflows, configuración de cobertura o quality gates.
5. **Room está muerto o incompleto.** Plugin, KSP y runtime agregan complejidad de build sin uso localizado.
6. **Alta dependencia de strings y enum no validados.** valueOf y fechas remotas pueden lanzar excepciones ante datos inesperados.
7. **Creación de hábitos no sigue la ruta de Add en la pantalla.** HabitScreen siempre emite UpdateHabit, incluso al crear; funciona accidentalmente en Firestore porque set() crea/sobrescribe el documento, pero desconecta la intención y validación de AddHabitUseCase.
8. **Semántica de racha discutible.** GetHabitsWithStatusUseCase suma cantidad de completions de periodos consecutivos (streak += count), no cantidad de periodos cumplidos. La variable currentPeriodComplete nunca es verdadera para un periodo que contiene el día actual. Además, isScheduled de los últimos siete días siempre es true, sin calcular recurrencia real.
9. **El algoritmo de racha no tiene test dedicado visible.** Hay tests de Add/Delete/Toggle/accessibility de Habit, pero no se encontró GetHabitsWithStatusUseCaseTest.
10. **Contrato JSON inconsistente.** FeatureFlags.MOTIVATION_PHRASE y el comentario de MotivationPhraseManager muestran un mapa directo {"en":"..."}, mientras MotivationPhraseDto y MotivationPhraseManager esperan {"phrases":{"en":"..."}}. Los tests usan la forma envuelta, así que no cubren el default directo; una configuración directa puede terminar en mapa vacío.
11. **Remote Config sobreactualizado.** Hay listener y polling cada cinco segundos, con minimumFetchIntervalInSeconds = 0; puede generar consumo de red y múltiples ciclos al reabrir la app.
12. **Documentación desfasada.** README anuncia pullTranslations automático en preBuild, pero el composeApp/build.gradle.kts actual del working tree comenta que la tarea es manual. README también presenta Room, MVI y ausencia de tamaños hardcodeados con más amplitud de la que permite el código actual.
13. **Release no está endurecido.** versionCode = 1, versionName = "1.0", minificación desactivada y no hay evidencia de signing/pipeline.

### Lectura comercial de calidad

La calidad observable es suficiente para demostrar criterio de ingeniería y capacidad de construir una base mantenible. La forma honesta de venderla es **“código estructurado, con pruebas unitarias de lógica y varias integraciones Android”**. No es defendible decir **“certificado, sin bugs, listo para producción”** sólo con este repositorio.

## 15. Evidencia por archivos

Las rutas siguientes permiten a otro modelo o a un cliente revisar el claim directamente. Las líneas son las observadas en el checkout auditado.

| Evidencia | Archivo(s) | Qué demuestra |
|---|---|---|
| Arranque, auth gate y onboarding | composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt:76-129 | Estados Loading/SignedOut/SignedIn, onboarding persistente y entrada a la app autenticada |
| Navegación autenticada | composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt:131-307 | NavController, barra inferior, flags y overlay Pomodoro |
| Rutas tipadas | composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt:5-27 | Jerarquía serializable de pantallas |
| Grafo de navegación | composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt:27-137 | Rutas condicionales, detalle de diario y guards de feature |
| Inyección | composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt:17-35 y módulos di | Módulos Koin y factoría DataStore/Remote Config |
| Google Sign-In | composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt:26-64 | Credential Manager, Google ID Token y manejo de excepciones |
| Firebase Auth | composeApp/src/androidMain/kotlin/com/programovil/aura/auth/domain/AndroidAuthService.kt:17-67 | currentUser, GoogleAuthProvider, login y listener |
| Firebase init | composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt:17-45 y shared/FirebaseConfig.kt:10-21 | Inicialización Firebase, Koin, lifecycle y worker de heartbeat |
| Manifest | composeApp/src/androidMain/AndroidManifest.xml:4-30 | Permiso de notificaciones, Activity launcher y FCM service no exportado |
| Tareas | composeApp/src/androidMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepositoryImpl.kt:18-88 | Colección Firestore, listener y CRUD con fecha/descripción |
| Hábitos remotos | composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt:16-134 | Colecciones de hábitos/completions y toggle por fecha |
| Cálculo de hábitos | composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsWithStatusUseCase.kt:15-142 | Progreso, periodos, racha y ventana de siete días |
| UI de hábitos | composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt:40-193 y composable/HabitCard.kt | Lista, CTA, edición, completions y estado vacío |
| Diario remoto | composeApp/src/androidMain/kotlin/com/programovil/aura/journal/data/repository/JournalRepositoryImpl.kt:16-84 | Orden, listener, mapeo y CRUD de Journal |
| Diario UX | composeApp/src/commonMain/kotlin/com/programovil/aura/journal/presentation/screen/JournalScreen.kt:47-176 y JournalDetailScreen.kt | Lista, swipe delete, editor y estados |
| Onboarding | composeApp/src/commonMain/composeResources/files/onboarding_config.json, onboarding/data/repository/OnboardingRepositoryImpl.kt:12-28, OnboardingScreen.kt:52-260 | Cuatro slides, localización, fallback y pager |
| DataStore | shared/data/DataStoreFactory.android.kt:9-17, onboarding/data/OnboardingPreferences.kt, settings/data/ThemeRepositoryImpl.kt | Archivo local y preferencias de onboarding/tema |
| Notificación prefs | notification/data/NotificationPreferences.kt:11-44 | Toggle y hora de resumen |
| Pomodoro persistente | pomodoro/data/PomodoroPreferencesRepository.kt:15-75, pomodoro/domain/PomodoroCompletionHandler.kt:3-55 | Estado persistido y transición Pomodoro/descansos |
| Pomodoro ViewModel | pomodoro/presentation/PomodoroViewModel.kt:36-227 | Ticker, epoch, scheduler, foreground/background y restauración |
| WorkManager | notification/domain/AndroidNotificationScheduler.kt:21-118, notification/presentation/worker/*.kt | Periodicidad, one-shot, resumen y completion |
| FCM | composeApp/src/androidMain/kotlin/com/programovil/aura/FirebaseMessagingService.kt:6-33 | Recepción de mensaje y notificación de tópico |
| Remote Config | shared/FirebaseRemoteConfigService.kt:16-67, shared/FeatureFlagManager.kt:17-71 | Defaults, fetch, listener y actualización de flags |
| Plan/variantes | experiments/domain/usecase/GetHomeVariantUseCase.kt, GetNotificationVariantUseCase.kt, habit/domain/usecase/GetHabitsAccessibilityUseCase.kt | Diferenciación Free/Premium y gate de Hábitos |
| Experimentación | experiments/data/repository/FirebaseExperimentRepositoryImpl.kt:12-55, experiments/presentation/worker/ExperimentsHeartbeatWorker.kt:17-42 | RTDB por UID y heartbeat SessionActive cada 12 horas |
| Design system | designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/theme/Color.kt, DsTheme.kt, Type.kt | Cinco paletas, tokens y tipografía |
| Cloud Function | functions/src/index.ts:1-34 | Endpoint HTTP de prueba y broadcast al tópico |
| Tests | composeApp/src/commonTest/**/*.kt | 45 clases/178 @Test; lógica y estados, no UI/integración |

## 16. Historial Git relevante

### Estado observado

- Rama activa: master.
- HEAD y origin/master: 5e29bc0.
- Último commit: 2026-06-10, Alejandro Castro, fix: versioning.
- Existe la rama local feature/extra-credit-implementation, además de varias referencias remotas.
- El checkout no estaba limpio antes de crear este informe.

### Cambios locales previos a la auditoría

Antes de crear este archivo ya existían modificaciones sin commit en ocho rutas:

    composeApp/build.gradle.kts
    composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt
    composeApp/src/androidMain/kotlin/com/programovil/aura/auth/domain/AndroidAuthService.kt
    composeApp/src/commonMain/composeResources/values-es/strings.xml
    composeApp/src/commonMain/composeResources/values-fr/strings.xml
    composeApp/src/commonMain/composeResources/values/strings.xml
    composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/AuthViewModel.kt
    composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/screen/SignInScreen.kt

Estos cambios mejoran el manejo de NoCredentialException, validan token vacío, agregan logging controlado, evitan que el listener pise un login explícito, ajustan el fallback de error y desacoplan pullTranslations de preBuild. No fueron creados ni modificados por esta auditoría y deben distinguirse de la revisión de HEAD.

### Hitos del historial

El historial contiene commits específicos para:

- integración y correcciones de Pomodoro y notificaciones;
- Remote Config y frases motivacionales;
- gate Free/Premium y GetHabitsAccessibilityUseCase;
- internacionalización/Loco;
- onboarding, Journal, Dashboard y design system;
- cobertura de tests para Auth, Settings, Home, Journal, Habit, Todo, Remote Config y Pomodoro.

En particular, 5dc0065 documenta la incorporación de cobertura de tests y corrección de dos fallos de tests previos; 15a688d cubre la máquina de estado de Pomodoro; 87e141d agrega la sección de testing al README. Hay también commits históricos de features o toggles que no deben confundirse automáticamente con la implementación presente: siempre debe revisarse el árbol actual.

### Lectura comercial del historial

La secuencia de commits permite mostrar evolución incremental, correcciones y trabajo de cobertura. Es una evidencia de proceso de desarrollo, no evidencia de usuarios, clientes, entregas a clientes, releases publicados ni funcionamiento en producción.

## 17. Las 10 características más vendibles

Ordenadas para una presentación de Workana, con la formulación que se puede defender:

| # | Característica | Claim comercial defendible | Evidencia |
|---:|---|---|---|
| 1 | Login Google + Firebase Auth | “Implementé autenticación Android con Credential Manager, Google ID Token y Firebase Auth, incluyendo estados de sesión y error.” | MainActivity, AndroidAuthService, AuthViewModel |
| 2 | Persistencia cloud por usuario | “Conecté tareas, hábitos y diario a colecciones Firestore aisladas por UID, con listeners reactivos y operaciones CRUD.” | Repositorios Android Firestore |
| 3 | Arquitectura KMP/CMP | “Compartí lógica, modelos, casos de uso y UI Compose, separando integraciones de plataforma con expect/actual.” | commonMain, androidMain, iosMain, designsystem |
| 4 | Suite de productividad | “Construí un flujo integrado de tareas, hábitos, diario, dashboard y Pomodoro.” | AppNavHost, features y Home |
| 5 | Pomodoro persistente | “Implementé una máquina Pomodoro con descansos, sesiones, restauración por tiempo absoluto y notificación al completar.” | PomodoroCompletionHandler, PomodoroViewModel, worker |
| 6 | Lifecycle y notificaciones Android | “Coordiné ProcessLifecycle, WorkManager, notificaciones locales, intents y recepción FCM.” | AndroidApp, scheduler, workers, service |
| 7 | Feature delivery remoto | “Añadí flags y variantes Free/Premium configurables desde Firebase Remote Config.” | managers, use cases y gate de Habit |
| 8 | Design system y accesibilidad visual | “Definí una capa visual compartida con cinco paletas, modo alto contraste, tipografía y componentes reutilizables.” | módulo designsystem |
| 9 | Onboarding e internacionalización | “Implementé onboarding configurable desde JSON y recursos en/es/fr con fallback de locale.” | JSON, mapper y Compose Resources |
| 10 | Tests de lógica | “El repositorio incluye 45 clases y 178 tests unitarios declarados para ViewModels, casos de uso, mappers, modelos y managers.” | commonTest, conteo estático |

En los puestos 1, 2 y 6 debe aparecer explícitamente la palabra **Android**. En el puesto 3 conviene añadir “implementación iOS parcial” en la ficha técnica si se muestra el repositorio completo.

## 18. Valor profesional para un cliente Workana

Aura demuestra capacidades que un cliente suele comprar como entregables, no sólo como diseño visual:

- convertir un producto de productividad en varias features coherentes;
- elegir una arquitectura que permite compartir código sin esconder las diferencias de plataforma;
- conectar UI, estado, casos de uso, repositorios y servicios cloud;
- trabajar con autenticación de terceros y datos por usuario;
- resolver lifecycle, persistencia de temporizadores y tareas en segundo plano;
- modelar estados vacíos, loading, errores, temas, accesibilidad y localización;
- estructurar una suite de tests para lógica no trivial.

El portfolio debe mostrar Aura como un **caso de ingeniería móvil Android con arquitectura multiplataforma preparada**, no como una app genérica de lista. El valor está en la trazabilidad completa: una acción visible en la pantalla termina en un ViewModel, un caso de uso, un repositorio y una persistencia real en Android.

### Posicionamiento recomendado

> Aplicación de productividad Android-first construida con Kotlin, Compose Multiplatform y Firebase. Incluye autenticación Google, tareas y diario sincronizados por usuario, hábitos recurrentes, dashboard, temporizador Pomodoro persistente, notificaciones programadas, Remote Config, temas y una suite de pruebas unitarias.

La frase es fuerte y defendible si se mantiene “Android-first” y no se transforma en “multiplataforma completa” o “producción masiva”.

## 19. Skills de Workana

Las etiquetas exactas pueden variar según la taxonomía de Workana. Propuesta de ocho skills:

1. Android Nativo
2. Kotlin
3. Jetpack Compose / Compose Multiplatform
4. Firebase (Authentication, Firestore, Remote Config, FCM)
5. Kotlin Multiplatform
6. MVVM / Clean Architecture
7. Coroutines, Flow y StateFlow
8. WorkManager y testing unitario

### Las cinco mejores para este proyecto

1. **Android Nativo** — es el target funcional con mayor evidencia.
2. **Kotlin** — cubre la implementación principal y el dominio.
3. **Jetpack Compose / Compose Multiplatform** — la UI completa es Compose.
4. **Firebase** — Auth, Firestore, RTDB, Remote Config y FCM están localizados.
5. **Kotlin Multiplatform** — es una decisión arquitectónica diferenciadora, aunque iOS esté parcial.

Clean Architecture/MVVM, WorkManager y testing deben quedar en la descripción técnica o como skills secundarias, no desplazar las cinco anteriores.

## 20. Screenshots recomendados

No se generaron screenshots durante la auditoría porque eso habría requerido ejecutar la app y crear artefactos fuera del alcance autorizado. Las capturas recomendadas son:

| Captura | Qué mostrar | Qué demuestra |
|---|---|---|
| 1. Login | SignInScreen con botón Google y estado limpio | Entrada real y propuesta visual, sin mostrar tokens/configuración |
| 2. Onboarding | Slide de tareas o dashboard con indicador de página | Producto explicado, navegación y localización |
| 3. Home | Dashboard con tarjetas de tareas/hábitos y acceso a Settings | Integración de varias áreas en una experiencia única |
| 4. Todo | Lista con tareas activas/completadas y diálogo con fecha | CRUD, estados, validación y due date |
| 5. Habit | Tarjeta con color, progreso, racha y siete días | Lógica de recurrencia y tracking; preparar estado Premium |
| 6. Journal | Lista y editor de entrada | Persistencia de contenido y navegación de detalle |
| 7. Pomodoro | Temporizador circular en ejecución con sesiones | La feature visual más diferenciadora |
| 8. Completion | Overlay TIME IS UP o equivalente localizado | Máquina de estados y transición a descanso |
| 9. Settings | Cinco temas incluyendo High Contrast + toggle/hora de notificación | Design system, preferencias y accesibilidad visual |
| 10. Notificación | Notification shade o pantalla de permiso, sin IDs internos | WorkManager/OS notification en Android |

### Orden de un carrusel comercial

Login → Home → Todo → Habit → Pomodoro → Journal → Settings/High Contrast → notificación. Una segunda lámina puede mostrar un diagrama breve Compose → ViewModel → UseCase → Firestore/DataStore, con paths de archivo, para probar que las pantallas no son sólo mockups.

No conviene usar un screenshot del host iOS como prueba de funcionalidad completa: el host existe, pero Auth y los repositorios principales iOS son stubs.

## 21. Cinco títulos profesionales de portfolio

1. **Aura — App de productividad Android con Kotlin, Compose y Firebase**
2. **Aura — MVP de productividad con autenticación Google y sincronización Firestore**
3. **Aura — Aplicación Kotlin Multiplatform con Compose, Pomodoro y notificaciones**
4. **Aura — Sistema de tareas, hábitos y diario con arquitectura MVVM**
5. **Aura — Android-first productivity app con Firebase, DataStore y WorkManager**

El título 1 es el más seguro para Workana. El 3 debe acompañarse de “Android-first / iOS parcial” en la descripción para evitar que el título sugiera paridad ya terminada.

## 22. Materia prima para descripción

### Versión corta

Aura es una aplicación Android-first de productividad desarrollada con Kotlin, Compose Multiplatform y Firebase. Integra login con Google, tareas y diario persistidos por usuario, hábitos recurrentes, dashboard, temporizador Pomodoro persistente, notificaciones programadas, temas personalizables y configuración remota de features.

### Versión media

Construí Aura como una aplicación de productividad real, no como una sola pantalla de demostración. La app Android combina autenticación Google mediante Credential Manager y Firebase Auth, colecciones Firestore para tareas, hábitos y diario, un dashboard con estados de productividad y un temporizador Pomodoro que conserva su progreso al salir y volver a la app. La UI está desarrollada con Compose Multiplatform y un design system compartido con cinco paletas, alto contraste y recursos en inglés, español y francés. También incorpora DataStore, WorkManager, notificaciones locales, FCM y feature flags con Firebase Remote Config. El repositorio incluye una suite de tests unitarios para lógica de dominio y presentación.

### Versión técnica

Aura es un MVP Android-first construido con Kotlin Multiplatform, Compose Multiplatform y una arquitectura organizada por features con capas de dominio, datos, presentación y DI. El flujo Android implementa Google Credential Manager → Google ID Token → FirebaseAuth; los repositorios de Todo, Habit y Journal usan Firestore bajo users/{uid} y convierten listeners en Flow mediante callbackFlow. DataStore persiste temas, onboarding, preferencias de notificación y el estado Pomodoro, cuyo endsAtEpochMillis permite recalcular el tiempo tras recreación o background. WorkManager gestiona resumen diario, alertas de vencimiento, completion de Pomodoro y heartbeat de experimentos. Remote Config alimenta flags y variantes Free/Premium, mientras un módulo de design system comparte paletas, tipografía y componentes Compose. El proyecto contiene 45 clases y 178 tests unitarios declarados en commonTest; no se presenta ese número como una corrida exitosa sin ejecutar el build. La capa iOS incluye host, DataStore y scheduler de notificaciones, pero sus integraciones principales de Auth y datos todavía son parciales.

## 23. Claims fuertes y defendibles

Estos claims se pueden usar porque tienen una ruta clara en el código:

- “Implementé login con Google en Android usando Credential Manager, Google ID Token y Firebase Authentication.”
- “Conecté tareas, hábitos y diario a Firestore con colecciones por usuario y listeners reactivos.”
- “Diseñé una arquitectura Kotlin Multiplatform con UI Compose compartida y adaptadores de plataforma.”
- “Implementé casos de uso, repositorios, ViewModels y estados observables por feature.”
- “Construí hábitos con recurrencia diaria/semanal/mensual, objetivos, completions, progreso de siete días y rachas.”
- “Implementé un temporizador Pomodoro con sesiones, pausas, descansos, persistencia y recuperación desde background.”
- “Integré WorkManager y notificaciones Android para resumen diario y finalización de Pomodoro.”
- “Añadí Remote Config con feature flags y variantes Free/Premium.”
- “Definí un design system compartido con cinco paletas, alto contraste y tipografía propia.”
- “Implementé onboarding desde JSON y recursos localizados en inglés, español y francés.”
- “El repositorio contiene 45 clases y 178 pruebas unitarias declaradas para lógica y estado.”

Cuando sea posible, añadir el alcance **“en Android”**, **“en el código auditado”** o **“declaradas en el repositorio”**. Esas palabras aumentan la credibilidad porque no convierten evidencia estática en una promesa de producción.

## 24. Claims que NO debemos utilizar

- “Aura es una app iOS y Android completamente terminada.” Auth, Remote Config y datos principales iOS son stubs/no-op.
- “Aplicación lista para producción.” No hay build/runtime verificable en esta auditoría, CI, reglas Firebase, signing ni evidencia de release.
- “Offline-first con sincronización y resolución de conflictos.” No hay caché ni estrategia de conflictos propia.
- “Persistencia con Room.” Room está configurado, pero no hay entidades, DAOs ni base implementada.
- “Integración con Google Calendar o calendario del dispositivo.” No hay CalendarContract ni API de calendario.
- “Búsqueda, filtros, prioridades, objetivos y estadísticas avanzadas.” No están implementados como módulos funcionales.
- “Suscripciones Premium y pagos integrados.” Premium es una bandera Remote Config/override local; no hay Billing.
- “A/B testing optimizado con métricas reales.” Hay modelos, variantes y heartbeat, pero no resultados, usuarios, cohortes ni métricas de negocio.
- “Notificaciones push personalizadas por usuario.” FCM está conectado al tópico de prueba y la Function publica a ese tópico; no hay token dirigido ni backend de preferencias.
- “178 tests pasando.” Sólo se verificó estáticamente que existen 178 anotaciones @Test; no se ejecutó la suite.
- “Cobertura completa de UI, Firebase o end-to-end.” No hay tests Compose, Firebase integration ni E2E localizados.
- “Cero warnings o código libre de bugs.” No se ejecutaron compilador, lint ni análisis estático.
- “Sin strings/tamaños hardcodeados.” Hay textos hardcodeados en notificaciones/errores y muchos dp/sp locales.
- “Paridad de funciones entre plataformas.” Android y iOS tienen implementaciones claramente distintas.
- “Usuarios, clientes, ventas, retención o impacto medido.” Ningún dato de ese tipo está en el repositorio.
- “Seguridad auditada de Firebase.” Las reglas y configuración de backend no están presentes.

## 25. Preguntas que debe responder el propietario

### Backend y operación

1. ¿Cuál es el estado real del proyecto Firebase aura-app-7dce3 y quién administra la consola?
2. ¿Están desplegadas las reglas de Firestore y Realtime Database? ¿Cómo restringen users/{uid}?
3. ¿Qué proveedores de Firebase Auth están habilitados en la consola y en qué ambientes?
4. ¿Qué SHA-1/SHA-256, OAuth clients y application IDs están configurados para debug y release?
5. ¿Existe una configuración de release firmada y un APK/AAB instalable que haya sido probado en dispositivos?
6. ¿La Cloud Function sendTestNotification está desplegada? Si sí, ¿qué auth, App Check, rate limiting y control de origen tiene?
7. ¿Cuál es la política para el tópico test-notifications y cuándo se eliminará del build de demostración?
8. ¿Existen índices Firestore requeridos por la consulta de fechas y se verificó el consumo/coste de listeners?

### Producto y negocio

9. ¿Hábitos debe ser realmente Premium o el gate es sólo una variante de demo? ¿Cuál es la fuente de verdad del entitlement?
10. ¿Hay plan de pagos, suscripción o integración de Play Billing fuera de este repositorio?
11. ¿Cuál es el público objetivo y qué problema se quiere enfatizar en Workana: productividad personal, wellness, hábitos o gestión de tareas?
12. ¿Qué features son obligatorias para una versión comercial y cuáles fueron experimentales?
13. ¿Existe validación de usuarios, feedback, métricas, entrevistas o resultados que se puedan documentar sin inventar?
14. ¿Qué significa “objetivos” en el producto, dado que no hay módulo de metas en el código auditado?

### Plataformas y alcance

15. ¿iOS es una demo de arquitectura, un target futuro o se espera presentarlo como funcional hoy?
16. ¿Se planea integrar Firebase/Google Sign-In nativos en iOS y persistencia cloud real?
17. ¿Se ha probado el proyecto iOS en Mac Intel, considerando que composeApp declara sólo iosArm64 e iosSimulatorArm64?
18. ¿Qué versión mínima de iOS/Android se quiere soportar en la presentación?

### Calidad, seguridad y evidencia

19. ¿Los 178 tests se ejecutaron recientemente? ¿Hay reportes JUnit, cobertura o CI que se puedan adjuntar?
20. ¿Se probó TalkBack, VoiceOver, tamaños de fuente grandes, rotación, tablet, landscape, offline y Doze?
21. ¿Se revisaron los casos de concurrencia del toggle de hábitos, borrado de completions huérfanas y semántica esperada de racha?
22. ¿Se confirmó el formato de Remote Config para motivation_phrase y el comportamiento del default directo frente al DTO envuelto?
23. ¿Qué datos personales almacena el diario y cuál es la política de privacidad, retención, exportación y borrado de cuenta?
24. ¿allowBackup está aprobado para el tratamiento de preferencias y estado local?
25. ¿Las ocho modificaciones locales observadas deben integrarse en la versión final del portfolio o representan trabajo temporal de diagnóstico?

### Presentación en Workana

26. ¿Qué screenshots y qué APK/demo se pueden compartir públicamente sin exponer project IDs, datos de usuarios o configuración privada?
27. ¿Se puede mostrar una cuenta demo y un flujo reproducible de login sin publicar credenciales?
28. ¿Se quiere posicionar el proyecto como “Android con KMP” o como “KMP multiplataforma”? La primera opción coincide mejor con la evidencia actual.
29. ¿Qué rol exacto se quiere demostrar: desarrollo Android, arquitectura, Firebase, UI/UX, QA automatizado o integración de servicios?
30. ¿Existe un nombre de cliente o contexto contractual que pueda mencionarse? Si no existe, no debe insinuarse que Aura fue desarrollado para un cliente real.

## Anexo A — Corrección de Google Sign-In y renovación visual

Este anexo registra el trabajo realizado después de la auditoría base. Las conclusiones de ejecución siguen siendo limitadas por la ausencia de Android SDK y de un dispositivo/emulador disponible en este entorno.

### A.1 Diagnóstico verificable de Google

El `google-services.json` actual corresponde al proyecto Firebase `aura-app-7dce3`, al paquete `com.programovil.aura` y al cliente OAuth web `623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com`. El certificado Android que aparece en esa configuración tiene SHA-1 `31:71:7B:9C:14:8A:B9:BE:12:24:AD:94:0F:40:F2:75:2E:3E:C7:A7`.

El `debug.keystore` disponible en este equipo utiliza otro certificado:

- SHA-1: `3A:51:D0:DF:C5:45:B9:C6:59:3E:20:94:0F:26:93:C2:48:1F:D3:CF`;
- SHA-256: `86:76:89:92:50:2A:DF:8B:EE:3A:FE:74:61:CD:48:62:0F:D2:2A:7B:1E:B8:56:94:C3:C0:22:A2:FE:61:E2:50`.

La discrepancia es una causa concreta y verificable de fallos de verificación OAuth/Firebase durante el desarrollo local: el certificado con el que se firma el APK instalado no coincide con el certificado registrado en Firebase. No es seguro “arreglarla” cambiando manualmente el JSON ni cambiando de proyecto; el historial muestra que el repositorio alternó entre dos proyectos Firebase distintos.

### A.2 Corrección aplicada al flujo Android

En `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt` se aplicaron estas correcciones:

- el `serverClientId` se obtiene de `R.string.default_web_client_id`, generado por el plugin de Google Services a partir de la configuración vigente, eliminando el segundo valor hardcodeado;
- cada solicitud genera un nonce criptográficamente aleatorio de 32 bytes;
- se conserva el flujo rápido para cuentas ya autorizadas;
- si `setFilterByAuthorizedAccounts(true)` devuelve `NoCredentialException`, se reintenta con `false` para abrir el selector de cuenta en instalaciones nuevas o sin una cuenta autorizada;
- la cancelación del usuario no se muestra como un error de autenticación y las excepciones restantes se registran y se comunican al `AuthViewModel`.

La corrección de código hace robusto el flujo de selección y reduce una causa de fallo de Credential Manager, pero no puede registrar certificados en Firebase desde el repositorio.

### A.3 Acción externa obligatoria para cerrar la incidencia

En Firebase Console, el propietario debe añadir el SHA-1 y el SHA-256 del `debug.keystore` local a la aplicación Android `com.programovil.aura`, comprobar que el proveedor Google esté habilitado y descargar un `google-services.json` actualizado. Para release se debe registrar además el certificado real de firma del AAB/APK, que será diferente del debug. La [guía oficial de Firebase para Google Sign-In](https://firebase.google.com/docs/auth/android/google-signin) documenta la relación entre SHA-1, proveedor Google y el archivo de configuración.

### A.4 Renovación visual aplicada

El trabajo visual se centralizó en el design system compartido:

- `designsystem/.../theme/Color.kt`: paleta clara de alto contraste práctico, superficies tonales, coral como acento y tokens explícitos para contenido, bordes, estados y error; se conservaron paletas oscuras y de alto contraste;
- `designsystem/.../theme/Type.kt`: jerarquía tipográfica más equilibrada, con tamaños y pesos coherentes para display, títulos, cuerpo y labels;
- `designsystem/.../theme/DsTheme.kt`: los tokens de Aura ahora alimentan el `MaterialTheme` de Material 3, incluyendo colores y tipografía para componentes Material;
- `PrimaryButton.kt` y `BasicInput.kt`: botones llenos, estados disabled, campos con contenedor tonal, bordes, cursor y selección coherentes;
- `App.kt`: navegación inferior con superficie elevada, esquinas redondeadas e indicador de selección tonal.

Se renovaron los recorridos principales en Compose: login, onboarding, dashboard, tareas, hábitos, diario, detalle del diario, Pomodoro y ajustes. El criterio común aplicado fue jerarquía visual clara, tarjetas con radios consistentes, espacios de 8 dp, 12 dp, 16 dp y 24 dp, acciones primarias visibles, estados vacíos legibles, superficies diferenciadas, feedback de progreso y contenido desplazable en pantallas de listas. También se ajustó el temporizador para mantener una relación cuadrada y un tamaño máximo razonable en anchos distintos.

La mejora es observable en código; no debe afirmarse todavía que fue validada en todos los tamaños, orientación, TalkBack o dispositivos hasta ejecutar una matriz visual real.

### A.5 Gradle, JDK y reproducibilidad entre Windows y Linux

Se conservó el contrato de versiones existente: wrapper Gradle `9.4.1`, daemon JDK `21` y la configuración declarativa de `gradle/gradle-daemon-jvm.properties`. Se retiró de `settings.gradle.kts` el bootstrap del plugin externo Foojay para que la evaluación inicial no dependa de resolver ese plugin adicional; esto reduce un punto de fallo cuando el entorno está offline o tiene cachés diferentes. No se actualizaron dependencias ni versiones.

La validación directa del wrapper/distribución se realizó con Gradle 9.4.1 y JDK 21. En una primera ejecución el entorno no tenía un Android SDK válido; posteriormente, con el SDK disponible en `C:\Users\Pc\AppData\Local\Android\Sdk`, `:composeApp:assembleDebug` terminó correctamente. Para compartir el proyecto entre Linux y Windows, cada desarrollador debe mantener su propio `local.properties` ignorado por Git o definir `ANDROID_HOME`/`ANDROID_SDK_ROOT`; no se debe versionar una ruta absoluta de Windows. También conviene documentar la instalación de JDK 21, Android SDK, `platforms;android-36` y `build-tools;36.0.0` en el onboarding técnico del repositorio.

La advertencia de migración de APIs/deprecaciones que aparece con AGP 9.x quedó registrada como trabajo futuro de compatibilidad. No se modificó de forma especulativa porque mezclar una migración de AGP/Kotlin/KMP con el rediseño visual podría introducir una rotura mayor.

### A.6 Estado de entrega y límites

- No se ejecutó `git commit`, `git push`, merge, reset ni cambio de rama.
- El reporte es el único archivo nuevo no rastreado; las modificaciones Kotlin/Compose/Gradle visibles en `git status` corresponden al diagnóstico/corrección y al rediseño solicitado, además de cambios locales que ya existían antes de esta intervención.
- Se generó una APK debug verificable localmente; no se generó ni validó una release firmada para distribución.
- Antes de una entrega comercial, debe completarse la configuración Firebase, instalar el SDK, ejecutar compilación/tests/lint y validar visualmente el APK en al menos un teléfono pequeño, uno grande y una configuración de fuente ampliada.

### A.7 Evidencia del APK debug corregido

La compilación produjo `composeApp/build/outputs/apk/debug/composeApp-debug.apk` con `:composeApp:assembleDebug` y resultado `BUILD SUCCESSFUL`. La inspección con `apksigner` confirma:

- paquete: `com.programovil.aura`;
- firma APK v2 válida;
- SHA-1 del firmante: `3A:51:D0:DF:C5:45:B9:C6:59:3E:20:94:0F:26:93:C2:48:1F:D3:CF`;
- `default_web_client_id` empaquetado: `623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com`.

Esta evidencia confirma que el proyecto compila y que el Web Client ID correcto llega al APK. Si esa APK sigue mostrando `[28444]`, la corrección pendiente está fuera del repositorio: registrar su certificado SHA-1 en el cliente Android del proyecto Google/Firebase y verificar el proveedor Google.
