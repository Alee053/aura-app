# Guía rápida para mi compañero — Firebase en AURA

> **Objetivo**: que entiendas qué hace cada servicio de Firebase en AURA, qué nombres usar, dónde se configuran, y cómo verificar que está todo bien.
> **Tiempo de lectura**: ~10 min.

## TL;DR

AURA usa **5 servicios de Firebase**:

| Servicio | Para qué | Dónde se configura |
|---|---|---|
| **Authentication** | Login con Google | Ya estaba, no tocar |
| **Firestore** | Datos de negocio (Todo, Habit, Journal) | Ya estaba, no tocar |
| **Cloud Messaging (FCM)** | Notificaciones push remotas | Ya estaba, no tocar |
| **Realtime Database (RTDB)** | Logs del experimento A/B (estos son nuevos) | **Configurar una vez** |
| **Remote Config** | El plan Free/Premium de cada usuario | **Configurar una vez** |

Los dos nuevos para la tarea de A/B testing: **RTDB** y **Remote Config**.

---

## 1. Realtime Database (RTDB) — dónde se guardan los logs del experimento

### ¿Qué es?

Es una base de datos JSON jerárquica en la nube. Se usa para guardar **eventos** (cada 12h la app postea un "latido" con el plan del usuario). Sirve para analizar luego qué variante (Free o Premium) se usa más.

### Paso 1: Crear la base de datos

1. Abre: **https://console.firebase.google.com/project/aura-6ac09/database**
2. Clica **"Crear base de datos"** (Create database).
3. **Ubicación**: elige `us-central1` (es el default).
4. **Modo de seguridad**: elige **"Empezar en modo de prueba"** (Start in test mode).
5. Clica **"Habilitar"** (Enable). Espera 30-60 segundos.

### Paso 2: Poner las reglas de seguridad

Una vez creada, arriba a la derecha hay una pestaña **"Reglas"** (Rules). Borra todo y pega esto:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

Clica **"Publicar"** (Publish). Confirma.

**Qué hace**: solo el usuario autenticado con UID `abc123` puede leer/escribir en `/users/abc123/...`. Nadie más puede tocar esos datos.

### Paso 3: Verificar que funciona

Después de instalar la app y hacer sign-in, cada 12h aparecerá un documento aquí:
- En Console, en la URL raíz, verás aparecer:
  ```
  users
    └── (tu-UID-de-Google)
        └── experiments
            └── events
                └── -NxYz123abc...
                    ├── type: "session_active"
                    ├── variant: "Premium"
                    ├── timestamp: 1717860000000
                    └── metadata: {}
  ```

Si reinstalas la app, se generará otro evento. La clave numérica (`-NxYz...`) la genera Firebase automáticamente.

### Nombres de variables (lo que la app escribe)

El nombre de las claves JSON es FIJO y está en el código (`FirebaseExperimentRepositoryImpl.kt`). Si quieres cambiarlo, también tendrías que cambiar el código:

| Clave en RTDB | Tipo | Valor | Quién lo escribe |
|---|---|---|---|
| `type` | String | `session_active` / `home_opened` / `tab_clicked` / `notification_delivered` | `ExperimentsHeartbeatWorker` (cada 12h) |
| `variant` | String | `Free` o `Premium` | igual |
| `timestamp` | Long (ms epoch) | hora del evento en milisegundos | igual |
| `metadata` | Object | `{tab: "Todos"}` o `{}` o `{channel: "due_date_reminder"}` | igual |

**No cambies estos nombres** sin actualizar el código. El JSON se ve así:

```json
{
  "type": "session_active",
  "variant": "Premium",
  "timestamp": 1717860000000,
  "metadata": {}
}
```

### Ruta donde se guardan

```
/users/{UID-DEL-USUARIO-AUTHENTICADO}/experiments/events/{ID-AUTO-GENERADO}
```

- `users` — raíz (por convención de Firebase).
- `{UID}` — el identificador único de Google del usuario logueado. Es automático, no lo pones tú.
- `experiments/events` — el namespace de este feature (no lo cambies a menos que cambies el código).
- `{ID}` — Firebase lo genera con `.push()`, formato `-NxYz...`. No es amigable pero garantiza que no colisionen.

---

## 2. Remote Config — la palanca Free vs Premium

### ¿Qué es?

Es un servicio de "configuration as a service". Tú pones pares `clave → valor` en la nube y la app los descarga. Sirve para cambiar el comportamiento sin publicar un APK nuevo.

En AURA lo usamos para **una sola cosa**: el parámetro `user_plan`, que vale `"Free"` o `"Premium"`.

### Paso 1: Crear el parámetro

1. Abre: **https://console.firebase.google.com/project/aura-6ac09/config**
2. Clica **"Add parameter"** o **"Create your first parameter"**.
3. Llena:
   - **Key (clave)**: escribe **exactamente** `user_plan` (en snake_case, sin mayúsculas).
   - **Value type**: **String**.
   - **Default value**: escribe `"Free"` (con F mayúscula — el código busca esto).
4. Clica **"Save"**.
5. Clica **"Publish"** arriba. Confirma.

### Paso 2 (opcional): Crear la condición `is_premium` para A/B testing

Esto es lo que te permite dar `Premium` solo a un porcentaje de usuarios:

1. En la lista de parámetros, busca `user_plan`. Clica el **lápiz** (editar) a la derecha.
2. Verás una columna "Default" con valor `Free`. Hay un botón **"Add condition"**.
3. Llena:
   - **Condition name**: `is_premium` (o lo que quieras, sin espacios).
   - **Applies if**: elige **"Percentile rollout"**.
   - **Percent**: pon `100` (todos), `50` (mitad), o lo que quieras.
   - **Value for this condition**: `Premium`.
4. Clica **"Save condition"**.
5. Clica **"Publish"** de nuevo.

**Cómo funciona el Percentile Rollout**: cuando publicas un rollout del 50%, Firebase le asigna a **cada instalación** de la app un número aleatorio del 0 al 100 al momento de instalar. Si tu número es ≤ 50, recibes el valor de la condición (`Premium`); si es > 50, recibes el Default (`Free`). **Es permanente por instalación** (no cambia cada vez que abres la app).

**Tabla resumen** (mi recomendación para empezar):

| Default value | Condición `is_premium` (rollout X%) | Resultado |
|---|---|---|
| `Free` | (sin condición) | 100% Free |
| `Premium` | (sin condición) | 100% Premium |
| `Free` | X% → `Premium` | X% Premium, (100−X)% Free |

**Para tu demo**: pon **Default = `Premium`**, **sin condición**. Así todos los usuarios serán Premium. Cuando quieras mostrar el A/B real, agrega la condición con rollout al 50%.

### Paso 3: Verificar que la app lo lee

1. Reinstala la app: `./gradlew :composeApp:installDebug`
2. Abre la app.
3. Mira Logcat con filtro `tag:UserPlanManager` o `tag:FirebaseRemoteConfig`. Deberías ver:
   ```
   UserPlanManager.initialize() — fetching from Remote Config
   FirebaseRemoteConfig.getUserPlan: raw="Premium" effective="Premium"
   UserPlanManager initialized, current value: Premium
   ```
4. La UI debería mostrar **5 tabs** (incluyendo Habits y Journal) y la tarjeta **DAILY MOTIVATION** en Home.

### Cómo se propaga el cambio en vivo (sin cerrar la app)

La app usa **3 mecanismos** para enterarse de cambios en Remote Config:

1. **Real-time listener** (`addOnConfigUpdateListener`): Firebase empuja una notificación al dispositivo cuando publicas un cambio. Llega en 1-3 segundos.
2. **Polling de 30s** (`FeatureFlagManager` + `UserPlanManager`): cada 30 segundos la app revisa si hay cambios, por si el listener falló.
3. **Fetch en init**: cuando abres la app autenticada, hace un fetch + activate.

**Por eso, si cambias `user_plan` en Console y la app está abierta, en 1-30 segundos debería actualizarse sin tocarla.**

### Nombres de variables en el código

| Constante | Valor | Dónde se usa |
|---|---|---|
| `UserPlanFlag.USER_PLAN.key` | `"user_plan"` | Clave de Remote Config (debe coincidir con la que creaste en Console) |
| `UserPlanFlag.USER_PLAN.defaultValue` | `"Free"` | Default local si la app no puede fetchear (red caída) |
| `UserPlan.fromRemoteConfigString("Premium")` | `UserPlan.Premium` | Mapeo de string a sealed class |
| `UserPlan.fromRemoteConfigString("Free")` | `UserPlan.Free` | igual |
| `UserPlan.fromRemoteConfigString(otro)` | `UserPlan.Free` | Default seguro |

**No cambies los strings** `"Free"` y `"Premium"` en el código a menos que también cambies la convención en Console. El mapeo es case-sensitive.

---

## 3. Cómo interactúan ambos servicios (resumen)

```
Firebase Console
  ├── Remote Config
  │     └── user_plan = "Premium"        ← cambias aquí
  │
  └── Realtime Database
        └── users/{uid}/experiments/events/  ← lees los logs aquí

Android App
  ├── UserPlanManager ← lee Remote Config cada 30s + listener
  │     │
  │     ├── actualiza UI (tabs, Home, notificaciones)
  │     │
  │     └── ExperimentsHeartbeatWorker (cada 12h)
  │           └── escribe a Realtime Database
```

**El ciclo completo**:
1. Tú cambias `user_plan = "Premium"` en Console.
2. En menos de 30s, la app lo lee y actualiza la UI (5 tabs, tarjeta motivacional, 2 notif/día).
3. En el próximo latido (12h después), se escribe un evento `session_active` con `variant: "Premium"` a RTDB.
4. Tú abres RTDB en Console y ves la actividad de los usuarios agrupados por variante.

---

## 4. Lo que NO tienes que tocar (ya está hecho)

- **Authentication (Google Sign-In)** — ya está configurado y funcionando.
- **Firestore** — guarda Todo/Habit/Journal. No se usa para este feature.
- **Cloud Functions** — el endpoint `sendTestNotification` ya existe en `functions/src/index.ts`. No necesitas tocarlo.
- **FCM (Firebase Messaging)** — el service de Android ya está registrado. Las notificaciones push ya funcionan.
- **Cloud Storage** — AURA no lo usa.

---

## 5. Resumen de nombres — tabla rápida

| Concepto | Nombre exacto | Dónde se define | Quién lo lee |
|---|---|---|---|
| Clave de Remote Config | `user_plan` | Console + `FeatureFlags.kt` | `FirebaseRemoteConfigService.getUserPlan()` |
| Default value | `"Free"` | `FeatureFlags.kt` | Código (fallback si RC falla) |
| Valor Free en código | `UserPlan.Free` (sealed class) | `experiments/domain/model/UserPlan.kt` | toda la app |
| Valor Premium en código | `UserPlan.Premium` | igual | igual |
| Ruta de RTDB | `/users/{uid}/experiments/events/{pushId}` | `FirebaseExperimentRepositoryImpl.logEvent()` | Workers, Console |
| Nombre del evento (heartbeat) | `session_active` | `FirebaseExperimentRepositoryImpl.eventTypeName()` | `ExperimentsHeartbeatWorker` |
| Nombre del evento (Home) | `home_opened` | igual | (futuro) HomeViewModel |
| Nombre del evento (tab) | `tab_clicked` | igual | (futuro) App.kt |
| Nombre del evento (notif) | `notification_delivered` | igual | (futuro) DailySummaryWorker |
| Tag en código (variante) | `"Free"` o `"Premium"` | `UserPlan.variantName()` | RTDB payload |
| Tag para Firebase Analytics user property | `plan` (= `"free"` o `"premium"`) | (no implementado aún) | — |

---

## 6. Errores comunes

1. **Confundir `user_plan` (mayúscula P)** con `UserPlan` (PascalCase). La clave de Remote Config es minúscula con guión bajo. El nombre de la sealed class es CamelCase.
2. **Olvidar Publicar** después de cambiar un valor. Console autosave no existe; tienes que clicar el botón azul "Publish" arriba.
3. **Poner la condición `is_premium` con valor `Free`** pensando que "todos serán Free". El valor de la condición es **lo que reciben los que caen en la condición** (no el default).
4. **Olvidar borrar `aura_preferences` al reinstalar**. Si el caché de DataStore tiene un valor "Free" viejo y tu Remote Config dice "Premium", el `UserPlanManager` se actualiza pero la primera lectura del flow todavía devuelve el cache viejo. En la práctica no es problema porque el flow re-emite en cuanto el manager cambia.

---

## 7. Para verificar TODO de un vistazo

```bash
# 1. Ver el SHA-1 de tu debug keystore (debe coincidir con el registrado en Firebase)
keytool -list -v -keystore $HOME/.android/debug.keystore -storepass android -alias androiddebugkey 2>&1 | grep SHA1

# 2. Ver la lista de cambios en esta rama
git log --oneline master..feature/extra-credit-implementation

# 3. Verificar que el build funciona
./gradlew :composeApp:compileDebugKotlinAndroid

# 4. Correr tests (debe dar 70 tests, 10 failed — los 10 son pre-existentes)
./gradlew :composeApp:testDebugUnitTest

# 5. Ver logs en vivo durante una prueba
adb logcat -c
adb logcat -s "UserPlanManager:*" "FirebaseRemoteConfig:*" "AuraSignIn:*"
```

---

Si tienes dudas sobre algo específico, pregunta antes de tocar nada. Es mejor preguntar que romper el setup.
