# Inventario de pulido

## Decisiones implementadas

- Se conserva Kotlin/Compose. No hay migración ni backend nuevo.
- Tokens de papel/tinta, cinco paletas, Manrope local, radios y espaciado compartidos; los controles Material reciben el mismo esquema.
- Marca orbital, escenas vectoriales, launcher adaptable y monocromo.
- Inicio editorial, lista de tareas por estado, cards de hábito con fechas reales, Enfoque orbital, Diario editorial, Ajustes con selección exclusiva, acceso y onboarding reconstruidos.
- Navegación tipada estable, barra/rail, destinos secundarios sin barra y aviso al retirarse una feature. Las entradas guardadas retienen el estado de presentación cuando se retira acceso.
- Operaciones observables, borradores, confirmación de descarte/borrado, aviso de larga espera, skeletons y distinción entre vacío y fallo.
- El estado autenticado sobrevive a recreación de Activity mediante un ViewModelStore retenido, pero se limpia al salir de sesión; una generación de sesión separa el estado restaurable de navegación entre cuentas.
- Permiso de notificaciones releído al volver al foreground. La hora respeta 12/24h y ofrece campos apilados con fuente grande.
- Motion nativo; sin Lottie/Rive, shared transitions ni animaciones del reloj que recorran tiempo perdido. Haptics de toggles solo tras respuesta confirmada observada.

## Organización de archivos

Se agrupan primitivas pequeñas por familia (`AuraStates.kt`, `AuraOverlays.kt`) para no crear archivos vacíos ni abstracciones sin consumidores. `PlatformUi` reúne adaptadores de Atrás, escala de animación y reloj. `UiOperations` materializa el contrato C1.

`TodoDialog.kt` y `HabitDialog.kt` ya no contienen el dialog antiguo: alojan los editores sobre `AuraEditorSheet`. Esta nomenclatura heredada no supone conservar el layout anterior.

Se retiraron `DashboardCard`, `PrimaryButton` y `BasicInput` tras comprobar que no tenían consumidores. `retired-components.patch` conserva su contenido previo a esta implementación, incluidos cambios locales anteriores. Es un archivo de recuperación, no código compilado; revisar `git apply --check` antes de intentar restaurarlo.

## Límites preservados

Comparación contra la base local inspeccionada: ningún repositorio, mapper, esquema, contrato de autenticación, motor Pomodoro, scheduler o worker se modifica. La única excepción de dominio es C2: propagar el error combinado del dashboard en vez de convertirlo a ceros. Se actualizaron las dos expectativas unitarias que exigían el comportamiento antiguo.

Las modificaciones anteriores en configuración Firebase, AndroidAuthService, AuthViewModel y settings.gradle pertenecían al checkout inicial y no se han revertido ni reescrito en esta renovación.

## Deuda que permanece separada

- DatePicker UTC frente a representación histórica local de vencimientos: en La Paz, `2026-09-15T00:00:00Z` se representa como 14 de septiembre. No hay migración de Long ni etiquetas engañosas “Hoy/Vencida”.
- Rachas representan registros de períodos consecutivos, no necesariamente días.
- Saltar un enfoque cuenta como sesión en el motor existente.
- iOS sigue siendo un target parcial no compilable en este host Windows.
- Material3 y herramientas de build conservan versiones previas; se registran sus advertencias sin migrarlas dentro de un rediseño.

El gate físico P21/P23 no se cierra por tener tests unitarios o capturas de emulador. Consultar `IMPLEMENTATION.md` para resultados efectivos.
