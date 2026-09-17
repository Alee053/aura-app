# Aura — registro de implementación y QA

Fecha de cierre local: 15 de septiembre de 2026. Base: checkout local con HEAD `5e29bc0` y cambios previos del usuario. La renovación se realizó sin commits, push, despliegues ni escrituras remotas.

## Resultado

La dirección “Ritmo sereno” está implementada en Kotlin/Compose: design system con tokens de papel/tinta, Manrope local, marca orbital, superficies y estados compartidos; shell de navegación adaptable; Inicio editorial; Tareas, Hábitos, Enfoque, Diario, Ajustes, acceso y onboarding reconstruidos; motion respetuoso de escala del sistema; overlays, haptics acotados, borradores y feedback de operaciones.

Se preservaron Firebase, autenticación, persistencia, navegación funcional, lógica de negocio, motor Pomodoro, scheduler, workers y configuración Android. La única excepción funcional deliberada es C2: el dashboard propaga un fallo de fuente en vez de presentarlo como cero, con cobertura actualizada.

## Gates ejecutados

| Gate | Resultado | Evidencia |
|---|---|---|
| Unit tests | 190 tests, 0 failures, 0 skipped | `:composeApp:testDebugUnitTest` |
| Android build | PASS | `:composeApp:assembleDebug` |
| Instrumentation APK | PASS | `:composeApp:assembleDebugAndroidTest` |
| Android lint | PASS, 0 errores; 57 warnings | `:composeApp:lintDebug` |
| Visual fixture suite | 11/11 PASS | `docs/ui-redesign/qa/instrumentation-phone.txt` |
| Capturas | 25 PNGs generados | `docs/ui-redesign/qa/captures/ui-qa/phone/` |

Comando reproducible del gate de build:

```text
gradlew.bat :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:testDebugUnitTest --max-workers=2 --no-daemon -Pkotlin.compiler.execution.strategy=in-process --console=plain
```

El lint deja únicamente advertencias conocidas del proyecto/tooling: DSL de Koin deprecado, `confirmValueChange` de swipe y avisos Gradle/dependencias. No se migraron APIs funcionales ajenas al alcance visual.

## Batería visual reproducible

Se usó exclusivamente el emulador `emulator-5556` (`Medium_Phone`, Android 37.1, 1080×2400) con red desactivada, APK local y fixtures en memoria. El runner `AuraUiTestRunner` evita inicializar Firebase/Koin de producción. Las 11 escenas cubren Inicio/navegación, acceso normal y cargando, error de Inicio, Enfoque oscuro con tipografía grande, alto contraste, fechas reales de Hábitos, Tareas pendiente/error, Diario lista/editor y temas.

Script:

```text
scripts/verify_aura_ui.ps1 -SkipBuild
```

El script solo acepta seriales `emulator-*`, instala los APKs locales, activa modo avión, ejecuta `AuraVisualTest`, guarda el resultado y extrae las capturas. No se utiliza ni se rellena Firestore.

APK debug generado:

`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

SHA-256: `36B2819306AF63EA94BA5EB489804261B70CDA4DB6A3D8E91BA80EE3CF3304FD`

## Revisión visual observada

- Inicio presenta jerarquía editorial, aro de progreso, acción primaria y navegación persistente.
- Tareas conserva el borrador cuando falla una operación y muestra el error dentro de la hoja.
- Enfoque mantiene el reloj protagonista en oscuro/alto contraste y soporta tipografía grande con contenido desplazable.
- Diario mantiene texto y espacios del borrador, confirma acciones destructivas y diferencia carga, vacío y fallo.
- Ajustes usa selección exclusiva para temas y controles de fila completos; onboarding/acceso comparten la marca orbital.

## Límites y deuda separada

- No hubo dispositivo Android físico disponible en este checkout. Quedan pendientes el gate P21/P23 en hardware real, TalkBack/lector de pantalla, touch/haptics reales, rendimiento con datos abundantes, teclado y ventana de 320dp.
- OAuth/Firebase reales, FCM, permisos y notificaciones deben verificarse en una cuenta/dispositivo de prueba conectado; las pruebas visuales no fingen ese contrato.
- Vencimientos Todo: el DatePicker conserva la interpretación histórica UTC→fecha local y puede mostrar el día anterior en La Paz; no se migran Long ni datos.
- Rachas representan períodos consecutivos según el algoritmo existente, no necesariamente días.
- Saltar un enfoque cuenta como sesión según el motor existente.
- iOS continúa siendo el target parcial anterior; no se declara una validación iOS.

Estas deudas no bloquean la entrega visual local, pero sí deben constar antes de presentar Aura como verificada en producción.
