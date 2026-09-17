# Aura demo seed

Herramienta local e independiente para consultar y, cuando se autorice explícitamente, sembrar en Firestore los datos demo definidos en [`../../AURA_DEMO_DATA_PLAN.md`](../../AURA_DEMO_DATA_PLAN.md).

El script usa Firebase Admin SDK con Application Default Credentials. La variable `GOOGLE_APPLICATION_CREDENTIALS` se entrega directamente al SDK; el archivo de credenciales nunca se copia, se imprime ni se lee desde este tooling. `AURA_DEMO_EMAIL` se resuelve mediante Firebase Admin Auth para obtener el UID real.

## Preparación

Desde este directorio instala la dependencia local:

```powershell
npm install
```

Las variables de entorno deben estar configuradas en la misma sesión:

```text
GOOGLE_APPLICATION_CREDENTIALS
AURA_DEMO_EMAIL
```

El script verifica que el project ID detectado por Admin SDK coincida con `.firebaserc`, `composeApp/google-services.json` y el proyecto mencionado por el plan de datos.

## Dry run

Ejecuta primero:

```powershell
node seed-demo.mjs --dry-run
```

El dry run se conecta realmente a Firebase, muestra proyecto, email, UID y los conteos actuales de `todos`, `habits`, `completions` y `journals`. Después lista las 48 rutas que se crearían. No ejecuta ninguna escritura. Si encuentra documentos, informa que `--seed` abortaría.

## Seed real

No ejecutes este comando hasta confirmar el dry run y que la cuenta demo está vacía:

```powershell
node seed-demo.mjs --seed
```

Antes de escribir, el modo seed repite el preflight y aborta si cualquiera de las cuatro colecciones contiene al menos un documento. No borra, actualiza ni sobrescribe. Las escrituras usan `batch.create()` (sin `merge`) y luego se verifican los conteos, IDs y campos de los 48 documentos.

También existen los alias `npm run dry-run` y `npm run seed`.

Este directorio es tooling local: no modifica Kotlin, Compose, ViewModels, repositorios de producción, navegación, Gradle Android, `google-services.json`, UI ni lógica funcional.
