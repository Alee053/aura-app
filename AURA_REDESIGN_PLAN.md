# Aura — Plan de renovación UI/UX para el agente implementador

Fecha: 15 de septiembre de 2026. Estado: especificación ejecutada en el checkout local y conservada como contrato de diseño/QA. La implementación no realiza commits, push, despliegues ni escrituras remotas.

Leer primero [AURA_UI_AUDIT.md](AURA_UI_AUDIT.md). Este plan fija el orden, los contratos y los criterios de aceptación; la auditoría fija identidad, tokens y composición de cada pantalla. Ante conflicto, los límites funcionales y los contratos de este plan prevalecen. No sustituir decisiones de diseño por una nueva exploración estética.

## 1. Resultado esperado

Entregar una aplicación Android con identidad “Ritmo sereno”: papel cálido, tinta violeta, Manrope, marca orbital, Inicio editorial, listas legibles, hábitos con progreso claro, Enfoque como pantalla protagonista y Diario orientado a escribir. Conservar Kotlin/Compose, Firebase, persistencia, navegación tipada y funcionalidades terminadas.

Se reconstruye presentación. No migrar framework, backend, DI, schemas, sistema de build ni arquitectura de dominio. No añadir features de productividad para hacer más llamativa la demo.

### 1.1 Archivos protegidos

Por defecto, no modificar:

- `google-services.json`, `.firebaserc`, `firebase.json`, `firestore.rules`, `firestore.indexes.json`, `functions/**`, `supabase/**`.
- Repositorios Android/iOS, modelos de dominio, mappers, casos de uso, claves de DataStore, scheduler, workers, FCM, credenciales, cliente OAuth, nonce y listener de Auth.
- `pomodoro/presentation/PomodoroViewModel.kt`, `pomodoro/domain/**`, `pomodoro/data/**` y sus contratos. Esta renovación solo consume el estado del timer.
- `settings.gradle.kts`, Gradle raíz, AGP, Kotlin, SDKs, package/applicationId, configuración de firma o Manifest funcional.
- Infraestructura de Remote Config/experimentos, criterios Free/Premium y `GetHabitsAccessibilityUseCase`.

Excepciones acotadas y previstas: estados de presentación en los ViewModels enumerados, propagación de errores en `GetDashboardDataUseCase` según C2, adaptadores visuales de permiso/movimiento, callback de estado de solicitud Google en MainActivity, bindings de DI estrictamente necesarios para presentación, recursos de marca Android y dependencias explícitas de animación/test compatibles. No extender excepciones a refactors funcionales oportunistas.

### 1.2 Base de trabajo

El checkout auditado tiene muchos cambios locales anteriores en aplicación y configuración. **No partir de HEAD descartándolos.** Antes de editar en la fase futura, registrar estado y diff. No ejecutar reset/checkout masivo, clean de datos, reinstalación que borre preferencias, ni poblar Firestore con fixtures.

El código actual declara la sincronización Loco manual. Editar las tres traducciones locales de las nuevas claves; no ejecutar `pushTranslations`/`pullTranslations` ni restablecer el hook de preBuild. Registrar claves para sincronización posterior coordinada.

## 2. Convenciones de rutas

Los alias siguientes son abreviaturas exactas, no módulos nuevos:

| Alias | Ruta desde raíz |
|---|---|
| A | `composeApp/src/commonMain/kotlin/com/programovil/aura` |
| D | `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem` |
| R | `composeApp/src/commonMain/composeResources` |
| DR | `designsystem/src/commonMain/composeResources` (crear) |
| AND | `composeApp/src/androidMain/kotlin/com/programovil/aura` |
| IOS | `composeApp/src/iosMain/kotlin/com/programovil/aura` |
| T | `composeApp/src/commonTest/kotlin/com/programovil/aura` |
| AT | `composeApp/src/androidInstrumentedTest/kotlin/com/programovil/aura` (crear si no existe) |

Todo nombre nuevo listado abajo es una decisión de destino. No crear carpetas vacías ni componentes que no se usan. Mantener wrappers antiguos hasta migrar todos sus consumidores, después eliminarlos en la tarea de pulido.

## 3. Arquitectura de presentación final

```text
App: sesión y onboarding; tema
 └─ AuthenticatedApp: NavController + VMs globales existentes + flags + lifecycle
     ├─ AuraAppShell: insets, barra/rail, snackbar y capa modal global
     └─ AppNavHost: rutas tipadas, guards y transiciones
         └─ FeatureScreen: recoge estado y conecta callbacks
             └─ FeatureContent: UI pura con datos/acciones
                 ├─ componentes de feature
                 └─ componentes D sin Koin, Firebase ni modelos de dominio
```

No mover todos los ViewModels a `App`. Mantener Todo y Pomodoro en su scope actual; los demás permanecen por entrada de navegación. No reinstanciar Remote Config ni `MotivationPhraseManager` por pestaña. Un solo host de finalización de Pomodoro.

### 3.1 Frontera de componentes

**En D:** colores, tipografía, spacing, formas, elevación, movimiento; Button/TextField/Surface/Header/EmptyState/Notice/Skeleton/EditorSheet/ConfirmDialog/Selection/Progress. Props simples, slots e iconos inyectados. Sin import del `Res` de composeApp ni modelo `Todo`/`Habit`. Textos y descripciones resueltos por el consumidor.

**En A/shared/presentation:** shell, marca y escenas de producto, navegación visual, formateo localizado de fechas, política de motion/haptic, estado efímero de operaciones y borradores. Puede conocer rutas, nunca hacer escrituras Firebase.

**En cada feature:** TaskRow, HabitProgressCard, FocusDial, JournalEditor, ThemePreviewTile; reciben sus modelos o datos de presentación y callbacks. Pantalla pública conserva inicialmente su firma para migración gradual.

## 4. Contratos que eliminan decisiones ambiguas

### C1 — Operaciones de escritura

En TodoViewModel, HabitViewModel, JournalViewModel y JournalDetailViewModel exponer estado de operación con identidad: Idle, Pending(requestId, targetId, kind), Succeeded(requestId), Failed(requestId, mensaje). Para toggles usar clave por item; en hábitos, bloquear también el botón Hoy mientras esté pendiente la misma fecha. Puede representarse con tipos propios, pero mantener estos estados y semántica.

- Una acción física origina una sola llamada al use case existente. Pending se establece antes de lanzar la coroutine. Doble tap durante Pending se ignora para ese objetivo.
- Los `Result` de los use cases determinan Succeeded/Failed. La colección reactiva no determina éxito del envío: puede emitir escritura local pendiente.
- Editor abierto hasta éxito. Error conserva exactamente el borrador; no vaciar campos, cerrar sheet ni marcar `isSaved`.
- El resultado permanece en StateFlow hasta que la UI lo consume por requestId. No depender de un evento sin replay que puede perderse al rotar. No reejecutar una escritura al recolectar un resultado.
- Capturar fallos sin tragarse cancelación de coroutines. Al cambiar cuenta, no restaurar borrador ni resultado de la sesión anterior.
- Operaciones largas: a los 10s mostrar “La operación sigue pendiente”; no prometer fallo/cancelación de una escritura Firestore. Mantener una sola solicitud; cuando resuelva, actualizar. No implementar cola offline ni botón Reintentar mientras la solicitud anterior siga en vuelo.
- Confirmación de eliminación previa; sin Undo porque no hay contrato de restauración de IDs/metadatos. Tras fallo, elemento y editor siguen accesibles.

Esta extensión es presentación alrededor de las llamadas existentes. No cambiar firma de repositorios ni lógica de los use cases.

### C2 — Carga y errores

Separar error persistente de carga y error efímero de acción. Prioridad del cuerpo: carga inicial sin datos → fallo de carga sin datos → contenido vacío confirmado → contenido. Si existen datos previos y falla actualización, conservarlos con aviso. Snackbar no consume el estado persistente de carga.

Añadir `retryLoad()` solo donde un flujo haya fallado; cancelar el job de carga anterior antes de recolección nueva. No crear listeners duplicados por recomposición ni reintentar una escritura mediante `retryLoad()`.

Home requiere trato especial: `GetDashboardDataUseCase` siempre devuelve success y convierte fuentes fallidas en ceros. Aplicar una única excepción fuera de presentación: si cualquiera de los dos Result de entrada falla, propagar `Result.failure` por el contrato existente (precedencia determinista: tareas, después hábitos). Si ambos tienen éxito, ejecutar exactamente los cálculos actuales. No cambiar firma, DashboardData, queries ni semántica de contadores. HomeViewModel conserva último dashboard confirmado y un error persistente; sin datos previos muestra guiones y “No pudimos cargar tu resumen”, con Reintentar. Al recuperar ambas fuentes, quitar aviso y mostrar datos reales. No agregar listeners paralelos, cálculos duplicados ni resultados parciales. El contrato combinado implica que una fuente oculta fallida puede indisponer el resumen: mantener atajos, motivación y Ajustes operativos, y registrar esa limitación sin rediseñar el dominio.

### C3 — Borradores

- Tarea/hábito: formulario keyed por ID o instancia de creación; estado saveable para rotación y picker. Conservar valores originales para calcular `dirty`. No reinicializar con cada emisión de lista.
- Diario: conservar en ViewModel mientras vive destino y guardar copia de campos en SavedStateHandle para recreación de Activity/proceso cuando Android restaure la tarea. No prometer recuperación tras borrar datos o una sesión nueva. Al cargar existente, no sobrescribir borrador restaurado del mismo ID.
- Sin cambios: cerrar directamente. Con cambios: “Descartar cambios” / “Seguir editando”. Atrás con teclado abierto lo cierra primero; siguiente Atrás aplica política del editor.
- Durante Pending bloquear descartar hasta resultado, con mensaje de operación pendiente, sin bloquear lectura ni selección de texto.
- No autosave remoto, nuevo esquema de borradores, Room ni DataStore adicional.

### C4 — Navegación final

Orden en teléfono: **Inicio, Tareas, Hábitos, Enfoque, Diario**. Mismas rutas; Enfoque es label de `NavRoute.Pomodoro`. Settings se abre desde icono en Inicio y lleva Atrás. No crear pestaña “Más”.

- Barra anclada al borde inferior con surface, sin cápsula flotante exterior. Ítems Material con label visible e indicador tonal; icono 24dp. Safe area del sistema añadida una sola vez.
- Si una feature no está habilitada, omitir su item; compactar en el orden anterior. Si solo queda Inicio, ocultar barra. No mostrar bloqueado/premium/compra.
- Mostrar barra en Home/Todo/Habit/Pomodoro/Journal. Ocultar en Settings y JournalDetail. El selector Google y onboarding no usan shell autenticado.
- Tab: singleTop, guardar/restaurar estado al cambiar, sin apilar copias. Atrás desde tab retorna a Home; desde Home deja al sistema manejar salida. Desde detalle/Ajustes retorna a su origen real.
- Grafo tipado estable: conservar registro de destinos y proteger entrada/contenido con guard, en vez de reconstruir todo el grafo cuando cambia un flag. Antes de mostrar una feature, comprobar su acceso usando la fuente existente.
- Si se retira acceso al destino actual o su detalle, ir a Home una vez, cerrar modal de esa feature y dar aviso breve. No borrar borrador ni datos remotos; no ejecutar Guardar automáticamente. El guard no debe bloquear cierre de una operación en vuelo ni desencadenar nuevas escrituras.
- Pomodoro global sigue sincronizando incluso fuera de pantalla. Si flag se apaga, ocultar acceso y finalización visual como actualmente; no cancelar timer por decisión visual.
- ≥600dp: rail de 80dp con mismos destinos y etiquetas, contenido centrado. Fuente grande mantiene legibilidad; no sustituir labels por iconos sin nombre.

### C5 — Insets, overlays y foco

`AuraAppShell` posee safeDrawing/top/bottom de pantallas autenticadas; contents no vuelven a añadir insets del sistema. Top bars de feature usan insets cero al estar dentro del shell. Acceso/onboarding poseen sus propios insets. Sheets y dialogs respetan IME y safe area de su ventana.

Un solo modal interactivo en cada instante. Si finaliza Pomodoro mientras hay DatePicker, ConfirmDialog o editor modal, mantener `showCompletionMessage` pendiente y mostrar al cerrar ese modal; no apilar dialogs ni descartar borrador. En Diario de pantalla completa, CompletionDialog puede aparecer conservando texto/selección. Cerrar devuelve foco al origen. El dialog global se monta por encima del shell y bloquea acceso a barra/rail.

### C6 — Fechas y recurrencias

Formatear fechas con locale en/es/fr en presentación. Labels de últimos siete días derivan de `LocalDate.dayOfWeek`, no del índice. Hoy se distingue con contorno y label; completado con check.

Vencimiento: preservar el Long actual al abrir/guardar sin cambios. Para nuevos valores mantener contrato del DatePicker existente; no reinterpretar fechas históricas silenciosamente. Mostrar fecha usando la semántica actual hasta resolver y documentar el caso de zona UTC del picker versus zona local. Si la discrepancia de día se reproduce, entregar un ticket funcional separado con ejemplo exacto y propuesta; no afirmar que esa deuda quedó resuelta por formatear. No usar etiquetas relativas “Hoy/Vencida” si la fecha del picker y la visualización no coinciden; durante esa condición mostrar solo fecha absoluta localizada.

Recurrencia: daily objetivo 1; weekly 1–7; monthly 1–31. Ajustar borrador al cambiar período para que nunca se envíe un valor fuera del rango. Mantener el cálculo de racha y los registros históricos.

### C7 — Motion y fuentes de verdad

Usar la tabla de motion de la auditoría. Defaults: interacción 140ms, estado 180ms, navegación 240ms; spring únicamente en feedback de presión/completado. La política accesible permite estados finales inmediatos. No escribir repositorios desde callbacks de fin de animación.

Pomodoro: animar el dibujo, no el tiempo de negocio. Tiempo textual exacto; sin contador que vuelva a cero, sin segundo ticker, sin interpolar saltos grandes al volver a foreground. Mensaje de finalización muestra `completedMode` y el **modo actual**, que ya es la próxima sesión. No reutilizar para ese texto una función que calcula el modo posterior al actual.

Haptic solo por acción local reconocida o cambio de finalización mientras la app ya estaba visible. No por primer valor de StateFlow, cambio remoto ni restauración. Una clave de evento evita duplicados por recomposición. Si no hay señal suficiente para distinguir restauración, omitir ese haptic; nunca adivinar.

## 5. Secuencia y gates

```text
P00 baseline
 → P01 tokens → P02 assets → P03 controles → P04 superficies/estados → P05 contratos
 → P06 shell/nav
 → P07 Inicio → P08 Tareas → P09 editor tarea → P10 Hábitos → P11 editor hábito
 → P12 Enfoque → P13 finalización → P14 Diario → P15 editor Diario
 → P16 Ajustes → P17 Acceso → P18 Onboarding
 → P19 motion → P20 estados integrados → P21 adaptación/accesibilidad
 → P22 pulido → P23 QA final
```

Cada tarea debe dejar el proyecto compilable, sin placeholder interactivo. No abrir diez pantallas a medias para revisarlas juntas al final. Animaciones locales definidas desde su componente; P19 coordina y ajusta, no inventa un lenguaje nuevo.

### P00 — Congelar evidencia de la base funcional

- **Objetivo:** comparar renovación contra el checkout que hoy funciona.
- **Archivos:** leer Git, Gradle, A, AND, T; crear solo evidencia futura bajo `docs/ui-redesign/qa/` cuando se autorice implementar. Ningún componente nuevo.
- **Comportamiento/visual/motion:** capturas originales de todas las S00–S12, cinco temas y vídeo breve de navegación/Pomodoro; no cambiar UI.
- **Dependencias:** ninguna.
- **No modificar:** aplicación, datos reales, configuración remota ni preferencias del propietario.
- **Riesgo:** confundir cambios previos con los propios o atribuir bugs anteriores al rediseño. Registrar hash/estado del checkout, dispositivo, SO, tamaño, tema, versión y fecha de captura.
- **Comprobación:** ejecutar build/unit tests cuando empiece implementación; documentar resultado real, no copiar cifra del README. Si hay dispositivo de pruebas, recorrer CRUD y sesión con datos acordados.
- **Aceptación:** baseline identificado y problemas anteriores separados; ausencia de dispositivo queda registrada y bloquea solo QA visual final, no trabajo de código posterior autorizado.

### P01 — Foundation y tokens completos

- **Objetivo:** un vocabulario visual único con cinco temas compatibles.
- **Modificar:** D/theme/`Color.kt`, `Type.kt`, `DsTheme.kt`.
- **Crear:** D/theme/`Spacing.kt`, `Shape.kt`, `Elevation.kt`, `Motion.kt`.
- **Decisiones:** aplicar tablas de auditoría; ampliar roles de MaterialTheme incluidos surfaceContainer, onSurfaceVariant, outlineVariant, inverse y scrim para no dejar colores Material por defecto. Mantener enums PURPLE/RED/GREEN/DARK/HIGH_CONTRAST y propiedades antiguas como aliases temporales.
- **Comportamiento/motion:** sin animación global de paleta. Alto contraste suprime ornamentación. No hardcodes de spacing/color fuera de tokens o recursos de marca.
- **Dependencias:** ninguna nueva.
- **No modificar:** ThemeRepositoryImpl, clave `theme_mode`, preferencia por defecto PURPLE.
- **Riesgo:** componentes Material con roles sin mapear; temas guardados inválidos si se renombra enum.
- **Comprobación/aceptación:** preview estática con texto, input, radio, error y botón en los cinco temas; todos los nombres guardados siguen resolviendo y no queda control con color ajeno al tema.

### P02 — Tipografía e identidad empaquetada

- **Objetivo:** identidad reconocible también en arranque y launcher.
- **Crear:** DR/font/`manrope_regular.ttf`, `manrope_medium.ttf`, `manrope_semibold.ttf`, `manrope_bold.ttf`; DR/files/`manrope_OFL.txt`; A/shared/presentation/composable/`AuraBrandMark.kt`, `AuraScene.kt`.
- **Modificar:** D/theme/Type.kt para recursos; launcher `composeApp/src/androidMain/res/drawable-v24/ic_launcher_foreground.xml`, `drawable/ic_launcher_background.xml`, `mipmap-*/ic_launcher*.png` con misma identidad. Mantener referencias/nombres existentes. No cambiar icono monocromo de notificación por un PNG de marca.
- **Decisiones:** Manrope 400/500/600/700; instancias estáticas derivadas del archivo oficial, conservar licencia y origen. Aro de geometría definida en auditoría; cuatro escenas sin texto rasterizado, una por slide. En launcher respetar zonas seguras adaptive/legacy. No se exige actualizar el host iOS para dar por terminado Android.
- **Comportamiento/motion:** escenas quietas por defecto y sin llamadas de red; parámetros de progreso solo cuando los consume UI.
- **Dependencias:** ninguna de runtime; herramienta de preparación de fuente solo si necesaria en desarrollo. Fallback SansSerif durante preparación, no como entrega final.
- **No modificar:** applicationId, Manifest funcional, servicios ni drawables de notificación.
- **Riesgo:** pesos sintéticos, glifos faltantes o assets que cambian de forma entre resoluciones.
- **Comprobación/aceptación:** verificar á/ñ/ç/œ/apóstrofos, pesos reales, números y launcher; funcionamiento offline sin descargar fuente. Origen/licencia anotados.

### P03 — Controles compartidos

- **Objetivo:** acciones y campos consistentes antes de rediseñar pantallas.
- **Modificar:** D/components/button/`PrimaryButton.kt`, input/`BasicInput.kt`, divider/`HorizontalDivider.kt` como wrappers compatibles.
- **Crear:** D/components/button/`AuraButton.kt`, input/`AuraTextField.kt`, selection/`AuraSelectionRow.kt`.
- **Decisiones:** botón 56dp mínimo, radios16/12; variantes primaria/tonal/texto/destructiva; campo label/ayuda/error/contador opcional e IME. Un solo componente multilineal; no repetir OutlinedTextFieldDefaults.
- **Comportamiento/motion:** enabled/loading/error/pressed; tamaño estable al cargar, bloqueo de doble envío, ripple; selección 140ms. IconButton 48dp. Formulario no valida por color solamente.
- **Dependencias:** ninguna nueva; si requiere animación explícita, agregar alias `compose-animation` alineado con `composeMultiplatform` y dependencia en designsystem.
- **No modificar:** validación de negocio ni contratos de datos.
- **Riesgo:** wrapper cambia firma rompiendo pantallas aún no migradas.
- **Comprobación/aceptación:** todas las llamadas antiguas compilan; controles funcionan con teclado/TalkBack y fuente grande; error tiene texto y botón loading no se puede enviar dos veces.

### P04 — Estructuras, estados y modales compartidos

- **Objetivo:** eliminar duplicación de empty/card/snackbar/sheet.
- **Crear:** D/components/surface/`AuraSurface.kt`, header/`AuraScreenHeader.kt`, state/`AuraEmptyState.kt`, `AuraInlineNotice.kt`, `AuraSkeleton.kt`, overlay/`AuraEditorSheet.kt`, `AuraConfirmDialog.kt`, progress/`AuraProgress.kt`.
- **Modificar:** ningún consumidor masivamente todavía.
- **Decisiones:** empty sin tarjeta envolvente, escena112dp, título22sp, ayuda16sp, una CTA. Sheet expandida, contenido scrollable, footer sobre IME, maxWidth560dp; dialog maxWidth480dp. Error de carga ocupa cuerpo con reintento; sin ilustración de éxito.
- **Comportamiento/motion:** contratos C2/C3/C5; abrir modal toma foco y cerrar lo restituye; estado→contenido fade180ms; skeleton se detiene fuera de composición.
- **Dependencias:** Material3 existente; aislar OptIn experimental aquí.
- **No modificar:** lógica de acceso/persistencia.
- **Riesgo:** sheet permite swipe-dismiss durante Pending o scrim deja botones de fondo accesibles.
- **Comprobación/aceptación:** empty, error, sheet larga y confirmación en preview/test; tecla Atrás y tap fuera siguen política de dirty/pending, fondo no interactivo.

### P05 — Estados de presentación y operaciones observables

- **Objetivo:** permitir feedback real antes de conectar nuevos editores.
- **Modificar:** A/todo/presentation/viewmodel/`TodoViewModel.kt`; habit equivalente; journal ambos ViewModels; A/home/presentation/viewmodel/`HomeViewModel.kt`; A/home/domain/usecase/`GetDashboardDataUseCase.kt` solo canal de error; A/journal/di/`JournalModule.kt` al inyectar delete en detalle.
- **Crear:** A/shared/presentation/`UiOperationState.kt`.
- **Decisiones:** C1 y C2 exactos. Home conserva la suscripción existente y propaga errores sin cambiar cálculos. Diario detalle posee su propio delete use case, evitando un segundo JournalViewModel para borrar y navegar anticipadamente.
- **Comportamiento/motion:** estados de guardado/carga/error separados; ninguna animación en VM. `isSaved` compatible durante migración, solo true tras éxito. Capturar excepción de getEntry y diferenciar null de fallo.
- **Dependencias:** ninguna de runtime; SavedStateHandle de lifecycle existente para P15, confirmar resolución y agregar artefacto correspondiente alineado solo si falta.
- **No modificar:** repositorios y algoritmos exitosos de los use cases, especialmente alta de hábito por UpdateHabit y timer. Única excepción: canal de error del dashboard descrito en C2.
- **Riesgo:** eventos duplicados, listeners huérfanos, reintento de escritura pendiente.
- **Comprobación/aceptación:** extender tests T por Pending→Success/Failure, doble envío, error de carga, consumidor recreado y null/exception en Diario. Home: tarea falla, hábito falla, ambos fallan, recuperación y conservación del último dato. Actualizar únicamente expectativas anteriores que codifiquen fallos como ceros en `GetDashboardDataUseCaseTest`; conservar sin cambios todas las expectativas de cálculo exitoso. Ninguna suscripción nueva para resolver errores.

### P06 — Shell y navegación

- **Objetivo:** orientación consistente y vida estable de las features.
- **Modificar:** A/`App.kt`, navigation/`AppNavHost.kt`; `NavRoute.kt` solo si hace falta metadata, conservando tipos/serialización.
- **Crear:** A/navigation/`AuraDestinations.kt`; A/shared/presentation/composable/`AuraAppShell.kt`, `AuraNavigation.kt`.
- **Decisiones:** aplicar C4/C5; extraer navegación repetida a una función de presentación, lista de destinos filtrada con fuente de acceso existente. No cambiar orden según frecuencia de uso ni centrar Home dinámicamente.
- **Comportamiento/motion:** tab fade140ms; secundarios240/200ms. Mantener scroll/estado, Atrás, guards y única finalización global. Rail a600dp; teléfono con barra anclada.
- **Dependencias:** Navigation Compose existente; sin Navigation3 ni Accompanist.
- **No modificar:** instanciación/sincronización de Pomodoro, auth, flags managers y eventos de notificación.
- **Riesgo:** reemplazar NavHost elimina VM o navegación a ruta oculta rompe backstack.
- **Comprobación/aceptación:** tests UI de tab→tab→Atrás, diario→detalle→Atrás, Ajustes→Atrás, cambio de flags y doble tap. Timer no reinicia ni crea segundo ticker; barra ausente en editor/ajustes y sin insets duplicados.

### P07 — Inicio editorial

- **Objetivo:** reemplazar dashboard de cards uniformes por S03.
- **Modificar:** A/home/presentation/screen/`HomeScreen.kt`, composable/`DashboardCard.kt`, AppNavHost callbacks.
- **Crear:** en home/presentation/composable `TodayHeader.kt`, `PendingTasksHero.kt`, `HabitSummary.kt`, `FocusShortcut.kt`, `MotivationNote.kt`.
- **Decisiones:** hero tinta, número40sp, título exacto “Tareas pendientes”; Hábitos marcado/total y racha secundaria; atajo Enfoque abre destino sin iniciar; motivación estática. Aplicar prioridades cuando faltan features de S03.
- **Comportamiento/motion:** C2; sin porcentaje para0/0; contador220ms solo actualizaciones; entrada inicial acotada. Sin avatar, score o gráfica.
- **Dependencias:** ninguna.
- **No modificar:** DashboardData, variantes o entitlement.
- **Riesgo:** mostrar KPI de fuente fallida como cero o anunciar “días” incorrectos.
- **Comprobación/aceptación:** fixtures de cero/muchos datos, Free/Premium y todas las combinaciones de acceso; captura de hero y bloques diferenciados; motivación no expone click. No huérfanos visuales si solo queda Ajustes.

### P08 — Lista de tareas

- **Objetivo:** S04, lectura rápida y confirmación localizada.
- **Modificar:** A/todo/presentation/screen/`TodoScreen.kt`, composable/`TodoItem.kt`.
- **Crear:** en composable `TaskRow.kt`, `DueDateLabel.kt`, `CompletedTasksSection.kt`; A/shared/presentation/`LocalizedDateFormatter.kt`.
- **Decisiones:** filas sobre surface, separador, título16sp/24, descripción14sp/20 y fecha14sp; pendientes primero, completadas plegables abiertas por defecto. CTA “Nueva tarea” solo con contenido; empty usa su única CTA.
- **Comportamiento/motion:** checkbox distinto de editar; pending por ID; después de confirmar mover fila, no antes; animateItem con claves. C6 para fechas.
- **Dependencias:** ninguna.
- **No modificar:** orden persistido, modelo, filtros remotos ni fecha almacenada.
- **Riesgo:** pérdida de foco al mover entre grupos; checkbox propaga toque a editor.
- **Comprobación/aceptación:** 0/1/100 tareas, títulos largos, completed-only y toggle fallido; callback único, scroll conservado, fila actualizable sin reiniciar lista. Tras fallo no aparece éxito.

### P09 — Editor de tarea

- **Objetivo:** reemplazar dialog por S05 sin pérdida de borrador.
- **Modificar:** A/todo/presentation/composable/`TodoDialog.kt`, TodoScreen conexión.
- **Crear:** composable/`TodoEditorSheet.kt`.
- **Decisiones:** nombre, descripción, vencimiento, Guardar full-width, menú de eliminar; DatePicker estándar tematizado, limpiar fecha con48dp. Confirmaciones compartidas.
- **Comportamiento/motion:** C1/C3/C6; sheet nativa, errores160ms. onDismiss no debe ejecutarse al pulsar Guardar, sino al consumir éxito.
- **Dependencias:** ninguna.
- **No modificar:** formato de timestamps ni trimming del caso de uso.
- **Riesgo:** UTC/local, cierre por swipe, callback antiguo que aún cierra automáticamente.
- **Comprobación/aceptación:** alta/edición/eliminar/quitar fecha, error de red, doble save, rotación y teclado. Comparar día seleccionado versus presentado en La Paz y UTC; documentar deuda de fecha sin migrarla. Borrador sobrevive fallo y picker.

### P10 — Lista de hábitos

- **Objetivo:** S06, continuidad visible y acción Hoy entendible.
- **Modificar:** A/habit/presentation/screen/`HabitScreen.kt`, composable/`HabitCard.kt`.
- **Crear:** composable/`HabitProgressCard.kt`, `HabitDayCell.kt`, `StreakBadge.kt`.
- **Decisiones:** header/resumen, nombre+menú, texto de período, barra, grilla; color guardado solo como acento, controles usan colores semánticos contrastados. Etiquetas de fecha calculadas realmente. Racha se titula “Registros en racha”.
- **Comportamiento/motion:** progreso240ms y check160ms; pending de misma fecha compartido entre Hoy/grilla. Siete slots48dp; fila con scroll si ancho útil<336dp, hoy visible inicialmente.
- **Dependencias:** ninguna.
- **No modificar:** accesibilidad Premium, cálculo de períodos, racha o fechas de completación.
- **Riesgo:** doble toggle desde controles duplicados; rótulos incorrectos; targets solapados.
- **Comprobación/aceptación:** fixture que termine martes y otro domingo; daily/weekly/monthly, progress>target, racha0 y múltiples colores. Cada fecha pronunciada por TalkBack y enviada al callback coincide con el dato.

### P11 — Editor de hábito

- **Objetivo:** S07 y controles accesibles para todos los campos actuales.
- **Modificar:** A/habit/presentation/composable/`HabitDialog.kt`, HabitScreen callbacks.
- **Crear:** composable/`HabitEditorSheet.kt`, `RecurrenceOptions.kt`, `TargetStepper.kt`, `HabitColorChoice.kt`.
- **Decisiones:** frecuencia en tres filas radio cuando no quepa horizontal; stepper48dp y entrada numérica; color3×2: Coral, Menta, Azul, Violeta, Ámbar, Malva. Hex existentes conservados. Color legado no listado añade “Actual”.
- **Comportamiento/motion:** objetivo aparece180ms; clamp del borrador al cambiar recurrencia; C1/C3. Retener id/createdAt al editar y flujo actual de alta.
- **Dependencias:** ninguna.
- **No modificar:** use cases, generación/identidad de hábitos ni colecciones.
- **Riesgo:** modificar color sin intención al editar; borrar historial junto al hábito sin contrato.
- **Comprobación/aceptación:** monthly31→weekly7→daily1; vacío inviable, dos saves una escritura; falla conserva valores; selección TalkBack identifica nombre y estado. Eliminar no añade cascadas nuevas.

### P12 — Enfoque como pantalla principal de marca

- **Objetivo:** S10 con reloj exacto y layout adaptable.
- **Modificar:** A/pomodoro/presentation/`PomodoroScreen.kt`.
- **Crear:** presentation/composable/`FocusDial.kt`, `FocusControls.kt`, `DurationOptions.kt`, `SessionSummary.kt`.
- **Decisiones:** aro264dp, 208dp compacto; stroke8dp redondeado, halo estático solo fondo; tiempo64/72sp; opciones visuales5/10/25 conservando valores originales; botones secundarios48dp, principal80dp; labels14sp legibles.
- **Comportamiento/motion:** C7; confirmación solo al descartar avance (isRunning o restante<inicial) antes de reset/skip/cambio de duración. Cancelar no invoca VM. Duración no simula modo; pausa larga automática sigue15min aunque no exista opción manual15.
- **Dependencias:** Canvas/animación Compose existente.
- **No modificar:** PomodoroViewModel, Defaults, Handler, repositorio, scheduler, conteo de sesiones o skip que cuenta sesión.
- **Riesgo:** animación temporal se convierte en segundo reloj; labels de siguiente sesión erróneos; texto recortado.
- **Comprobación/aceptación:** estados25/5/15min, pausa y restauración; vídeo de segundo a segundo sin drift textual. Volver tras background actualiza inmediatamente. En horizontal corto aparece layout lateral según P21.

### P13 — Finalización global

- **Objetivo:** S11 y coherencia entre pantalla, notificación y retorno.
- **Modificar:** A/App.kt; extraer implementación desde PomodoroScreen.kt.
- **Crear:** A/pomodoro/presentation/composable/`PomodoroCompletionDialog.kt`.
- **Decisiones:** dialog pequeño en vez de superficie que tapa todo; un CTA Continuar que solo descarta mensaje. Usar modo actual como próximo y completedMode como anterior.
- **Comportamiento/motion:** C5/C7; animación única240ms; modal bloquea barra/rail. Si otro modal está activo, esperar su cierre sin consumir flag persistido. No reejecutar finalización.
- **Dependencias:** ninguna.
- **No modificar:** handler, flags persistidos, intent/action Android ni workers.
- **Riesgo:** doble overlay o haptic al restaurar; siguiente etiqueta salta dos sesiones.
- **Comprobación/aceptación:** terminar estando en Tareas, editor Diario, sheet y background; continuar conserva contexto/borrador. Cuarta sesión anuncia pausa larga; final de descanso anuncia enfoque. Mensaje visible una vez por finalización no descartada.

### P14 — Diario editorial

- **Objetivo:** S08 sin borrado accidental.
- **Modificar:** A/journal/presentation/screen/`JournalScreen.kt`, composable/`JournalCard.kt`.
- **Crear:** composable/`JournalEntryRow.kt`, `JournalMonthHeader.kt`.
- **Decisiones:** agrupar visualmente por mes sin alterar orden; fecha a izquierda, título y extracto a derecha; sin icono repetido. Menú Eliminar accesible; swipe muestra fondo/acción y solicita confirmación.
- **Comportamiento/motion:** swipe cancelado retorna; confirmar llama delete una vez, fallo conserva fila; salida180ms solo tras éxito. Empty con CTA Escribir.
- **Dependencias:** SwipeToDismissBox existente, sin librería nueva.
- **No modificar:** timestamps, orden del repositorio ni IDs.
- **Riesgo:** confirmValueChange ejecuta borrado durante gesto; estado dismiss queda ocultando item al fallar.
- **Comprobación/aceptación:** nuevo/abrir/borrar/cancelar/error, varios meses, título largo y contenido vacío; nunca se elimina solo por arrastrar; scroll vuelve a misma entrada.

### P15 — Editor de Diario

- **Objetivo:** S09, edición segura de pantalla completa.
- **Modificar:** A/journal/presentation/screen/`JournalDetailScreen.kt`, viewmodel/`JournalDetailViewModel.kt`, AppNavHost wiring de delete; T/journal/presentation/viewmodel/`JournalDetailViewModelTest.kt`.
- **Crear:** composable/`JournalEditor.kt`, `JournalEditorTopBar.kt`.
- **Decisiones:** título editorial editable, cuerpo plano17/28sp; Guardar textual siempre localizable; menú de eliminar solo para entry existente cargada. Sin borde de formulario alrededor de todo el escrito.
- **Comportamiento/motion:** C1/C2/C3; cargar existente deshabilita guardar y no parece alta; null→“Entrada no disponible” y Volver; excepción→error/reintentar. `onDelete` ya no crea otro VM y hace pop inmediato; navegación solo consume éxito. Loading→contenido160ms.
- **Dependencias:** SavedStateHandle si no está accesible mediante lifecycle actual, sin nueva versión.
- **No modificar:** contratos get/add/update/delete ni autoguardado remoto.
- **Riesgo:** carga tardía sobrescribe borrador, dobles entradas, Atrás pierde texto.
- **Comprobación/aceptación:** pruebas de guardado doble, fallo, ID desconocido, excepción, draft restaurado y eliminación confirmada; texto largo con teclado, selección/copy/paste nativos. Nunca una entrada nueva por error de cargar ID existente.

### P16 — Ajustes y preferencias

- **Objetivo:** S12, selección clara y control del resumen diario.
- **Modificar:** A/settings/presentation/screen/`SettingsScreen.kt`, composable/`ThemeCard.kt`, `PreferenceItem.kt`; AppNavHost callback Atrás.
- **Crear:** composable/`ThemePreviewTile.kt`, `ReminderTimeDialog.kt`.
- **Decisiones:** miniaturas de cinco temas con radio, dos columnas; grupos Apariencia/Notificaciones/Sesión; salir como fila etiquetada con confirmación. Hora mediante Material TimePicker/TimeInput (dialog con ambos modos), borrador confirmado de una vez.
- **Comportamiento/motion:** onConfirm llama una vez `setNotificationTime(hour,minute)`; cancelar no persiste. Toggle false al denegar permiso; mensaje de instrucciones Android sin enlace falso. Respetar preference y permiso por separado; revalidar permiso al foreground en adaptador si hace falta, no reprogramar desde composable. Detalle hora180ms, selección160ms, tema global inmediato.
- **Dependencias:** Material3 existente; no calendario ni librería de permisos.
- **No modificar:** ThemeMode, DataStore, scheduler, política A/B del resumen. El número de llamadas internas a DataStore permanece a cargo de SettingsViewModel.
- **Riesgo:** cambiar tema reinicia pantalla; radio anunciado como switch; resumir permiso como granted por preferencia local.
- **Comprobación/aceptación:** cinco temas sobreviven reinicio; cancelar hora no cambia nada; aceptar23:59 guarda ambos; permiso denegado no muestra estado habilitado efectivo; Atrás y logout conservan autenticación esperada.

### P17 — Acceso y gate de sesión

- **Objetivo:** S00/S01 y continuidad de autenticación.
- **Modificar:** A/App.kt, auth/presentation/screen/`SignInScreen.kt`; AND/`MainActivity.kt` únicamente estado UI de solicitud en vuelo.
- **Crear:** A/shared/presentation/composable/`AuraSessionGate.kt`; auth/presentation/composable/`GoogleSignInButton.kt`, `SignInIntro.kt`.
- **Decisiones:** composición y copy S01; botón Google con recurso oficial y estilo de proveedor; error técnico mapeado a mensaje comprensible en presentación. Arranque tiene marca, autenticación conserva pantalla.
- **Comportamiento/motion:** callback Android informa inicio/fin en `try/finally`, incluidas cancelaciones y fallback; bloquear doble request antes de obtener token. AuthViewModel conserva autoridad sobre sesión; no reemplazar listener ni `signInInFlight`. Loading tras intento local mantiene login; Loading inicial muestra gate. Entrada240ms y error160ms.
- **Dependencias:** ninguna; no SDK Google distinto.
- **No modificar:** nonce/OAuth, credential options, fallback autorizado/no autorizado, authService, token ni signOut.
- **Riesgo:** spinner queda activo al cancelar o signedIn queda tapado por estado local; exposición de detalles de excepción.
- **Comprobación/aceptación:** pruebas manuales cuenta existente/nueva, cancelar en ambas etapas, error recuperable y doble toque. SignedIn siempre gana a loading local; reabrir sesión persistida no obliga a login.

### P18 — Onboarding

- **Objetivo:** S02 con arte propio y mensajes veraces.
- **Modificar:** A/onboarding/presentation/screen/`OnboardingScreen.kt`, R/files/`onboarding_config.json`; tres archivos strings.
- **Crear:** composable/`OnboardingPage.kt`, `OnboardingProgress.kt` bajo onboarding/presentation.
- **Decisiones:** conservar4 IDs y orden; escena correspondiente a cada ID; copy indicado en auditoría, no priorización/swipe de tareas ni resumen de diario. No hardcodear nuevo JSON en Kotlin.
- **Comportamiento/motion:** pager sigue programático con fuente única VM; avance280ms, indicador180ms. Omitir solo sesión, Empezar persiste; error mantiene bypass. Página scrollable y footer estable.
- **Dependencias:** ninguna; `image_url` no dispara descargas.
- **No modificar:** repositorio, mapper, resolución de locale ni preferencia.
- **Riesgo:** onStart visual escribe preferencia dos veces o footer cambia de altura por idioma.
- **Comprobación/aceptación:** en/es/fr, volver y avanzar, omitir/reabrir, empezar/reabrir, estado vacío/error. Todas las CTAs visibles a200% de fuente.

### P19 — Integración de motion y haptics

- **Objetivo:** coordinar movimientos ya especificados y eliminar ruido.
- **Crear:** A/shared/presentation/`AuraMotionPolicy.kt`, `AuraHaptics.kt`; adaptadores AND/IOS/shared/presentation si requieren información del sistema no disponible en commonMain.
- **Modificar:** D/theme/Motion.kt y consumidores P03–P18 estrictamente para aplicar tabla de motion.
- **Decisiones:** sin shared transitions en esta entrega. Stagger solo primera composición Home y máximo3 bloques; no entradas de lista al scroll. Error sin shake; timer sin respiración continua.
- **Comportamiento:** respetar animator duration scale y su cambio; fallback sin animaciones en tests, alto contraste sin adornos. Haptics C7 con preferencia del sistema y sin pedir nuevos permisos.
- **Dependencias:** artefacto `org.jetbrains.compose.animation:animation` alineado si falta en consumidores; no Lottie/Rive/Vibrator personalizado.
- **No modificar:** repositorios ni callbacks de negocio; nunca diferir escritura para esperar motion.
- **Riesgo:** feedback duplicado por StateFlow o loops en background.
- **Comprobación/aceptación:** grabar éxito/error/tabs/sheet/timer; animaciones a0x ejecutan todas las acciones; no hay vibración al restaurar lista o navegar; infinite animations cesan fuera de pantalla.

### P20 — Integración de empty/loading/error/success

- **Objetivo:** cubrir estados completos sin confundir ausencia con fallo.
- **Modificar:** content de S00–S12 y notices compartidos; recursos en/es/fr.
- **Crear:** fixtures de preview/test bajo T o source set de debug, nunca seeds de producción; archivo de matriz `docs/ui-redesign/qa/states.md` en implementación futura.
- **Decisiones:** skeleton de filas adaptado a cada pantalla, no spinner global para toda carga; home conserva geometría; empty describe acción concreta; no CTA duplicada en FAB. Snackbar encima de barra/IME y nunca única evidencia de fallo de carga.
- **Comportamiento/motion:** C1/C2; skeleton aparece tras150ms, sin retención mínima; error persistente hasta reintento exitoso. Éxito se anuncia una vez tras Result, no al cerrar sheet.
- **Dependencias:** ninguna.
- **No modificar:** conectividad o repositorios para inventar estados offline/sync. Si no hay metadata de sincronización, no mostrar “Todo sincronizado”.
- **Riesgo:** fixtures alcanzan release o error desaparece cuando snackbar se consume.
- **Comprobación/aceptación:** cada fila de matriz §7 tiene captura o prueba; un resultado de carga fallido jamás muestra “Crea tu primera…” como si la cuenta estuviera vacía.

### P21 — Adaptación, localización y accesibilidad

- **Objetivo:** terminar layouts Android utilizables fuera de la captura ideal.
- **Modificar:** shell y componentes con ancho/altura fija; R/values*/strings.xml; adaptadores de fecha; semántica de DayCell/ColorChoice/ThemePreview/FocusDial.
- **Crear:** AT/`AuraAccessibilityTest.kt`, `AuraLayoutTest.kt` con fixtures.
- **Decisiones:** §6 de este plan; targets48dp, labels localizados, selección y errores no solo color. Timer agrupa modo/tiempo/estado; no live-region con lectura cada segundo. Hoja/dialog adquiere foco y lo devuelve. Fuentes200% sin reducción artificial.
- **Comportamiento/motion:** policy reducida verificada; teclado no tapa Guardar; scroll y focus accesibles. Semántica no duplica nombre de navegación entre icono y texto.
- **Dependencias:** pruebas Compose ya declaradas; añadir solo runner/manifest de test faltante de versión compatible, si Gradle lo requiere.
- **No modificar:** fontScale del usuario, sistema de idioma, orientación bloqueada o minSdk para ocultar problemas.
- **Riesgo:** hitboxes invisibles se solapan; pruebas solo miden screenshots y omiten TalkBack.
- **Comprobación/aceptación:** matriz §6 completa; medir contraste real y guardar pares/ratios; recorrido TalkBack sin callejón sin salida, fecha/hábito/color anunciados correctamente. Pendiente dispositivo real implica tarea no cerrada.

### P22 — Pulido y eliminación de compatibilidad temporal

- **Objetivo:** que el producto final no combine UI antigua con nueva.
- **Modificar:** wrappers antiguos sin consumidores; imports; recursos obsoletos referenciados solo por UI retirada.
- **Crear:** inventario final `docs/ui-redesign/qa/polish.md` en futura implementación.
- **Decisiones:** retirar DashboardCard genérica si ya no se usa; retirar wrappers TodoDialog/HabitDialog cuando todas las llamadas usen sheet; unificar radios/espaciado/iconografía con tokens. No borrar recursos de launcher/notificación compartidos sin resolver referencias.
- **Comportamiento/motion:** repasar focus, presión, loading, transiciones interrumpidas y back. Sin nuevos efectos estéticos.
- **Dependencias:** ninguna nueva; no cleanup masivo de dependencias ajenas como Room.
- **No modificar:** módulos no implicados ni todo el formato del repositorio.
- **Riesgo:** barrido de recursos elimina una traducción usada por otra feature.
- **Comprobación/aceptación:** búsqueda de hardcodes visuales y composables antiguos, referencias de recursos válidas, build/lint y capturas comparadas. Toda diferencia tiene motivo de diseño.

### P23 — QA funcional y visual final

- **Objetivo:** demostrar renovación completa y ausencia de regresiones atribuibles.
- **Archivos:** evidencia en `docs/ui-redesign/qa/`; cambios correctivos solo dentro del alcance anterior, regresando a la tarea afectada. Ningún componente nuevo planificado.
- **Decisiones/comportamiento/motion:** ejecutar §§7–9; revisar a tamaño real, no solo zoom de screenshot.
- **Dependencias:** ninguna de producto nueva.
- **No modificar:** datos del propietario, flags en producción, reglas Firebase, despliegues ni configuración de release.
- **Riesgo:** declarar finalización con unit tests pero sin Android real o con documentación desactualizada respecto al APK.
- **Comprobación/aceptación:** todos los gates obligatorios pasan, bugs previos diferenciados y limitaciones declaradas; entregar APK generado, capturas/video y reporte de pruebas si esa fase autoriza generar/entregar build. No commit/push automático por terminar tareas.

## 6. Reglas de layout que el implementador no debe decidir de nuevo

| Condición | Comportamiento exigido |
|---|---|
| Ancho320–359dp | margen16dp; una columna; textos envuelven; botones apilados si falta ancho |
| Ancho360–599dp | margen24dp; una columna principal; barra inferior, cinco items como máximo |
| Ancho≥600dp | rail80dp; contenido maxWidth960dp, centrado; formularios560dp y Diario640dp |
| Enfoque con altura útil<600dp y ancho<600dp | aro208dp y contenido scrollable, controles nunca fuera de acceso |
| Enfoque horizontal con ancho≥600dp y altura útil<600dp | aro a izquierda, opciones/controles/resumen a derecha, ambas zonas scrollables si fuente grande |
| Fuente≥1.5× | selección de recurrencia en filas; temas en una columna; abandonar alturas fijas de texto |
| Grilla hábito ancho útil<336dp | DayCells48dp en fila horizontal desplazable, hoy inicialmente visible; indicación de “últimos7días” |
| IME abierto | editor no mueve header fuera de alcance; footer encima del teclado o alcanzable por scroll; sin doble imePadding |
| Alto contraste | fondo opaco, bordes claros, check y label, sin halo/skeleton animado/gradientes decorativos |
| Locale en/es/fr | mismo layout, pluralización y fechas adecuadas; formato de hora respeta preferencia12/24h sin cambiar valor24h persistido |

No diseñar tablet master/detail con dos ViewModels independientes de la misma entrada: rail y ancho controlado son suficientes en esta renovación.

## 7. Matriz mínima de estados

Cada celda enumerada debe tener fixture o reproducción registrada. No todas requieren un test nuevo; sí evidencia de revisión.

| Experiencia | Estados obligatorios |
|---|---|
| Sesión/acceso | arranque, sin sesión, selector abierto, cancelación, autenticando, error, sesión persistida |
| Onboarding |4páginas, loading, omitido, completado, error/bypass, en/es/fr |
| Inicio | loading,0pendientes, muchas, hábitos0/0, hábitos con avance, fuente fallida, Free/Premium, atajos ocultos |
| Tareas | vacío confirmado, pendientes, solo completadas, mezcla, carga/error, toggle pending/error |
| Editor tarea | nuevo, editar, sin título, con/sin fecha, picker, dirty, guardando, falla, borrar/cancelar |
| Hábitos | daily/weekly/monthly,0y>0racha, meta superada, colores heredados, falla toggle, acceso retirado |
| Editor hábito | frecuencia/rangos, color actual heredado, dirty, pending/error, borrar |
| Diario | vacío, meses múltiples, error, swipe cancelado, confirmar/fallar borrado |
| Editor Diario | nuevo, existente cargando, listo, IDausente, excepción, borrador restaurado, guardar/borrar pending/error |
| Enfoque | idle, running, paused, duraciones manuales, descanso5/15min, reset/skip cancelado/confirmado, restore |
| Finalización | enfoque1y4, descanso, foreground, reabrir, otro modal activo, flag apagado |
| Ajustes |5temas, permiso granted/denied, toggle off/on, hora editada/cancelada, salir/cancelar |

## 8. Validación funcional de no regresión

En una cuenta/dispositivo de pruebas acordados, sin datos del propietario:

1. Login Google y persistencia al cerrar/reabrir. Cancelar selector vuelve a una UI operativa.
2. Omitir onboarding no equivale a completar; Empezar sí persiste.
3. Crear tarea con descripción y fecha; editar, quitar fecha, completar/descompletar, borrar. Reflejo en Home correcto para todas las pendientes.
4. Crear hábito con cada recurrencia, editar objetivo/color, registrar fecha previa y hoy, desmarcar, borrar. Las llamadas y datos son equivalentes a la base; etiquetas corresponden a fechas.
5. Crear Diario, editar texto largo, guardar, reabrir, borrar con confirmación. Fallo y Atrás no pierden borrador silenciosamente.
6. Iniciar/pausar/reanudar/reiniciar Pomodoro. Cambiar tab y volver no reinicia. Background y reapertura recuperan tiempo. Cada cuarta sesión avanza a15min; skip conserva semántica existente.
7. Finalización observada en otra pantalla y desde notificación; una única presentación, texto de próxima sesión correcto.
8. Cambiar los cinco temas y reabrir; cambiar hora/permiso y confirmar que se conserva el scheduling previsto. No sustituirlo por alarmas nuevas.
9. Variar flags con fakes o entorno de prueba: sin tareas/hábitos/diario/Pomodoro, Free/Premium, retirar feature estando dentro. No tocar Remote Config de producción para capturas.
10. SignOut y otra cuenta: no mostrar datos/borradores de la anterior ni mantener una UI autenticada por error de estado local.

Casos de deuda previa que se registran por separado: fechas UTC/local de Todo; semántica acumulativa de racha; precisión de scheduling dependiente del sistema; stubs iOS. No cambiar estos contratos para que un vídeo “parezca mejor”. Si se detecta una regresión nueva, sí corregirla antes de cierre.

## 9. Comandos y evidencia de QA para la fase de implementación

Comandos propuestos para PowerShell, **no ejecutados durante esta auditoría**:

```powershell
.\gradlew.bat :composeApp:assembleDebug --console=plain
.\gradlew.bat :composeApp:testDebugUnitTest --console=plain
.\gradlew.bat :composeApp:lintDebug --console=plain
.\gradlew.bat :composeApp:connectedDebugAndroidTest --console=plain
```

Confirmar primero nombres de tasks disponibles con el Gradle actual. Si una task no existe, usar equivalente documentado, no cambiar AGP para crearla. No invocar clean salvo necesidad concreta. Configurar GRADLE_USER_HOME de trabajo si el entorno lo requiere, sin alterar repositorio ni configuración global del propietario. No ejecutar sincronización de traducciones como parte de QA.

Pruebas nuevas útiles: contratos de operaciones/borradores/carga, guards de navegación, correspondencia de días, selector exclusivo, insets y actions accesibles. No escribir tests que solo comprueban el valor literal de un color.

Revisión visual: capturas320/360/412dp, fuente100/150/200%, retrato/horizontal, teclado y cinco temas. Android real obligatorio al menos en teléfono objetivo. Otro perfil compacto puede probarse en emulador. iOS solo se marca validado si hay entorno Mac y se ejecutan sus comprobaciones; ausencia de Mac no se disfraza de soporte comprobado.

Rendimiento: comparar con baseline en mismo dispositivo/build comparable, capturar scroll de100items, tabs y Pomodoro durante30s. Presupuesto de referencia a60Hz:16.7ms/frame; objetivo≥95% de frames dentro del presupuesto en secuencia de interacción acordada, sin congelaciones perceptibles. Es un objetivo por medir, no una promesa observada. Si profiler disponible, registrar percentiles/jank; si solo hay vídeo, declarar que no sustituye medición. Ningún degradado/aro debe provocar recomposición de toda la app a cada tick.

Cada evidencia lleva: tarea/escenario, build/hash, dispositivo/API, dimensiones/fontScale/locale/tema, datos de fixture, pasos, resultado esperado/observado y archivo de captura. No incluir tokens, correos reales, IDs privados ni contenido personal en el reporte público.

## 10. Definition of Done

- Todas las S00–S12 rediseñadas; no quedan dialogs/estados antiguos fuera del sistema visual.
- Sin cambio de framework, schemas, credenciales, entitlement ni reloj Pomodoro.
- Cinco temas persistentes, todos los estados contrastados y textos localizados.
- Guardar/eliminar muestran resultado real; doble envío bloqueado; error conserva borrador.
- Navegación estable, flags respetados, insets/IME/TalkBack comprobados.
- Motion conforme a tabla y función completa con animaciones desactivadas.
- Build y pruebas pertinentes pasan o limitación previa queda documentada con evidencia; ninguna regresión nueva pendiente.
- QA visual y funcional en Android real completado. Sin dispositivo, declarar implementación pendiente de ese gate, no renovación terminada.
- Informe final lista cambios, validación, riesgos restantes y archivos; no atribuye mejoras de rendimiento no medidas.

La tarea del agente posterior queda ejecutada en este checkout; el registro de gates y los límites restantes están en [`docs/ui-redesign/qa/IMPLEMENTATION.md`](docs/ui-redesign/qa/IMPLEMENTATION.md). No reinterpretar “renovación completa” como permiso para rehacer autenticación, alterar reglas de negocio o publicar cambios remotos.
