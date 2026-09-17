# Aura — plan operativo de datos demo para portfolio

Fecha: 15 de septiembre de 2026. Alcance: auditoría funcional y de persistencia del proyecto actual, y diseño de una cuenta ficticia, coherente y no sensible para preparar capturas. Este documento **no modifica código, no crea datos, no elimina datos, no hace commits y no hace push**.

La ruta recomendada es crear los datos en una cuenta Google de demo dedicada. No uses la cuenta personal del propietario si el objetivo es poder repetir la preparación sin riesgo.

## 0. Decisiones rápidas

- Perfil: estudiante de Ingeniería de Sistemas que combina universidad, desarrollo de software, seguridad, proyectos y hábitos de estudio.
- Volumen recomendado: **14 tareas** (8 pendientes y 6 completadas), **5 hábitos**, **23 registros de completación** y **6 entradas de diario**.
- Relación narrativa: los proyectos ficticios **Aura** y **Pulso** conectan tareas, hábitos y diario. Aura sigue en mejora; Pulso aparece como proyecto colaborativo.
- Fecha de referencia de este documento: `D = 2026-09-15` (martes). Si la tablet tiene otra fecha, conserva los desplazamientos `D-…` y `D+…`, no las fechas absolutas entre paréntesis.
- Todas las fechas de backend se expresan como número Unix en milisegundos a las 12:00 UTC. Así la etiqueta local no retrocede un día en zonas como `America/La_Paz`.
- La cuenta debe tener acceso a Tareas, Diario y Enfoque. **Hábitos solo aparece si `HABITS_ENABLED` está activo y el plan resuelto es Premium (`is_premium=true`)**. Esto es una condición de Remote Config/entitlement, no un dato que pueda crearse desde un formulario.

## 1. Auditoría funcional

### 1.1 Pantallas y rutas reales

| Pantalla | Ruta | Qué muestra | Qué puede hacer el usuario | Dependencias |
|---|---|---|---|---|
| Acceso | `App` / `SignInScreen` | Marca, explicación y botón Google | Iniciar sesión; reintentar tras error | Firebase Auth + Android Credential Manager |
| Onboarding | estado previo a `Home` | Cuatro páginas cargadas desde `onboarding_config.json` | Avanzar, volver, saltar, iniciar | Preferencia local `onboarding_completed` |
| Inicio | `NavRoute.Home` | Fecha local, tareas pendientes, avance de hábitos, atajo Enfoque y, si Premium, motivación | Abrir Tareas, Hábitos, Enfoque, Ajustes o Diario según flags | Combina Todo + Hábitos; feature flags |
| Tareas | `NavRoute.Todo` | Grupos Pendientes/Completadas, descripción y fecha | Crear, editar, completar/descompletar, quitar fecha, eliminar | Firestore `todos` |
| Hábitos | `NavRoute.Habit` | Tarjetas con recurrencia, progreso del período, racha y últimos 7 días | Crear, editar nombre/frecuencia/meta/color, marcar/desmarcar cada fecha visible, eliminar | Premium + Firestore `habits`/`completions` |
| Enfoque | `NavRoute.Pomodoro` | Dial, modo, contador, duración, controles y sesiones acumuladas | Elegir 5/10/25 min, iniciar/pausar, reiniciar, saltar | DataStore local + WorkManager para aviso |
| Diario | `NavRoute.Journal` | Lista descendente agrupada por mes | Abrir, borrar con confirmación, crear entrada | Firestore `journals` |
| Editor de diario | `NavRoute.JournalDetail(entryId?)` | Título y texto a pantalla completa | Crear/editar, guardar, borrar, descartar borrador | Firestore; borrador en `SavedStateHandle` durante el proceso |
| Ajustes | `NavRoute.Settings` | Cinco temas, notificaciones, hora del resumen y cerrar sesión | Cambiar tema, activar permiso/resumen, cambiar hora, cerrar sesión | DataStore + permiso Android + WorkManager |

No existen otras rutas de usuario para calendario, proyectos, objetivos, categorías, historial o estadísticas.

### 1.2 Campos y reglas de cada formulario

#### Tarea (`Todo`)

- `title`: texto obligatorio; no puede quedar en blanco. Se recorta al guardar.
- `description`: texto opcional; una descripción vacía se guarda como ausente.
- `dueDate`: fecha opcional seleccionada con `DatePicker`; es un `Long` de milisegundos, sin hora de negocio.
- `isCompleted`: booleano; empieza en `false` y se cambia desde el checkbox de la fila, no desde el editor.
- `id`: lo genera Firestore al crear desde la aplicación.
- `createdAt`: se escribe como server timestamp al añadir, pero la UI actual no lo lee ni lo muestra.
- Crear, editar, completar/descompletar y eliminar están disponibles.
- **No hay** categoría, color, prioridad, tipo, duración, hora, notas separadas, subtareas, etiquetas ni proyecto.

#### Hábito (`Habit`)

- `name`: texto obligatorio; no puede quedar en blanco.
- `recurrenceType`: exactamente `DAILY`, `WEEKLY` o `MONTHLY`.
- `targetCount`: diario queda fijado en `1`; semanal acepta `1–7`; mensual acepta `1–31`.
- `color`: una de seis opciones: Coral `#FF8D70`, Menta `#51B89E`, Azul `#6A9AE8`, Violeta `#7C6AE6`, Ámbar `#E9B949`, Malva `#B779D1`.
- `createdAt`: se genera automáticamente al crear; no hay campo editable en el formulario.
- La completación es otro documento con `habitId` y `completedDate` (`YYYY-MM-DD`). El usuario puede tocar los siete días visibles de la tarjeta.
- No hay archivado, recordatorio por hábito, hora, nota, categoría ni prioridad.

#### Entrada de diario (`JournalEntry`)

- `title`: la UI exige que no esté en blanco para habilitar Guardar.
- `content`: texto libre; la UI no impone una longitud mínima.
- `createdAt` y `updatedAt`: milisegundos generados por el repositorio al crear/editar.
- `id`: lo genera el repositorio/Firestore.
- Crear, editar, borrar y descartar cambios están disponibles.
- No hay etiquetas, mood, color, adjuntos, ubicación ni categoría.

#### Pomodoro

- Modos reales: `POMODORO`, `SHORT_BREAK`, `LONG_BREAK`.
- Duraciones seleccionables: **5, 10 y 25 minutos**. Los defaults son 25/5/15 para Pomodoro/descanso corto/descanso largo.
- Controles: iniciar/pausar, reiniciar y saltar.
- Cada Pomodoro completado incrementa `sessionsCompleted`; saltar también cuenta como sesión según el motor actual. Los descansos no incrementan el contador.
- Después de 4 Pomodoros se selecciona descanso largo.
- No existe una lista de sesiones, fecha de sesión, tarea asociada, historial remoto, estadísticas de minutos ni campo de proyecto.

#### Ajustes y flags

- Temas: Aura/Purple, Forest/Green, Clay/Red, Midnight/Dark y High contrast.
- Notificaciones: booleano, permiso Android y hora/minuto del resumen diario.
- No existe un selector de idioma en Aura; se usa el locale del dispositivo.
- No existe un control de plan Premium en la UI renovada.
- Flags relevantes: `TODOS_ENABLED`, `HABITS_ENABLED`, `JOURNAL_ENABLED`, `POMODORO_ENABLED`, `IS_PREMIUM`.

### 1.3 Información derivada que sí aparece

**Inicio** combina dos streams:

- `incompleteTodos`: cantidad de tareas cuyo `isCompleted` es `false`.
- `completedHabitsToday`: hábitos cuya completación de hoy está marcada.
- `totalHabitsToday`: cantidad de hábitos visibles.
- `currentStreak`: la racha máxima entre los hábitos.

**Hábitos** calcula `currentPeriodProgress` contra el día, semana ISO (lunes–domingo) o mes actual. La “racha” suma las completaciones del período actual y de períodos anteriores consecutivos que alcanzaron la meta; por diseño no significa necesariamente “días”. La tarjeta siempre muestra los últimos siete días, aunque existan completaciones anteriores.

**Diario** ordena por `createdAt` descendente y agrupa visualmente por mes. Es el único lugar que se parece a un historial.

**Pomodoro** solo muestra el estado actual y `sessionsCompleted`; no genera una pantalla de estadísticas.

**Calendario** no existe como pantalla. La única experiencia de fecha es el DatePicker de una tarea y la tira de siete días de un hábito.

## 2. Auditoría de persistencia

### 2.1 Tabla de almacenamiento

| Dato | Sistema | Ubicación/clave | ¿Aparece en otra tablet? |
|---|---|---|---|
| Sesión y usuario Google | Firebase Authentication | `FirebaseAuth.currentUser.uid` | Sí, al iniciar sesión con la misma cuenta |
| Tareas | Cloud Firestore | `users/{uid}/todos/{todoId}` | Sí, por listener de Firestore |
| Hábitos | Cloud Firestore | `users/{uid}/habits/{habitId}` | Sí, por listener de Firestore si el entitlement permite verlos |
| Completaciones de hábitos | Cloud Firestore | `users/{uid}/completions/{completionId}` | Sí, por listener de Firestore |
| Diario | Cloud Firestore | `users/{uid}/journals/{journalId}` | Sí, por listener de Firestore |
| Pomodoro actual | Preferences DataStore Android (`aura_preferences`) | `pomodoro_time_left_seconds`, `pomodoro_initial_time_seconds`, `pomodoro_is_running`, `pomodoro_mode`, `pomodoro_sessions_completed`, `pomodoro_selected_option`, `pomodoro_ends_at_epoch_millis`, `pomodoro_show_completion_message`, `pomodoro_completed_mode` | **No**; queda en el dispositivo |
| Tema | Preferences DataStore | `theme_mode` | No |
| Notificaciones | Preferences DataStore | `daily_summary_enabled`, `notification_hour`, `notification_minute` | No |
| Onboarding | Preferences DataStore | `onboarding_completed` | No |
| Override local de Premium | Preferences DataStore | `is_premium` | No; Remote Config sigue siendo la fuente por defecto |
| Eventos experimentales | Firebase Realtime Database | `users/{uid}/experiments/events/{pushId}` | Se sincroniza, pero no se presenta en la UI |
| Trabajos de notificación | WorkManager Android | nombres `daily_summary_work`, `pomodoro_completion_work`, etc. | No; son trabajos del dispositivo |

No hay Room, SQLite propio ni SharedPreferences de aplicación para estas entidades. Firestore puede mantener caché interna del SDK, pero la aplicación no implementa una base local alternativa para tareas, hábitos o diario.

### 2.2 Contratos Firestore exactos

El código Android lee estos tipos y nombres. Los campos con tipo incorrecto hacen que el documento se ignore o se convierta con un valor por defecto.

**`users/{uid}/todos/{todoId}`**

```text
title       string (obligatorio para que se vea)
description string (opcional; omitir si no hay descripción)
isCompleted boolean
dueDate     number Long en milisegundos (opcional)
createdAt   server timestamp al crear desde la app; no es necesario para la UI actual
```

**`users/{uid}/habits/{habitId}`**

```text
name          string
recurrenceType string: DAILY | WEEKLY | MONTHLY
targetCount   number entero
color         string hexadecimal, por ejemplo #7C6AE6
createdAt     number Long en milisegundos
```

**`users/{uid}/completions/{completionId}`**

```text
habitId       string igual al ID del hábito
completedDate string YYYY-MM-DD; una sola completación por hábito y fecha
completedAt   number Long en milisegundos
```

**`users/{uid}/journals/{journalId}`**

```text
title     string
content   string
createdAt number Long en milisegundos
updatedAt number Long en milisegundos
```

Las reglas actuales solo permiten al usuario autenticado leer/escribir su propio `{uid}`. Un script de cliente no puede escribir otra cuenta. Un Admin SDK sí puede saltar reglas, por lo que debe limitarse a una cuenta de demo y nunca incluir la cuenta de servicio en el repositorio o en la APK.

### 2.3 Veredicto de siembra en otra tablet

Sí es técnicamente seguro sembrar **tareas, hábitos, completaciones y diario** desde un backend de desarrollo si se cumplen todas estas condiciones:

1. Se usa el mismo proyecto Firebase que la APK instalada y se verifica el `project_id` actual (`aura-6ac09` en el `google-services.json` inspeccionado).
2. Se obtiene el UID exacto de una cuenta Google de demo desde Firebase Authentication.
3. Se escribe únicamente bajo `users/{UID}/…` y con los nombres/tipos anteriores.
4. Se usa una cuenta de demo separada, sin borrar ni sobrescribir datos de una cuenta existente.
5. Se valida primero en un proyecto/emulador de prueba y se ejecuta el script con IDs deterministas y modo dry-run.
6. Después se inicia sesión en la tablet con **la misma cuenta Google**. Los listeners deberían actualizar la UI; si no, cerrar y abrir la aplicación o volver a iniciar sesión, sin borrar datos de la aplicación.

La siembra remota **no** puede crear el contador Pomodoro, el tema, el permiso de notificaciones ni el estado de onboarding: esos valores viven en el DataStore de cada dispositivo. El contador de Pomodoro debe prepararse manualmente en la tablet.

## 3. Escenario demo coherente

La historia es la de una persona que mantiene dos frentes: entrega académica y desarrollo de Aura/Pulso. Las tareas completadas explican decisiones ya cerradas; las pendientes representan la siguiente semana; los hábitos reflejan una rutina parcial, no perfecta; el diario conecta las decisiones técnicas con la experiencia de estudio.

### 3.1 Resultado esperado de Home

Con el conjunto completo y D=`2026-09-15`:

- Tareas pendientes: **8**.
- Hábitos marcados hoy: **3/5** (Application Security, Algoritmos y Juice Shop).
- Racha destacada: **7** (Application Security diario).
- El Home muestra el hero de tareas, tarjeta de hábitos y atajo a Enfoque.
- La motivación diaria solo aparecerá si el plan es Premium y Remote Config entrega una frase; no es necesaria para las capturas principales.

## 4. Ruta manual: datos que ingresar literalmente

### 4.0 Preparación de la cuenta y la tablet

1. En Firebase Authentication crea o elige una cuenta Google de demo, por ejemplo `aura.portfolio.demo@…`; no uses una cuenta con información personal.
2. Comprueba que la APK de la tablet apunta al proyecto Firebase correcto y que la cuenta puede iniciar sesión.
3. Completa las cuatro páginas de onboarding con **Iniciar** o **Saltar**. No se generan datos de negocio allí.
4. Abre Ajustes y deja el tema **Aura** para la captura principal. Los otros temas se fotografían después si quieres mostrar el sistema visual.
5. Abre Home. Si no aparece Hábitos, no intentes crear datos repetidamente: verifica primero `HABITS_ENABLED=true` y `IS_PREMIUM=true` en el entorno de demo. Es un requisito de entitlement.
6. Mantén la tablet conectada a Internet durante la carga y espera a que cada elemento aparezca antes de introducir el siguiente.

### 4.1 Tareas — crear 14 elementos

**Secuencia para cada tarea:** Tareas → `Nueva tarea` → escribe Título → escribe Descripción → abre `Añadir fecha` solo si la fila indica fecha → selecciona la fecha → `Guardar` → espera a que la fila aparezca. Categoría, color, prioridad, hora y duración no se rellenan porque no existen.

> Aviso de fecha en La Paz: el DatePicker actual entrega medianoche UTC y la UI convierte a fecha local. Si al guardar una fecha la etiqueta queda un día atrás, selecciona en el calendario el día siguiente al deseado y comprueba la etiqueta final. La ruta backend de §5 evita este desfase usando 12:00 UTC.

#### T01 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Preparar examen de Física I`.
- Descripción: `Resolver problemas 4–12 y resumir cinemática en una hoja antes del laboratorio.`
- Categoría: no existe; no introducir.
- Color: no existe en tareas.
- Prioridad: no existe.
- Fecha: `D+2` → 17/09/2026.
- Hora: no existe.
- Duración: no existe.
- Estado: **Pendiente** (valor inicial; no marcar checkbox).

#### T02 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Revisar Pull Request de autenticación de Pulso`.
- Descripción: `Comprobar refresh token, manejo de errores y cobertura del flujo Android.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+1` → 16/09/2026.
- Estado: **Pendiente**.

#### T03 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Laboratorio Web Security — Juice Shop`.
- Descripción: `Completar retos de autenticación rota y guardar evidencias para el informe.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+4` → 19/09/2026.
- Estado: **Pendiente**.

#### T04 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Documentar API del proyecto Pulso`.
- Descripción: `Actualizar endpoints, payloads y ejemplos de respuesta para el equipo.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+10` → 25/09/2026.
- Estado: **Pendiente**.

#### T05 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Estudiar fundamentos de Application Security`.
- Descripción: `Repasar OWASP Top 10, threat modeling y controles de sesión.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+6` → 21/09/2026.
- Estado: **Pendiente**.

#### T06 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Preparar demo de Arquitectura de Software`.
- Descripción: `Cerrar diagrama de componentes y anotar trade-offs para la revisión del viernes.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+3` → 18/09/2026.
- Estado: **Pendiente**.

#### T07 — pendiente

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Configurar pipeline CI para Aura`.
- Descripción: `Separar lint, unit tests e instrumentación en el workflow de integración.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D+12` → 27/09/2026.
- Estado: **Pendiente**.

#### T08 — pendiente, sin fecha

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Resolver ejercicios de estructuras de datos`.
- Descripción: `Completar árboles AVL y justificar la complejidad amortizada de cada solución.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: **dejar `Añadir fecha` sin seleccionar**.
- Estado: **Pendiente**.

#### T09 — completar después de crear

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Modelar esquema Firestore de Aura`.
- Descripción: `Definir colecciones users/todos, habits, completions y journals.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D-7` → 08/09/2026.
- Estado inicial: Pendiente.
- Después de guardar: toca el **checkbox** de esta fila una vez; debe pasar a Completadas.

#### T10 — completar después de crear

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Cerrar issue de navegación en Pulso`.
- Descripción: `Verificar back stack y apertura desde la notificación de finalización.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D-5` → 10/09/2026.
- Estado inicial: Pendiente; después de guardar, toca su checkbox.

#### T11 — completar después de crear

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Entregar informe de Redes II`.
- Descripción: `Revisar topología, direccionamiento y pruebas de latencia antes de subirlo.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D-8` → 07/09/2026.
- Estado inicial: Pendiente; después de guardar, toca su checkbox.

#### T12 — completar después de crear

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Revisar cobertura del módulo de hábitos`.
- Descripción: `Añadir casos de recurrencia semanal y mensual al conjunto de pruebas.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D-3` → 12/09/2026.
- Estado inicial: Pendiente; después de guardar, toca su checkbox.

#### T13 — completar después de crear, sin fecha

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Publicar notas de la reunión de proyecto`.
- Descripción: `Registrar decisiones, responsables y próximos pasos para el equipo.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: **dejar sin fecha**.
- Estado inicial: Pendiente; después de guardar, toca su checkbox.

#### T14 — completar después de crear

- Pantalla/acción: `Tareas` → `Nueva tarea`.
- Título: `Refactorizar validación del formulario de Aura`.
- Descripción: `Eliminar duplicación y dejar mensajes claros para estados inválidos.`
- Categoría/color/prioridad/hora/duración: no existen.
- Fecha: `D-10` → 05/09/2026.
- Estado inicial: Pendiente; después de guardar, toca su checkbox.

Al finalizar deben quedar **8 filas en Pendientes** (T01–T08) y **6 en Completadas** (T09–T14). Si la lista no conserva el orden de creación, busca cada fila por su título; el repositorio de Todo no declara un `orderBy`.

### 4.2 Hábitos — crear 5 elementos y marcar los últimos 7 días

**Secuencia para cada hábito:** Hábitos → `Nuevo hábito` → escribe Nombre → selecciona Frecuencia → ajusta Objetivo si es semanal/mensual → selecciona Color → `Guardar` → espera la tarjeta. Las tarjetas pueden aparecer en un orden distinto al de creación; usa el nombre.

#### H01 — diario, completo hoy

- Pantalla/acción: `Hábitos` → `Nuevo hábito`.
- Nombre: `Revisar fundamentos de Application Security`.
- Frecuencia: `Diario` / `DAILY`.
- Objetivo: queda en `1` (no se edita).
- Color: **Violeta**, `#7C6AE6`.
- Después de guardar, en `Últimos 7 días` toca los siete círculos de `D-6, D-5, D-4, D-3, D-2, D-1 y D`.
- Resultado: `1 / 1` del día y racha visible cercana a `7`.

#### H02 — semanal, avance parcial

- Pantalla/acción: `Hábitos` → `Nuevo hábito`.
- Nombre: `Practicar algoritmos antes de clase`.
- Frecuencia: `Semanal` / `WEEKLY`.
- Objetivo: `4`.
- Color: **Azul**, `#6A9AE8`.
- Marca en los últimos siete días: `D-6`, `D-5`, `D-1` y `D`.
- Resultado actual: `2 / 4` de la semana; con el suplemento backend de §5 la racha queda en `6`.

#### H03 — semanal, seguridad práctica

- Pantalla/acción: `Hábitos` → `Nuevo hábito`.
- Nombre: `Laboratorio Web Security — Juice Shop`.
- Frecuencia: `Semanal` / `WEEKLY`.
- Objetivo: `3`.
- Color: **Ámbar**, `#E9B949`.
- Marca `D-6` y `D-1`.
- Resultado actual: `1 / 3` de la semana; el segundo registro visible es parte de la semana anterior.

#### H04 — mensual, avance medio

- Pantalla/acción: `Hábitos` → `Nuevo hábito`.
- Nombre: `Revisar backlog personal`.
- Frecuencia: `Mensual` / `MONTHLY`.
- Objetivo: `8`.
- Color: **Coral**, `#FF8D70`.
- Marca `D-7`, `D-4` y `D-1`.
- Resultado actual: `3 / 8` desde la UI; §5 añade dos registros antiguos para dejar `5 / 8`.

#### H05 — diario, no perfecto

- Pantalla/acción: `Hábitos` → `Nuevo hábito`.
- Nombre: `Escribir bitácora técnica de Aura`.
- Frecuencia: `Diario` / `DAILY`.
- Objetivo: queda en `1`.
- Color: **Menta**, `#51B89E`.
- Marca solo `D-1`; **no marques D**.
- Resultado actual: `0 / 1` del día y una pequeña racha previa, que hace que la cuenta parezca usada pero no artificialmente perfecta.

La opción **Malva `#B779D1`** queda auditada como disponible pero no se usa en esta versión de cinco hábitos para evitar una pantalla saturada. Si quieres una variante de captura, edita H05 a Malva; no crees un sexto hábito salvo que la tablet tenga espacio suficiente.

### 4.3 Diario — crear 6 entradas

**Secuencia para cada entrada:** Diario → `Escribir` → escribe Título → escribe Contenido → `Guardar` en la barra superior → espera a volver a la lista. La creación manual asigna la fecha actual a todas las entradas; para ver los grupos de agosto/septiembre de la captura recomendada, usa la siembra backend de §5.

#### J01 — entrada más reciente

- Pantalla/acción: `Diario` → `Escribir`.
- Título: `Una semana de decisiones pequeñas`.
- Contenido exacto:

  ```text
  Hoy cerré el mapa de trabajo de Aura: ocho tareas pendientes, tres hábitos activos y un espacio claro para el enfoque. La prioridad sigue siendo terminar el laboratorio de seguridad sin sacrificar la documentación.
  ```

#### J02

- Título: `Qué aprendí del laboratorio de seguridad`.
- Contenido exacto:

  ```text
  En Juice Shop confirmé que una prueba útil no solo encuentra un fallo: también deja una evidencia que otra persona puede repetir. Mañana voy a convertir los hallazgos en una pequeña guía para el equipo.
  ```

#### J03

- Título: `Pulso: menos superficie, más claridad`.
- Contenido exacto:

  ```text
  Pulso se siente más simple después de separar la sesión de autenticación del resto de la navegación. El siguiente paso es revisar el contrato del refresh token con una prueba de integración.
  ```

#### J04

- Título: `La parte difícil de entender redes`.
- Contenido exacto:

  ```text
  La parte difícil de Redes II no fue memorizar comandos, sino explicar por qué cada salto existe. Dibujar la topología antes de tocar la configuración me ahorró varias vueltas.
  ```

#### J05

- Título: `Primer borrador de Aura`.
- Contenido exacto:

  ```text
  El primer borrador de Aura tenía demasiadas tarjetas y muy poco ritmo. Al reducir la jerarquía a una acción principal, el producto empezó a sentirse como una herramienta que podría usar cada día.
  ```

#### J06

- Título: `Ritual de estudio antes de clase`.
- Contenido exacto:

  ```text
  Antes de la clase de Sistemas Distribuidos preparo una pregunta concreta y dejo el teléfono fuera de alcance. Veinte minutos de lectura enfocada rinden más que una sesión larga interrumpida.
  ```

Después de crear J01–J06, la lista ya no está vacía y muestra extractos legibles. No borres entradas para reordenarlas: la fecha automática es parte del contrato actual.

### 4.4 Pomodoro — preparar el estado local de la tablet

Pomodoro no tiene historial remoto ni relación técnica con Tareas. Para una captura fija conviene dejar **modo POMODORO, 25m, 3 sesiones completadas y “Después: Pausa corta”**.

1. Abre `Enfoque`.
2. Si aparece un estado en curso, pulsa `Reiniciar` y confirma `Continuar`.
3. Selecciona `5m`.
4. Pulsa `Saltar` seis veces, cerrando el aviso `Continuar` después de cada salto. La secuencia alterna Pomodoro/descanso y deja `sessionsCompleted = 3` en modo Pomodoro.
5. Selecciona `25m`; no pulses Iniciar para la captura estática. Si el cambio muestra confirmación, elige `Continuar`.
6. La pantalla objetivo debe mostrar `25:00`, etiqueta `POMODORO`, `Sesiones: 3` y `Después: SHORT BREAK`/`Pausa corta` según el locale.

Para una captura de interacción, selecciona `5m`, pulsa `Iniciar`, espera unos segundos y pulsa `Pausar`; no dejes el temporizador corriendo durante una sesión de fotos larga. El contador y el estado sobreviven en esa tablet, no en otra.

### 4.5 Ajustes para la sesión de fotos

1. Abre Ajustes desde el engranaje de Home.
2. Selecciona **Aura** para la captura principal. Después puedes repetir la captura de Home en Forest, Clay, Midnight y High contrast.
3. Para mostrar el estado completo de notificaciones, activa el interruptor y concede el permiso Android; establece `08:30` como hora. Esto programa WorkManager y no crea datos de tareas.
4. Si el permiso no está disponible, deja el interruptor apagado: no fuerces un estado visual que el sistema no haya concedido.
5. Vuelve a Home con Atrás; no cierres sesión mientras fotografíes.

## 5. Ruta backend opcional: versión histórica exacta

Esta sección describe qué se sembraría, pero **no lo ejecuta**. Es preferible a la ruta manual cuando necesitas que Diario parezca utilizado durante varias semanas y que las rachas incluyan períodos anteriores. Usa una cuenta de demo vacía; no borres ni reemplaces datos de una cuenta existente.

### 5.1 Procedimiento seguro, sin cambios de código

1. En Firebase Console → Authentication copia el UID de la cuenta demo.
2. Confirma el proyecto activo (`aura-6ac09` según la configuración inspeccionada) y las reglas antes de escribir.
3. Prepara un Admin SDK fuera del repositorio, con credenciales guardadas en el gestor de secretos del equipo. No pegues la clave en la APK, PowerShell compartido ni Git.
4. Construye las escrituras solo para las rutas y IDs `demo-*` de las tablas siguientes. Ejecuta primero un dry-run que liste UID, rutas y cantidad (14 Todo, 5 Habit, 23 Completion, 6 Journal).
5. Verifica que no exista una cuenta/colección personal en el UID. Si existe, detén el proceso: este plan no autoriza borrar ni sobrescribir.
6. Escribe en lotes pequeños; comprueba que cada `Result`/promise termina correctamente.
7. Inicia sesión en la tablet con el mismo Google UID, espera los listeners o relanza la app y valida los números de Home.

No siembres una colección `pomodoros`, `sessions`, `statistics`, `categories` o `projects`: el código no las lee y crear documentos allí no cambiará ninguna pantalla.

### 5.2 IDs y campos de tareas

Usa los títulos/descripciones exactos de §4.1. Los números de `dueDate` son 12:00 UTC del día indicado; omite `dueDate` en T08 y T13.

| ID | Estado | Fecha relativa | Fecha concreta D=2026-09-15 | `dueDate` numérico |
|---|---|---|---|---:|
| `demo-t01` | false | D+2 | 2026-09-17 | 1789646400000 |
| `demo-t02` | false | D+1 | 2026-09-16 | 1789560000000 |
| `demo-t03` | false | D+4 | 2026-09-19 | 1789819200000 |
| `demo-t04` | false | D+10 | 2026-09-25 | 1790337600000 |
| `demo-t05` | false | D+6 | 2026-09-21 | 1789992000000 |
| `demo-t06` | false | D+3 | 2026-09-18 | 1789732800000 |
| `demo-t07` | false | D+12 | 2026-09-27 | 1790510400000 |
| `demo-t08` | false | — | — | omitir |
| `demo-t09` | true | D-7 | 2026-09-08 | 1788868800000 |
| `demo-t10` | true | D-5 | 2026-09-10 | 1789041600000 |
| `demo-t11` | true | D-8 | 2026-09-07 | 1788782400000 |
| `demo-t12` | true | D-3 | 2026-09-12 | 1789214400000 |
| `demo-t13` | true | — | — | omitir |
| `demo-t14` | true | D-10 | 2026-09-05 | 1788609600000 |

En todos los documentos usa `title` y `description` de §4.1, más `isCompleted` y `dueDate` de esta tabla. No agregues campos ficticios esperando que aparezcan en la UI.

### 5.3 IDs y campos de hábitos

| ID | Nombre | `recurrenceType` | `targetCount` | Color | `createdAt` |
|---|---|---|---:|---|---:|
| `demo-h01` | Revisar fundamentos de Application Security | `DAILY` | 1 | `#7C6AE6` | 1787659200000 (2026-08-25) |
| `demo-h02` | Practicar algoritmos antes de clase | `WEEKLY` | 4 | `#6A9AE8` | 1787918400000 (2026-08-28) |
| `demo-h03` | Laboratorio Web Security — Juice Shop | `WEEKLY` | 3 | `#E9B949` | 1788264000000 (2026-09-01) |
| `demo-h04` | Revisar backlog personal | `MONTHLY` | 8 | `#FF8D70` | 1788350400000 (2026-09-02) |
| `demo-h05` | Escribir bitácora técnica de Aura | `DAILY` | 1 | `#51B89E` | 1788609600000 (2026-09-05) |

### 5.4 Completaciones exactas

Para cada fila crea `users/{UID}/completions/{ID}`. El `habitId` es el ID de la segunda columna; `completedDate` es la fecha ISO; `completedAt` puede ser el número noon-UTC de la última columna. No dupliques el mismo par hábito/fecha: el toggle del repositorio busca ese par y eliminaría el primero que encuentre.

| ID de completación | `habitId` | `completedDate` | `completedAt` |
|---|---|---|---:|
| `demo-h01-2026-09-09` | `demo-h01` | 2026-09-09 | 1788955200000 |
| `demo-h01-2026-09-10` | `demo-h01` | 2026-09-10 | 1789041600000 |
| `demo-h01-2026-09-11` | `demo-h01` | 2026-09-11 | 1789128000000 |
| `demo-h01-2026-09-12` | `demo-h01` | 2026-09-12 | 1789214400000 |
| `demo-h01-2026-09-13` | `demo-h01` | 2026-09-13 | 1789300800000 |
| `demo-h01-2026-09-14` | `demo-h01` | 2026-09-14 | 1789387200000 |
| `demo-h01-2026-09-15` | `demo-h01` | 2026-09-15 | 1789473600000 |
| `demo-h02-2026-09-07` | `demo-h02` | 2026-09-07 | 1788782400000 |
| `demo-h02-2026-09-08` | `demo-h02` | 2026-09-08 | 1788868800000 |
| `demo-h02-2026-09-09` | `demo-h02` | 2026-09-09 | 1788955200000 |
| `demo-h02-2026-09-10` | `demo-h02` | 2026-09-10 | 1789041600000 |
| `demo-h02-2026-09-14` | `demo-h02` | 2026-09-14 | 1789387200000 |
| `demo-h02-2026-09-15` | `demo-h02` | 2026-09-15 | 1789473600000 |
| `demo-h03-2026-09-07` | `demo-h03` | 2026-09-07 | 1788782400000 |
| `demo-h03-2026-09-08` | `demo-h03` | 2026-09-08 | 1788868800000 |
| `demo-h03-2026-09-09` | `demo-h03` | 2026-09-09 | 1788955200000 |
| `demo-h03-2026-09-14` | `demo-h03` | 2026-09-14 | 1789387200000 |
| `demo-h04-2026-09-02` | `demo-h04` | 2026-09-02 | 1788350400000 |
| `demo-h04-2026-09-05` | `demo-h04` | 2026-09-05 | 1788609600000 |
| `demo-h04-2026-09-08` | `demo-h04` | 2026-09-08 | 1788868800000 |
| `demo-h04-2026-09-11` | `demo-h04` | 2026-09-11 | 1789128000000 |
| `demo-h04-2026-09-14` | `demo-h04` | 2026-09-14 | 1789387200000 |
| `demo-h05-2026-09-14` | `demo-h05` | 2026-09-14 | 1789387200000 |

Con este conjunto, H01 queda `1/1` y racha 7; H02 queda `2/4` en la semana actual y racha 6; H03 queda `1/3` y racha 4; H04 queda `5/8`; H05 queda `0/1` hoy. Home debe calcular `3/5` y racha máxima `7`.

### 5.5 IDs y campos de diario

Usa el título y contenido exactos de §4.3. `createdAt` y `updatedAt` son números, no objetos Timestamp: el mapper actual llama `getLong`. Los valores noon-UTC mantienen el día correcto en La Paz.

| ID | Fecha visible | `createdAt` | `updatedAt` |
|---|---|---:|---:|
| `demo-j01` | 2026-09-15 | 1789473600000 | 1789473600000 |
| `demo-j02` | 2026-09-12 | 1789214400000 | 1789214400000 |
| `demo-j03` | 2026-09-08 | 1788868800000 | 1788868800000 |
| `demo-j04` | 2026-09-03 | 1788436800000 | 1788436800000 |
| `demo-j05` | 2026-08-27 | 1787832000000 | 1787832000000 |
| `demo-j06` | 2026-08-19 | 1787140800000 | 1787140800000 |

El resultado visual tendrá un grupo `September 2026` con cuatro entradas y `August 2026` con dos. Si la UI está en español, los encabezados y días se traducen automáticamente.

### 5.6 Qué no sembrar y qué hacer con lo local

- No crear documentos de Pomodoro: no tienen consumidor.
- No crear documentos de categorías, prioridades, proyectos, sesiones o estadísticas: no existen en el modelo.
- No modificar Remote Config global para toda producción solo para una captura. Si se necesita mostrar Hábitos, usa un entorno/proyecto de demo o un entitlement de prueba controlado.
- En la tablet configura manualmente tema, permiso/hora, onboarding y Pomodoro; esos valores no viajarán con Firestore.
- No borrar la caché de la app ni limpiar datos para “ordenar” la demo: podría eliminar preferencias locales y el contador que quieres conservar.

## 6. Validación después de introducir los datos

1. En Home confirma `8` tareas pendientes, tarjeta de hábitos `3 / 5` y racha `7` (si Hábitos está disponible).
2. En Tareas confirma que T08 y T13 no muestran fecha, que hay descripciones de dos líneas y que seis filas aparecen tachadas en Completadas.
3. En Hábitos confirma cinco colores, tres tipos de frecuencia, progreso parcial y la tira `Últimos 7 días`; toca una fecha solo si quieres comprobar el feedback, no durante la captura final.
4. En Diario confirma los grupos mensuales. Abre J01 para una captura de texto largo y vuelve con Atrás sin descartar cambios.
5. En Enfoque confirma `25:00`, modo Pomodoro, `Sesiones: 3` y siguiente descanso corto. No confundas este contador local con estadísticas históricas.
6. En Ajustes confirma Aura, el estado de permiso que realmente concedió Android y una hora válida como `08:30`.
7. Cierra y abre la aplicación una vez con red activa para verificar que las cuatro colecciones se vuelven a leer. No uses “Borrar datos” del sistema.

## CAPTURAS RECOMENDADAS

### 1. Home editorial — captura principal

- Abrir: `Home` en tema Aura/Purple.
- Datos visibles: hero con `8` pendientes, tarjeta Hábitos `3/5`, `Registros en racha: 7`, atajo `Abrir enfoque`.
- Estado: contenido cargado, sin skeleton ni error.
- Scroll: no; encuadra el encabezado, hero, hábitos y atajo.
- Destacar: marca orbital, jerarquía del hero y aro de progreso.
- Por qué: resume en una sola imagen la identidad visual y la relación Todo/Hábitos/Enfoque.

### 2. Tareas — trabajo real y estados

- Abrir: `Tareas`.
- Datos visibles: varias pendientes con títulos técnicos, fechas futuras/descripciones y comienzo de `Completadas` con filas tachadas.
- Estado: lista cargada; evita una hoja de edición abierta salvo que quieras mostrar el formulario.
- Scroll: un desplazamiento corto puede mostrar la frontera Pendientes/Completadas; no intentes encajar las 14 filas.
- Destacar: contraste entre filas activas y completadas, fechas y CTA `Nueva tarea`.
- Por qué: demuestra CRUD, densidad realista y estados sin inventar categorías.

### 3. Hábitos — continuidad imperfecta

- Abrir: `Hábitos` con entitlement Premium activo.
- Datos visibles: H01 y H02 completos/parciales, H03–H05 con colores distintos, progreso del período y `Últimos 7 días`.
- Estado: cargado, sin modal.
- Scroll: en teléfono, desplaza hasta mostrar dos o tres tarjetas; en tablet usa el ancho completo para mostrar más.
- Destacar: círculos de fecha, progreso `2/4` o `5/8`, colores y racha.
- Por qué: es la pantalla que mejor prueba que la cuenta tiene varios días de uso y no solo filas creadas hoy.

### 4. Enfoque — pantalla protagonista

- Abrir: `Enfoque` en Midnight/Dark para una variante fuerte; conserva también una toma Aura clara.
- Datos visibles: dial grande `25:00`, etiqueta POMODORO, `Sesiones: 3`, `Después: Pausa corta`.
- Estado: listo o pausado, nunca un contador accidentalmente en cero.
- Scroll: no en teléfono; si la tipografía del sistema es grande, desplaza solo lo necesario para incluir `Después`.
- Destacar: aro orbital, contraste oscuro y acción central.
- Por qué: presenta el rasgo más memorable del producto y demuestra que el estado local del timer funciona.

### 5. Diario — producto utilizado durante semanas

- Abrir: `Diario` con la siembra histórica de §5.
- Datos visibles: encabezados September/August, cuatro/dos entradas y extractos distintos.
- Estado: lista cargada, sin confirmación de borrado.
- Scroll: un desplazamiento corto si quieres incluir ambos meses; no hace falta llegar al final.
- Destacar: columna de día, agrupación mensual y ritmo editorial de las tarjetas.
- Por qué: comunica continuidad personal y da profundidad a la demo sin añadir una funcionalidad inexistente.

### 6. Editor de diario o Ajustes — detalle de calidad

- Opción A: abrir J01 y mostrar título + párrafo completo; no modificarlo durante la captura.
- Opción B: abrir Ajustes y mostrar los cinco temas, dejando Aura seleccionado; captura útil para enseñar el sistema de diseño.
- Estado: editor limpio/guardado o Ajustes estable; no mostrar un error salvo que la captura sea específicamente de resiliencia.
- Scroll: el editor puede desplazarse si el teclado aparece; Ajustes suele requerir un desplazamiento corto.
- Destacar: tipografía Manrope, campos sin ruido, selección exclusiva de temas y superficies.
- Por qué: complementa las pantallas de resumen con una vista de interacción cuidada.

## 7. Límites conocidos del plan

- Si la cuenta no tiene Premium, Hábitos y parte del resumen de Home no estarán disponibles aunque existan documentos. No modificar el modelo para saltarse el entitlement.
- Crear Diario manualmente no permite falsificar meses anteriores; los grupos históricos requieren backend con timestamps numéricos.
- Las tareas no tienen categorías, prioridades ni duración. Los nombres y descripciones llevan el contexto profesional sin simular campos que Aura no soporta.
- Pomodoro no produce estadísticas ni historial remoto. El contador `3` de la captura es estado local preparado manualmente.
- No se han ejecutado escrituras, scripts de Admin SDK ni cambios de código como parte de este plan.
