# Matriz de estados y evidencia

Esta matriz acompaña al plan; no sustituye la validación física. Los datos de las capturas son fixtures locales, nunca registros de Firebase.

| Superficie | Estados implementados | Verificación automatizada disponible |
|---|---|---|
| Sesión/acceso | resolución, solicitud Google en curso, fallo legible | AuthViewModelTest; fixture de acceso normal y ocupado |
| Onboarding | carga, cuatro páginas, atrás/siguiente, saltar/iniciar | suite OnboardingViewModelTest existente; revisar visualmente las cuatro páginas |
| Inicio | carga, datos, fallo, recuperación, features ocultas | HomeViewModelTest; GetDashboardDataUseCaseTest; fixtures normal/error |
| Tareas | vacío confirmado, grupos, carga/error, toggle pendiente | TodoViewModelTest; fixture lista/editor |
| Editor tarea | limpio/sucio, fecha, pendiente/error, descarte/borrado | fixture de edición con resultado diferido y fallo; Saveable snapshot |
| Hábitos | recurrencia, período, fechas, pendiente/error | HabitViewModelTest; fixture con fechas reales de la semana |
| Editor hábito | diario/semanal/mensual, objetivo validado, color actual, borrador | contratos de dominio existentes; controles compartidos; QA visual pendiente |
| Enfoque | listo/corriendo/pausado, descanso corto/largo, restauración | suite PomodoroViewModelTest sin cambios; fixtures claro/oscuro/contraste/texto grande |
| Finalización | enfoque/descanso, cierre, espera detrás de otro modal | motor/handler existente; arbitraje visual en App; QA física pendiente |
| Diario | vacío/carga/error, lista mensual, borrado confirmado y pendiente | JournalViewModelTest; fixture lista |
| Editor Diario | nuevo/existente, borrador, no encontrado, fallo, save/delete pendiente | JournalDetailViewModelTest ampliado; fixture de escritura |
| Ajustes | cinco temas exclusivos, permiso, hora 12/24h, texto grande, logout | SettingsViewModelTest existente; fixture de tiles; flujo del permiso requiere Android |

## Garantías de operación

`UiOperationsTest` verifica Pending síncrono, bloqueo de doble envío, identidad de resultado, operación larga a los 10s, reintento después del fallo, objetivos independientes y propagación de cancelación. Los ViewModels mantienen filas pendientes ante snapshots optimistas; una emisión local no es confirmación de escritura.

`JournalDetailViewModelTest` verifica además que un ID inexistente nunca se convierta silenciosamente en alta, que el borrador restaurado gane al texto remoto, que se recuperen excepciones de carga y que un fallo conserve exactamente los espacios del texto.

`AuraContrastTest` valida pares de texto de los cinco temas con ratio ≥4.5 y límites de control ≥3. Es contraste de tokens opacos: no reemplaza la inspección de composición ni TalkBack.

## Casos físicos que no pueden darse por aprobados con fixtures

OAuth Google (incluidos cancelar/reintentar), CRUD Firebase en cuenta de prueba autorizada, latencia/red real, cambio de cuenta, notificaciones y canal del sistema, timer en background/proceso recreado, TalkBack, respuesta háptica, teclado del dispositivo, rendimiento/jank y navegación predictiva. No usar datos reales del propietario para completar esta matriz.
