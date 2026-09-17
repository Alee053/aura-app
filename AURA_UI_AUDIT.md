# Aura — Auditoría de producto, UI y arquitectura visual

Fecha: 15 de septiembre de 2026. Alcance: checkout actual de `C:\Proyectos\aura-app`, incluidos los cambios locales anteriores a esta auditoría. Documento de auditoría y referencia visual de la implementación local.

## 1. Dictamen

Aura necesita reconstruir la composición de sus pantallas, la jerarquía de acciones y la expresión de sus estados. Cambiar colores y redondear tarjetas no alcanza. El producto ya dispone de una base funcional considerable; la UI no expresa esa profundidad.

**Decisión técnica: conservar Kotlin y Compose Multiplatform.** No se encontró ninguna necesidad visual que justifique migrar. El stack permite dibujo personalizado, composición adaptable, controles accesibles y animaciones de estado. La renovación debe concentrarse en `designsystem`, `presentation`, recursos y navegación. Las capacidades nativas de animación cubren el lenguaje propuesto; véase la [guía oficial de animaciones](https://developer.android.com/develop/ui/compose/animation/quick-guide).

**Dirección elegida: “Ritmo sereno”.** Un producto de productividad personal con claridad editorial, superficies de papel, tinta violeta y un motivo orbital propio. El gesto visual del aro conecta la marca, el progreso y el enfoque. Inicio organiza; Tareas permite actuar; Hábitos muestra continuidad; Pomodoro concentra; Diario invita a escribir. Comparten lenguaje sin repetir el mismo layout.

No incorporar gamificación, puntuaciones de bienestar, gráficas ficticias, IA, redes sociales, calendario avanzado, prioridades persistidas, pagos ni un backend nuevo. No son necesarios para conseguir calidad de portfolio.

## 2. Evidencia y límites

Se revisaron todas las pantallas y componentes de presentación, los ViewModels, las rutas, el tema, el catálogo y configuración de dependencias, los recursos, los modelos y casos de uso relevantes, la composición de Koin y las integraciones Android que condicionan los flujos. Se inspeccionaron repositorios Firestore de tareas/hábitos/diario, autenticación, preferencias, permisos, scheduler y lógica de Pomodoro; también la estructura de tests y el alcance parcial de iOS.

No se recibió ninguna captura actual. `adb devices -l` no encontró dispositivos conectados. No se ejecutaron builds, tests, instalaciones ni cambios de datos. Por tanto:

- **C**: comprobado en código, incluyendo propiedades concretas de composición.
- **D**: evaluación de diseño a partir de ese código; requiere contraste con el render real.
- **V**: validación pendiente en dispositivo: apariencia final, contraste renderizado, insets, TalkBack, teclado, rendimiento y comportamiento del sistema.

No se presentan métricas de rendimiento ni tests como ejecutados. Que Android funciona en un dispositivo real es contexto aportado por el propietario.

El worktree ya contiene modificaciones de aplicación, Firebase y Gradle. Son la base a preservar, no archivos que se deban restaurar. No se encontraron archivos `AGENTS.md` mediante la búsqueda en el repositorio. `WORKANA_AUDIT_REPORT.md` es antecedente, no evidencia de ejecución actual. El README está desactualizado en puntos: por ejemplo, dice que Loco corre en cada build, pero el Gradle actual declara la sincronización manual.

## 3. Qué hace Aura realmente

### 3.1 Inventario de experiencias

| Experiencia | Entrada y comportamiento comprobado | Fuente principal |
|---|---|---|
| Resolución de sesión | Loading → SignedOut/Error o SignedIn | `App.kt`, `auth/presentation/AuthViewModel.kt` |
| Acceso | Google Credential Manager → token → Firebase Auth; cancelación no se trata como fallo fatal | `auth/presentation/screen/SignInScreen.kt`, Android `MainActivity.kt` |
| Onboarding | Cuatro slides por JSON, después de autenticar; Anterior/Siguiente/Omitir/Empezar | `onboarding/presentation/screen/OnboardingScreen.kt` |
| Inicio | Pendientes, mayor valor de racha, hábitos marcados hoy; motivación condicionada por variante | `home/presentation/screen/HomeScreen.kt` |
| Tareas | Lista activa/completada; alta, edición, borrado, toggle, descripción y fecha opcionales | `todo/presentation/screen/TodoScreen.kt` |
| Editor de tarea | Dialog; DatePickerDialog anidado; guarda y cierra inmediatamente | `todo/presentation/composable/TodoDialog.kt` |
| Hábitos | Progreso por período, racha, últimos siete días, toggle de fechas | `habit/presentation/screen/HabitScreen.kt` |
| Editor de hábito | Nombre, recurrencia diaria/semanal/mensual, objetivo y seis colores | `habit/presentation/composable/HabitDialog.kt` |
| Diario | Lista descendente por creación, alta/edición, borrado por swipe | `journal/presentation/screen/JournalScreen.kt` |
| Detalle de diario | `entryId=null` crea; ID carga/edita; título obligatorio; guardar vuelve | `journal/presentation/screen/JournalDetailScreen.kt` |
| Pomodoro | Iniciar, pausar, reiniciar, saltar; duraciones manuales 5/25/10 minutos | `pomodoro/presentation/PomodoroScreen.kt` |
| Finalización | Overlay global; aparece también fuera de Pomodoro | `App.kt`, `PomodoroCompletionOverlay` |
| Ajustes | Cinco temas, resumen diario, hora/minuto, permiso Android y salir | `settings/presentation/screen/SettingsScreen.kt` |

Las pantallas de Google, el permiso Android y los pickers de sistema no son pantallas de producto libremente rediseñables. Aura puede diseñar el contexto previo y posterior, no imitar ni reemplazar credenciales del sistema.

### 3.2 Flujo y navegación existentes

```text
App / resolución de sesión
 ├─ SignedOut o Error → Acceso Google
 └─ SignedIn
     ├─ onboarding pendiente → 4 slides
     │   ├─ Omitir → acceso durante esta sesión
     │   └─ Empezar → preferencia persistida
     └─ AuthenticatedApp → Home
         ├─ Todo → crear/editar → selector de fecha
         ├─ Habit → crear/editar [si tiene acceso]
         ├─ Pomodoro → finalización global
         ├─ Journal → JournalDetail(null o ID)
         └─ Settings [desde Home] → permiso/hora/tema/salir
```

Hay siete tipos de ruta: Home, Todo, Habit, Settings, Journal, Pomodoro y JournalDetail. Acceso y onboarding están fuera del NavHost. La barra actual ordena Tareas, Hábitos, Inicio, Pomodoro, Diario, omitiendo opciones por flags. Usa `launchSingleTop` y `popUpTo`, pero no configura `saveState/restoreState`. Ajustes carece de botón visual Atrás. Diario detalle conserva la barra principal y deja sin selección la pestaña Diario, porque es una ruta hermana.

Hábitos exige **HABITS_ENABLED y UserPlan.Premium** mediante `GetHabitsAccessibilityUseCase`; no basta con un flag. La variante Free oculta motivación diaria. No existe flujo de compra: no diseñar un paywall operativo ni permitir activar Premium desde Ajustes.

### 3.3 Datos que la nueva UI puede afirmar

| Dato | Significado real | Consecuencia de diseño |
|---|---|---|
| `incompleteTodos` | Todas las tareas no completadas | Etiqueta “Tareas pendientes”; nunca “de hoy” |
| `completedHabitsToday / totalHabitsToday` | Hábitos marcados hoy / tamaño de lista | “Marcados hoy”; no obligaciones vencidas ni porcentaje de productividad |
| `currentStreak` | Máximo `streak` entre hábitos | No llamarlo “días consecutivos” universalmente |
| `streak` | Acumula completaciones de períodos consecutivos, más período actual | Mostrar “Registros en racha” y explicación; conservar cálculo |
| `currentPeriodProgress` | Registros / objetivo del período | Barra limitada visualmente a 100%; texto conserva valores reales |
| `last7Days` | Siete fechas móviles terminando hoy | Derivar etiqueta del día de cada fecha; no asumir lunes-domingo |
| `sessionsCompleted` | Contador persistido; saltar enfoque también lo incrementa | No llamarlo “sesiones de hoy” ni “tiempo trabajado” |
| `progress` Pomodoro | Tiempo restante / tiempo inicial | El aro se vacía; no invertirlo por estética |

No hay datos de perfil en `AuthState`, histórico agregado de productividad, puntuación personal, calendario de planificación, etiquetas, fotos de usuario ni métricas de diario en DashboardData. Cualquier maqueta que los muestre como reales sería engañosa.

### 3.4 Arquitectura que se conserva

- `composeApp`: features con `presentation`, `domain`, `data`, `di`; UI compartida en commonMain, adaptadores Android/iOS.
- `designsystem`: tres componentes (`PrimaryButton`, `BasicInput`, `AuraHorizontalDivider`) y tema (`Color`, `DsTheme`, `Type`). La dependencia debe seguir siendo aplicación → sistema de diseño.
- Android Auth: nonce, cliente OAuth generado desde Google Services, fallback de cuentas autorizadas, callbacks y listener Firebase. No rediseñar esta integración.
- Firestore: `users/{uid}/todos`, `habits`, `completions`, `journals`. Preservar IDs, campos, timestamps, listeners y aislamiento por usuario. Room está declarado; no es el almacén de estos flujos Android.
- DataStore: `theme_mode` guarda el nombre de `ThemeMode`; onboarding, preferencias de notificación y estado Pomodoro también persisten. No renombrar enums ni claves.
- Pomodoro: cálculo por fecha absoluta de fin; sincroniza al volver a foreground y desde eventos de notificación. El ViewModel vive por encima de las pantallas. No introducir un segundo reloj para animar.
- WorkManager, FCM, canales, workers, Remote Config y experimentos quedan fuera de la renovación funcional.
- iOS tiene host y targets, pero autenticación y partes de datos son stubs. No prometer paridad iOS; sí evitar imports Android en commonMain.

### 3.5 Dependencias y recursos

Catálogo observado: Kotlin 2.2.10, Compose Multiplatform 1.10.3, Material3 1.10.0-alpha05, Navigation Compose 2.9.2, Koin 4.1.1, kotlinx-datetime 0.6.1; Android minSdk 24, compile/target 36. Son versiones declaradas, no una resolución de dependencias ejecutada. Hay iconos Material extended, recursos Compose, lifecycle, DataStore, Firebase, WorkManager, Turbine, Mockative y dependencia de test UI Android.

No hay fuentes personalizadas, ilustraciones de producto ni biblioteca de assets de onboarding. Sus `image_url` están vacías y la UI elige iconos según ID. Hay un drawable de Compose y recursos launcher Android/iOS; el foreground Android usa una flor tipo Spa. No hay un lenguaje gráfico propio más allá de ese símbolo.

No hacen falta Lottie, Rive, Coil, Accompanist, un nuevo navegador ni shaders. Declarar explícitamente el artefacto de animación de la misma versión Compose si se consume directamente; no actualizar todo el stack. Material3 ya es alpha: encapsular APIs experimentales en wrappers concretos.

## 4. Auditoría crítica transversal

Prioridades: P0 = riesgo de datos/orientación falsa; P1 = imprescindible para percepción profesional; P2 = pulido.

| Prioridad / evidencia | Hallazgo concreto | Decisión |
|---|---|---|
| P1 C/D | Cards redondeadas, icono circular, fondo degradado y bordes muy similares en casi todas las features | Eliminar esa composición como receta universal |
| P1 C/D | Inicio apila dos números enormes y una frase con el mismo componente | Dar un papel distinto a resumen, progreso y contenido editorial |
| P0 C | “TAREAS DE HOY” representa todas las pendientes | Corregir copy, conservar agregación |
| P0 C | Home use case transforma fallos de fuentes en ceros dentro de Result.success | No inventar estado error desde la UI; resolver observabilidad según plan |
| P1 C | Motivación recibe `onClick={}` en una card clickable | Superficie estática, sin ripple ni semántica de botón |
| P1 C | Barra flotante en todas las rutas, scaffolds anidados y consumo de insets repartido | Un propietario de barras del sistema; barra solo en destinos principales |
| P0 C | Editores Todo/Habit llaman save y dismiss seguidos | Mantener editor hasta resultado confirmado; conservar borrador al fallar |
| P0 C | Borrar Diario en detalle vuelve antes de confirmar; swipe borra directamente | Confirmar intención y esperar resultado; no ofrecer Undo ficticio |
| P0 C | Detalle Diario no representa `isLoading`; ID no encontrado puede parecer alta nueva | Diferenciar cargar, editar, crear, no encontrado y fallo |
| P1 C | Grilla de hábito rotula por índice lunes-domingo, pero fechas son móviles | Calcular etiquetas usando la fecha real |
| P1 C/V | Días de 30dp, colores y steppers de 32dp; limpiar fecha de 24dp | Reservar hitboxes de 48dp sin solapamiento, verificar dispositivo |
| P1 C | Incrementar/decrementar objetivo sin descripción; colores sin nombre/selección accesible | Semántica de acción, radio y estado localizada |
| P1 C/V | Check blanco sobre colores pastel de hábito y uso de coral como texto | El color de usuario no determina texto/check; contraste por tokens |
| P1 C | Temas mutuamente excluyentes usan Switch | Radio y miniaturas reales, una única selección |
| P1 C | Hora de notificación se persiste en cada edición de dígitos | Selector con borrador y una confirmación de hora completa |
| P1 C | Auth Loading reemplaza el acceso por spinner global | Distinguir visualmente arranque e inicio de sesión en curso |
| P1 C | Errores se limpian tras snackbar; listas vacías pueden verse como primera visita tras fallar carga | Error de carga persistente independiente de errores de acción |
| P1 C/D | Sin sistema explícito de motion salvo scroll de pager; Material aporta comportamiento interno | Motion coherente para cambios de estado, orientación y confirmación |
| P1 C/V | Pomodoro usa Column SpaceBetween sin scroll y círculo grande | Composición por altura disponible; alternativa horizontal |
| P1 C/D | Fuentes SansSerif; escalas declaradas pero tamaños/radios/alphas se repiten localmente | Tokens completos y tipografía de marca |
| P2 C | Fechas ISO expuestas al usuario, textos de 11/12sp, tracking del timer local | Formateo localizado y roles legibles |
| P1 C | Onboarding promete priorizar, deslizar tareas y resumen de diario no presentes | Reescribir descripciones manteniendo JSON y flujo |

No toda descripción nula es un fallo: es correcta en adornos. El problema está en controles sin nombre y en estados que solo se reconocen por color. Compose puede ampliar áreas táctiles automáticamente, pero eso no garantiza ausencia de solapamientos en la grilla actual; reservar espacio real. Referencia: [defaults de accesibilidad de Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

## 5. Nueva identidad: Ritmo sereno

### 5.1 Principios no negociables

1. Una acción dominante por contexto. La navegación no compite con Guardar ni con Iniciar.
2. Los datos forman la composición. Ningún porcentaje o estadística de adorno.
3. Densidad intencional: Tareas compacta, Hábitos explicativa, Diario editorial, Enfoque espacial.
4. El movimiento explica un cambio. No mantener adornos animados consumiendo atención.
5. La tranquilidad no reduce legibilidad. Texto pequeño, transparencias y contraste bajo no son una estética aceptable.
6. Marca constante entre temas. Color alternativo no significa otra aplicación.

### 5.2 Color y superficies

Tema principal claro: papel cálido `#F7F5F2`, tinta `#24212C`, violeta `#5B4DDE`. Se conserva el violeta existente porque sirve a la identidad, pero se reemplaza la distribución de superficies. El coral pasa a acento ornamental, nunca a señal de urgencia.

| Token | Claro principal | Oscuro | Alto contraste |
|---|---|---|---|
| background | #F7F5F2 | #111018 | #000000 |
| surface | #FFFFFF | #1D1A27 | #000000 |
| surfaceMuted | #EEEBF3 | #2A2637 | #151515 |
| textPrimary | #24212C | #F6F2FF | #FFFFFF |
| textSecondary | #625D6C | #C5BED1 | #FFFFFF |
| primary / onPrimary | #5B4DDE / #FFFFFF | #C5B8FF / #281E57 | #FFFF00 / #000000 |
| primaryContainer / onPrimaryContainer | #E8E3FC / #30245F | #3B315F / #EEE7FF | #252500 / #FFFF00 |
| outlineDecorative | #DDD7E4 | #4B435B | #FFFFFF |
| controlOutline | #797181 | #A49AAF | #FFFFFF |
| success / successContainer | #236B53 / #E2F2E9 | #8DD9B8 / #173A2D | #73FFB3 / #002516 |
| warning / warningContainer | #805200 / #FFF0D0 | #FFD18A / #443016 | #FFFF00 / #252500 |
| error / errorContainer | #B32635 / #FCE8EB | #FFB3BE / #4A202A | #FFB3BE / #32000C |
| accent / onAccent | #F3AB8E / #542B1D | #F3AB8E / #542B1D | #FFFF00 / #000000 |
| hero / onHero | #29213F / #FFFFFF | #302741 / #FFFFFF | #000000 / #FFFFFF |

`onSuccessContainer`, `onWarningContainer` y `onErrorContainer` usan el correspondiente color semántico de la tabla; controles y texto deben comprobarse con el color final compuesto. Separar borde ornamental de borde necesario para reconocer un input. Objetivos de aceptación del proyecto: texto normal ≥4.5:1, texto grande ≥3:1, indicadores/controles esenciales ≥3:1; alto contraste busca ≥7:1 para texto. Son requisitos por validar, no ratios medidos aquí.

GREEN conserva su enum y pasa a “Bosque”: primary `#08745F`, onPrimary blanco, container `#D9F0E5`, onContainer `#153C30`, hero `#173B31`. RED conserva enum y pasa a “Arcilla”: primary `#A33750`, container `#F8E1E6`, onContainer `#542031`, hero `#482431`, onPrimary blanco. Ambos usan papel/tinta del tema claro. PURPLE = “Aura”; DARK = “Medianoche”; HIGH_CONTRAST = “Alto contraste”. No introducir “seguir sistema” sin diseñar una preferencia nueva.

Superficies: fondo sin degradado global; listas con filas y separadores; cards solo para bloques que realmente se agrupan. Una hero de tinta en Inicio. Pomodoro utiliza una base clara u oscura según tema y un halo estático de baja opacidad detrás del aro. En alto contraste se eliminan halos y transparencias semánticas.

Elevación: 0dp en filas/cards de contenido; 2dp en barra y acciones elevadas; 6dp en sheets; 8dp en dialogs. Sin sombras apiladas sobre bordes en cada item. Scrim negro al 40%, fijo por token.

### 5.3 Tipografía, medidas y forma

Familia elegida: Manrope empaquetada localmente, pesos 400/500/600/700. No fuente descargada en runtime. Se obtiene del [repositorio de Google Fonts, con su licencia](https://github.com/google/fonts/tree/main/ofl/manrope). Para evitar depender del soporte variable del mínimo Android, preparar instancias estáticas en fase de assets. Recursos de fuente compartidos conforme a [recursos de Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html).

| Rol | Tamaño/interlineado | Peso y uso |
|---|---|---|
| Timer | 64/72sp; 48/56sp en composición compacta | 500, cifras con ancho estable |
| Display | 40/48sp | 600, marca/hero; no todos los KPIs |
| Headline | 30/38sp | 700, título principal |
| Section | 22/30sp | 600 |
| Title | 18/26sp | 600, títulos de item |
| Body | 16/24sp | 400, lectura e inputs |
| BodySmall | 14/20sp | 400, ayudas |
| Label | 14/20sp | 600, botones/chips |
| Caption | 12/18sp | 500, metadatos secundarios |

Tracking normal 0; títulos grandes -0.4sp. No usar mayúsculas espaciadas para párrafos. En Diario, cuerpo 17/28sp con ancho de lectura máximo 640dp. No reducir fuente para hacer caber botones.

Spacing base de 4dp: 4/8/12/16/24/32/48/64. Margen de pantalla 24dp; 16dp bajo 360dp. Gaps de lista 0 para filas y 12 para cards. Padding de card 20dp. Radios: 8 insignias, 12 inputs/filas seleccionadas, 16 botones, 24 cards, 28 sheets, círculo/píldora únicamente donde la forma comunica selección o progreso. Botón principal minHeight 56dp; input minHeight 56dp; targets min 48×48dp.

### 5.4 Marca y recursos

Sustituir Spa como logo por un aro abierto con pequeño punto terminal: viewport 24, centro 12/12, radio 8, trazo 2.4 redondeado, arco desde -90° de 300°, punto al extremo. Escalar proporcionalmente; en icono launcher usar zona segura y fondo violeta. En UI no sugiere carga salvo que esté explícitamente en un estado de progreso.

Crear cuatro pequeñas escenas vectoriales/Compose para onboarding: lista con una tarea marcada, secuencia de siete registros, hoja con dos líneas, resumen con dos bloques. Mismo trazo de 2dp, misma tinta y acento; no screenshots falsos de datos personales. Empty states derivan de esas escenas, con máximo 112dp. No requiere generación de imágenes raster.

Iconografía: Material outlined para navegación/acciones, filled en selección, 24dp estándar. Iconos de 16/20dp solo auxiliares. Evitar mezclar emoji, glifos “✓”, ilustración fotográfica y familias de iconos. Check vectorial. Botón Google usa su identidad oficial, con excepciones documentadas a la tipografía de Aura: [guía de Google](https://developers.google.com/identity/branding-guidelines).

### 5.5 Componentes y estados

- Botón primario violeta sólido, secundario tonal, terciario texto, destructivo con etiqueta explícita. Loading conserva ancho y altura, bloquea doble envío y expone estado.
- Input con label permanente, ayuda/error inferior y borde de foco 2dp. Descripción multilineal comparte componente, no copia manual de OutlinedTextField.
- Cards interactivas con acción visible o chevron. Una card informativa no recibe `onClick` vacío.
- Chips de selección con semántica radio; checkbox para completar; switch solo para preferencias binarias.
- Editores cortos en ModalBottomSheet expandida; confirmaciones destructivas en Dialog; Diario en destino completo.
- Formulario con cambios: cerrar/Atrás abre “Descartar cambios”; al enviar, indicador inline y una sola operación. Fallo conserva texto. No afirmar éxito por un cambio optimista del listener.
- Skeleton inicial si la espera supera 150ms; si llegan datos antes, no aparece. Nunca retener datos para cumplir una animación mínima.
- Error de carga sin datos ocupa el cuerpo; error de acción usa ayuda local/snackbar y preserva contenido. No confundir cero datos con fallo.

## 6. Rediseño de cada pantalla y superficie

Las rutas de archivo de esta sección son relativas a `composeApp/src/commonMain/kotlin/com/programovil/aura/`, salvo indicación. Los contratos concretos de ejecución están en `AURA_REDESIGN_PLAN.md`.

### S00 — Resolución de sesión

**Actual:** spinner aislado, sin identidad ni continuidad (C/D). **Objetivo:** resolver acceso sin parpadeos ni retraso artificial. **Composición:** papel, marca orbital centrada 56dp y texto accesible “Preparando Aura”; sin tarjeta. **Componentes/jerarquía:** BrandMark y estado de arranque; identidad secundaria al estado. **Interacción:** ninguna mientras resuelve; no simular progreso porcentual. **Animación:** fundido de salida 160ms, sin mínimo de exposición. **Transición:** a acceso/onboarding/Home según estado real. **Estados:** restauración inicial distinta del envío de credenciales, sin inventar timeout recuperable no soportado. **Reutilizar:** DsTheme y AuthState. **Archivos:** `App.kt`, nuevo `shared/presentation/composable/AuraSessionGate.kt`.

### S01 — Acceso

**Actual:** logo Spa sobre tarjeta genérica; columna no llena altura, por lo que Center no centra en pantalla; loading pierde contexto; error puede exponer excepción técnica (C/D). **Objetivo:** explicar el valor y dar confianza al iniciar sesión. **Composición:** marca pequeña arriba; titular alineado a izquierda “Haz espacio para lo importante”; escena orbital con tres bloques que representan organizar, enfocar y escribir; CTA Google abajo dentro de área segura. **Componentes:** BrandMark, IntroScene, GoogleSignInButton, InlineNotice. **Jerarquía:** promesa → ejemplo visual → acción; sin card contenedora de todo. **Interacciones:** una solicitud activa; cancelar selector permite intentar otra vez. **Animaciones:** escena entra una vez 240ms con desplazamiento 12dp, botón cambia a loading sin saltar. **Transiciones:** conservar layout durante autenticación; fade al siguiente flujo. **Estados:** signed out, picker abierto, autenticación, error legible con reintento; nunca mostrar token/stacktrace. **Reutilizar:** callback y manejo Firebase completo. **Archivos:** `auth/presentation/screen/SignInScreen.kt`, `App.kt`; puente mínimo de estado en `MainActivity.kt` según plan.

### S02 — Onboarding (cuatro páginas)

**Actual:** iconos grandes equivalentes y tarjeta uniforme; pager no admite gesto; texto promete funciones ausentes (C/D). **Objetivo:** enseñar lo que existe sin convertirse en barrera. **Composición:** arriba marca/Omitir, escena propia en mitad superior, número “1 de 4”, título y explicación breve, footer fijo con Anterior y Siguiente/Empezar. **Componentes:** OnboardingScene, PageProgress, botones compartidos. **Jerarquía:** escena 200–240dp adaptativa → título 30sp → descripción de hasta tres líneas a escala normal; scroll en cada página si hace falta. **Interacciones:** mantener botones y pager programático; no habilitar swipe en esta renovación para evitar doble fuente de índice. **Animaciones:** desplazamiento horizontal existente 280ms; indicador 180ms. **Transiciones:** página anterior invierte dirección; Empezar no retrasa la escritura de preferencia. **Estados:** carga, contenido, omisión, final; error mantiene bypass actual. **Reutilizar:** IDs 1–4, JSON localizado, ViewModel y distinción skip/start. **Archivos:** `onboarding/presentation/screen/OnboardingScreen.kt`, `composeResources/files/onboarding_config.json`.

Copy conceptual: 1 “Organiza lo pendiente”: crear, editar y completar; 2 “Da continuidad a tus hábitos”: registros y recurrencia, indicar disponibilidad según plan sin prometer compra; 3 “Encuentra un momento para escribir”: diario; 4 “Tu próximo paso, más claro”: resumen de tareas y hábitos y navegación a enfoque. No afirmar que Home resume diario.

### S03 — Inicio

**Actual:** KPIs con misma silueta, jerarquía repetida y títulos inexactos; motivación parece botón (C/D). **Objetivo:** comprender el estado y elegir el siguiente espacio. **Composición:** header “Tu día, a tu ritmo” + fecha local + Ajustes; hero de tinta “Tareas pendientes” con número 40sp y acción “Ver tareas”; debajo bloque Hábitos con aro pequeño y `marcados/total`, más “Registros en racha” secundario; acceso tonal a “Abrir enfoque”; motivación en bloque editorial estático. **Componentes:** TodayHeader, PendingTasksHero, HabitSummary, FocusShortcut, MotivationNote. **Jerarquía:** tareas dominante cuando habilitadas; hábitos sube a hero si tareas no está disponible; si ninguno, hero editorial con acceso a Enfoque o Diario según disponibilidad. **Interacciones:** cada atajo navega a ruta existente; enfoque no arranca timer desde Home. **Animaciones:** carga→datos con fade 180ms, contador 220ms solo al cambiar valor ya conocido; bloques iniciales máximo tres con 35ms entre ellos. **Transiciones:** a pestañas con fade corto; Ajustes con avance horizontal. **Estados:** carga/sin pendientes/hábitos sin registros/flags/variante Free y Premium; sin ratio cuando total=0. Si todas las features se ocultan, texto de disponibilidad y Ajustes, sin huecos. **Reutilizar:** DashboardData, variante y acceso; no nuevas consultas de contenido por estética. **Archivos:** `home/presentation/screen/HomeScreen.kt`, `home/presentation/composable/DashboardCard.kt` y nuevos componentes; `navigation/AppNavHost.kt` para callbacks Enfoque/Diario.

El contrato de Dashboard actual oculta fallos de fuentes. El plan fija una excepción técnica pequeña: propagar el fallo por el `Result` ya existente, conservando todos los cálculos exitosos. Home muestra indisponibilidad del resumen completo; no inventa estados parciales ni añade listeners para diagnosticarlos.

### S04 — Tareas

**Actual:** todas las filas son tarjetas, poca diferencia entre pendiente, vencida y completada; fecha ISO; estado vacío duplica CTA con FAB (C/D). **Objetivo:** leer rápido y completar con certeza. **Composición:** título + contador de pendientes; sección “Pendientes” con filas separadas por líneas; completadas en grupo plegable; acción extendida “Nueva tarea”. **Componentes:** TaskRow, DueDateLabel, SectionHeader, CollapsibleSection, ExtendedAction, EmptyState. **Jerarquía:** título de tarea → fecha → descripción; estado completado reduce prominencia sin bajar contraste. No agregar prioridad ni filtros que oculten información por defecto. **Interacciones:** checkbox completa; cuerpo edita; completadas expandida inicialmente, expansión recordada por sesión. **Animaciones:** check/color 160ms; al confirmar el nuevo estado, traslado entre grupos 220ms con clave estable; no volver a animar toda la lista. **Transiciones:** editor entra desde abajo; tabs conservan scroll. **Estados:** primera lista vacía, todas completadas, carga, fallo inicial, fallo de toggle, datos previos con error. **Reutilizar:** modelo, cinco casos de uso y IDs. **Archivos:** `todo/presentation/screen/TodoScreen.kt`, `todo/presentation/composable/TodoItem.kt`, ViewModel para estado de operaciones.

### S05 — Crear/editar tarea y fecha

**Actual:** dialog denso, fila de tres acciones, cierre inmediato, formulario sin scroll ni IME explícito (C/V). **Objetivo:** capturar sin perder texto. **Composición:** sheet expandida con título y cerrar; nombre, descripción, fila de vencimiento; Guardar ancho completo; eliminar en menú de edición. **Componentes:** EditorSheet, TextField, DateField, ConfirmDialog, ActionButton. **Jerarquía:** contenido editable → metadato opcional → guardar; cancelar queda en cerrar/Atrás. **Interacciones:** título obligatorio; fecha opcional y eliminable; eliminar pide confirmación. DatePicker estándar, scroll del formulario y botón visible sobre teclado. **Animaciones:** sheet nativa, aparición de error 160ms y cambio de altura 180ms. **Transiciones:** fecha es modal sobre editor, vuelve a editor sin perder texto; cierre final solo tras éxito. **Estados:** nuevo, edición, inválido, guardando, fallo, confirmación de salida/borrado. **Reutilizar:** valores actuales de `dueDate` y validación trim. **Archivos:** `todo/presentation/composable/TodoDialog.kt` (wrapper durante migración), nuevo `TodoEditorSheet.kt`, ViewModel y helper de fecha de presentación.

No migrar timestamps existentes. La conversión UTC del DatePicker frente a la visualización local actual puede desplazar el día: registrar y probar este caso separadamente antes de cambiar la interpretación; no convertir registros masivamente bajo una tarea visual.

### S06 — Hábitos

**Actual:** cards casi idénticas a Tareas, barra y checkbox sin suficiente explicación del período, labels de siete días potencialmente incorrectos (C/D). **Objetivo:** registrar hoy y comprender continuidad. **Composición:** header con fecha y resumen “Marcados hoy”; cards de hábito con nombre, recurrencia y menú; acción explícita “Marcar hoy”; progreso con texto del período; siete fechas debajo. **Componentes:** HabitProgressCard, PeriodProgress, DayCell, StreakBadge, EmptyState. **Jerarquía:** nombre/acción hoy → progreso del período → historial; racha es secundaria, no un juicio sobre el usuario. **Interacciones:** hoy y cada fecha llaman el toggle existente; menú Editar; cuerpo informativo no debe robar toques al historial. **Animaciones:** check 160ms, barra 240ms, leve confirmación de escala 1→1.06→1 en check tras éxito; haptic ligero una vez. **Transiciones:** editor sheet; acceso retirado retorna Home sin borrar datos. **Estados:** daily/weekly/monthly, 0 registros, objetivo superado, racha=0, loading/error, Premium/flag. **Reutilizar:** `HabitWithStatus`, cálculo completo de racha y períodos, fechas exactas. **Archivos:** `habit/presentation/screen/HabitScreen.kt`, `habit/presentation/composable/HabitCard.kt`, ViewModel para pendientes por hábito/fecha.

Siete hitboxes de 48dp necesitan 336dp útiles. Si no caben, usar fila horizontal desplazable con señal parcial de continuidad, hoy visible inicialmente y fechas anteriores accesibles. No achicar hitboxes ni solaparlas.

### S07 — Crear/editar hábito

**Actual:** stepper pequeño, seis círculos sin nombre, cambio de monthly a weekly puede dejar objetivo fuera de rango (C). **Objetivo:** explicar la recurrencia y permitir editar con control. **Composición:** sheet; nombre; selección de frecuencia; objetivo debajo solo si no daily; colores en grilla 3×2 con nombres y check; guardar fijo. **Componentes:** EditorSheet, RecurrenceOptions, TargetStepper, ColorChoice. **Jerarquía:** qué hábito → cada cuánto → cuántas veces → color. **Interacciones:** daily=1, weekly 1–7, monthly 1–31; al cambiar tipo ajustar solo borrador al rango válido. Color persistido arbitrario se conserva como opción “Actual”. **Animaciones:** AnimatedVisibility del objetivo 180ms, selección 140ms; sin animación al escribir número manual. **Transiciones:** confirmación de descarte/borrado dentro del contexto. **Estados:** nuevo/editar/guardando/error/rango inválido. **Reutilizar:** enum, ID y createdAt existentes; el flujo actual de alta vía UpdateHabit hace upsert y se conserva hasta que exista una tarea funcional independiente. **Archivos:** `habit/presentation/composable/HabitDialog.kt`, nuevo `HabitEditorSheet.kt`, `HabitViewModel.kt`.

### S08 — Diario, lista

**Actual:** tarjeta con icono repetido, fecha ISO, swipe destructivo inmediato (C/D). **Objetivo:** reconocer entradas y volver a escribir. **Composición:** título “Diario”, explicación breve, lista editorial agrupada por mes de creación; fecha en columna estrecha y título/extracto; CTA “Escribir”. **Componentes:** JournalEntryRow, MonthHeader, EmptyState. **Jerarquía:** título → extracto máximo dos líneas → fecha; sin icono de libro en todas las filas. **Interacciones:** abrir edita; menú accesible para eliminar; swipe revela intención y abre confirmación, nunca escribe por sí solo. **Animaciones:** eliminación confirmada contrae fila 180ms; entrada nueva se incorpora 200ms. **Transiciones:** abrir editor avanza horizontalmente 240ms; no shared text que escale mientras se abre teclado. **Estados:** empty, loading, lista, error, borrado pendiente. **Reutilizar:** orden actual de `createdAt` descendente, entradas y delete use case. **Archivos:** `journal/presentation/screen/JournalScreen.kt`, `journal/presentation/composable/JournalCard.kt`, `JournalViewModel.kt`.

### S09 — Diario, nuevo/edición

**Actual:** dos cajas contorneadas, gran título de barra y check poco explícito; lectura aún cargando parece nueva; borrar sale inmediatamente (C/D). **Objetivo:** un espacio de escritura claro, con guardado explícito y seguro. **Composición:** barra compacta Atrás/“Nueva entrada” o fecha/Guardar; título editable 30sp sin caja y cuerpo 17/28sp sobre superficie plana; menú Eliminar solo al cargar entrada existente. **Componentes:** JournalEditor, EditorTopBar, InlineNotice, ConfirmDiscardDialog. **Jerarquía:** el texto escrito domina; controles secundarios discretos. **Interacciones:** título obligatorio, cuerpo libre; Guardar con texto; aviso de cambios al salir; borrador restaurable en recreación de Activity, sin prometer sincronización ni autosave remoto. **Animaciones:** loading→editor fade 160ms; botón guardando de ancho fijo; éxito navega una vez. **Transiciones:** ocultar barra principal; Atrás vuelve a la lista con scroll retenido. **Estados:** nuevo, cargando existente, listo, no encontrado, fallo de carga, guardando, fallo de guardado, eliminando. **Reutilizar:** `JournalDetail(entryId)` y use cases; capturar excepciones de carga en presentación sin cambiar repositorio. **Archivos:** `journal/presentation/screen/JournalDetailScreen.kt`, `JournalDetailViewModel.kt`, `AppNavHost.kt`, `journal/di/JournalModule.kt` si se inyecta delete en detalle.

### S10 — Pomodoro / Enfoque

**Actual:** círculo útil pero aislado; opciones 5/25/10 sin orden visual; resumen pequeño; SpaceBetween frágil ante poca altura (C/D). **Objetivo:** ser la firma visual del producto sin alterar el reloj. **Composición:** título “Enfoque”; estado explícito de modo; aro orbital 264dp ideal, 208dp compacto; minutos/segundos estables; botones manuales en orden visual 5/10/25; acción central 80dp con texto Iniciar/Pausar debajo, secundarios Reiniciar/Saltar; próxima sesión y contador en dos líneas legibles. **Componentes:** FocusDial, DurationOptions, FocusControls, SessionSummary. **Jerarquía:** tiempo → acción → modo/próximo paso. **Interacciones:** conservar duración, reset, pausa y skip; confirmar reinicio/salto/cambio de duración si descarta avance, luego invocar callback existente una vez. Selección manual cambia duración, no inventa cambio de modo. **Animaciones:** aro interpola diferencias pequeñas hasta 200ms; al restaurar app salta al valor real, sin recorrer minutos atrasados. Play/Pause crossfade 120ms; modo 220ms; halo no respira continuamente. **Transiciones:** cambiar pestaña no reinicia sesión. **Estados:** listo, ejecutando, pausado, pausa corta/larga, restaurado, finalizado, flag retirado. **Reutilizar:** ViewModel global, `PomodoroCompletionHandler`, `TimeProvider`, WorkManager y preferencias íntegros. **Archivos:** `pomodoro/presentation/PomodoroScreen.kt`, nuevos componentes en `pomodoro/presentation/composable/`.

### S11 — Finalización Pomodoro

**Actual:** overlay sólido cubre cuerpo pero deja barra externa disponible, sin semántica modal explícita (C/V). **Objetivo:** reconocer fin de sesión sin perder orientación o borradores. **Composición:** dialog de confirmación con aro cerrado/check, “Sesión finalizada”, modo completado, siguiente sesión y único botón “Continuar”. **Componentes:** CompletionDialog con slot visual orbital. **Jerarquía:** resultado → próximo paso → cerrar. **Interacciones:** Continuar/Atrás cierran con `dismissCompletionMessage`; no arrancan siguiente timer automáticamente. **Animaciones:** fade+escala .96→1 en 220ms, check se dibuja en 240ms una vez; haptic leve solo ante finalización observada en foreground. **Transiciones:** cierra al contexto anterior, incluso Diario con borrador. **Estados:** completó enfoque, completó descanso, restauración con mensaje pendiente, flag desactivado. **Reutilizar:** completedMode y mensaje persistido; mostrar próximo modo actual del estado ya avanzado, no el modo posterior calculado para otra sesión. **Archivos:** `App.kt`, extracción de `PomodoroCompletionOverlay` a archivo propio.

### S12 — Ajustes, tema, hora y permisos

**Actual:** cinco cards largas con switches exclusivos; hora editable por dígito; salir es icono en barra; falta Atrás (C/D). **Objetivo:** personalizar y controlar preferencias sin sensación de panel técnico. **Composición:** barra Atrás/Ajustes; sección Apariencia con miniaturas de tema 2 columnas (1 con fuente grande); sección Notificaciones con toggle y fila “Hora del resumen”; sección Sesión con “Cerrar sesión”. **Componentes:** ThemePreviewTile, PreferenceRow, TimePickerDialog, ConfirmDialog, InlineNotice. **Jerarquía:** opciones agrupadas, no cinco bloques iguales antes de llegar al resto. **Interacciones:** radio para tema; reloj edita borrador y confirma una vez; denegación mantiene toggle apagado y explica cómo habilitarlo desde Android; salir confirma intención. No añadir avatar/email inventados. **Animaciones:** indicador tema 160ms; aparición de hora 180ms; tema global cambia directamente para evitar frames de contraste incierto. **Transiciones:** Ajustes es secundario sin barra principal; modales devuelven foco a origen. **Estados:** cinco temas, permiso concedido/denegado, notificaciones apagadas, hora editando/confirmada. **Reutilizar:** ThemeMode, SettingsViewModel, NotificationPermissionState y scheduler. **Archivos:** `settings/presentation/screen/SettingsScreen.kt`, `ThemeCard.kt`, `PreferenceItem.kt`, callback Atrás en `AppNavHost.kt`.

## 7. Lenguaje de motion

La duración es orientación de diseño, no temporizador funcional. Los eventos de negocio nunca dependen de que termine una animación.

| Evento | Técnica y especificación | Propósito / restricción |
|---|---|---|
| Presión de botón | Ripple nativo; escala .98 solo en CTA grande, 90ms, retorno spring damping .85 / stiffness 600 | Acusar toque; no escalar inputs o texto de lista |
| Selección chip/radio | color + indicador 140–160ms | Localizar selección; mantener label/check |
| Loading→contenido | AnimatedContent, fade 180ms, tamaño estable | Evitar saltos; no animar error y empty como si fueran éxito |
| Revelar sección/ayuda | AnimatedVisibility + expansión 180ms | Explicar espacio nuevo |
| Cambiar altura de bloque | animateContentSize 180ms, solo contenedor pequeño | Nunca toda LazyColumn ni editor de texto en cada carácter |
| Completar tarea | check 160ms y ubicación 220ms | Explicar traslado de grupo tras confirmación |
| Progreso hábito | animateFloatAsState 240ms | Asociar acción con avance |
| Counter Home | AnimatedContent 220ms, sin contar desde cero al abrir | Comparar estado anterior y nuevo; TalkBack lee final |
| Tabs | fade 140ms sin desplazamiento horizontal | Destinos pares, no historial secuencial |
| Pantalla secundaria | entrada desde end 24dp + fade 240ms; retorno inverso 200ms | Profundidad; direcciones start/end |
| Sheet | animación Material existente; no segunda animación encima | Continuidad espacial; respetar drag/back |
| Dialog | fade+escala .96→1, 180–220ms | Atención contextual |
| Onboarding | pager programático 280ms; indicador 180ms | Progreso entre páginas |
| Primera entrada Home | máximo 3 bloques, stagger 35ms, total <350ms | Construir jerarquía una vez, no en cada recomposición |
| Skeleton | pulsación suave 900ms, máximo 4 placeholders visibles | Comunicar espera; detener al ocultarse o con movimiento reducido |
| Fin Pomodoro | aro/check 240ms una vez | Confirmar resultado, sin confeti |
| Error | aparición de mensaje 160ms + foco al campo inválido | Explicar recuperación; sin sacudidas ni vibración repetida |

Shared transitions: evaluadas y **no requeridas para esta renovación**. Diario cambia de tarjeta a input y teclado, lo que añade riesgo de selección y geometría. Home→Enfoque usa atajo, no representación compartida del reloj. El presupuesto visual se invierte en transiciones previsibles. Si se plantea después, aislar experimento con fallback; soporte conceptual en [documentación de shared elements](https://developer.android.com/develop/ui/compose/animation/shared-elements).

Movimiento reducido: respetar escala de animación del sistema; modo sin movimiento en política de presentación para previews/pruebas. Sin springs, stagger, pulsación, escala ni desplazamiento cuando se reduce. Mostrar directamente estados finales; no esconder feedback textual. Alto contraste desactiva efectos ornamentales independientemente de la escala del sistema.

Haptics: confirmación ligera por acción local de completar y finalización en foreground; no vibrar en ticks, scroll, cambios remotos o hidratación de datos. Si llega finalización al reabrir, mostrar dialog sin repetir vibración del sistema. No se necesita permiso de vibración ni motor personalizado para usar feedback de UI disponible; validar soporte exacto en la versión instalada.

## 8. Riesgos y decisiones difíciles

1. **Calidad de feedback necesita presentación con resultados.** Autorizar al futuro implementador cambios acotados de UiState/ViewModels para pending, error y resultado consumible. Prohibir reinterpretar éxito a partir de un `delay` o snapshot optimista.
2. **Home no puede detectar fallos con su contrato actual.** Excepción fijada: `GetDashboardDataUseCase` devuelve `Result.failure` cuando una fuente falla, sin cambiar agregación exitosa, modelos o queries. Home conserva datos anteriores con aviso o muestra resumen no disponible. Esta modificación del canal de error es necesaria para feedback veraz; se aísla y prueba. No agregar suscripciones paralelas ni un cálculo duplicado en presentación. Un fallo incluso de una fuente cuya tarjeta esté oculta puede dejar indisponible el resumen combinado: limitación explícita del contrato actual, sin ocultar navegación ni motivación.
3. **Cambiar navegación tiene riesgo de lifecycle.** Mantener VM global de Pomodoro y scopes de sesión; no duplicar inicializadores de Remote Config al extraer shell.
4. **Racha no equivale universalmente a días.** Se conserva algoritmo y se cambia lenguaje; una revisión matemática sería otra tarea.
5. **No autosave remoto ni Undo sin contrato.** Guardar explícito y confirmación destructiva reducen pérdida sin añadir persistencia nueva.
6. **Tema se serializa por nombre.** Las cinco opciones permanecen; sus nombres visibles pueden cambiar. Todos los roles nuevos deben existir en las cinco.
7. **Acceso y onboarding usan edge-to-edge fuera del shell.** Revisar insets allí, no solo en listas.
8. **Datos privados no son assets de portfolio.** Las muestras se crean como fixtures de preview/test; para capturas reales usar cuenta de pruebas acordada. No inyectar ejemplos en Firestore de una cuenta existente.

## 9. Qué constituye una renovación terminada

La implementación se acepta cuando existe identidad consistente en todas las experiencias, cada pantalla tiene composición propia, toda acción tiene feedback verificable, no se pierde un borrador por un error de red, las funciones existentes siguen operativas y las escenas se han revisado en Android real.

La comparación debe incluir Inicio con contenido, Hábitos con recurrencias mixtas, Enfoque ejecutando, Diario editando, Ajustes y un estado vacío. Evaluar también todos los temas, fuente 200%, ancho 320dp, horizontal, teclado, TalkBack, datos abundantes y feature flags. No aceptar una renovación validada únicamente con capturas de Home y sin flujos completos.

El orden de implementación, archivos, componentes, contratos, pruebas y gates está cerrado en [AURA_REDESIGN_PLAN.md](AURA_REDESIGN_PLAN.md).

## Estado tras la ejecución local

La dirección visual y los contratos descritos aquí fueron aplicados en el checkout local. La evidencia de build, 190 pruebas unitarias, lint, 11 escenas de instrumentación y 25 capturas está en [`docs/ui-redesign/qa/IMPLEMENTATION.md`](docs/ui-redesign/qa/IMPLEMENTATION.md). La revisión en dispositivo físico, TalkBack, OAuth/Firebase reales y rendimiento con datos abundantes siguen siendo gates explícitamente pendientes.
