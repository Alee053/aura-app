# Recursos de Ritmo sereno

La marca orbital y las cuatro escenas de producto se dibujan con Canvas/vector, sin imágenes remotas ni librerías gráficas adicionales. `aura-mark.svg` conserva una versión intercambiable de la marca; el launcher Android incluye foreground, background y monocromo. Los PNG antiguos del launcher quedan como recursos heredados no seleccionados: Android mínimo 24 usa el recurso `mipmap-anydpi` y desde 26 el adaptive icon.

Manrope se empaqueta en cuatro pesos estáticos (400, 500, 600 y 700). Fuente: [Google Fonts / Manrope](https://github.com/google/fonts/tree/main/ofl/manrope). Licencia SIL OFL incluida en `designsystem/src/commonMain/composeResources/files/manrope_OFL.txt`. `scripts/prepare_aura_assets.py` reproduce la descarga y generación con fontTools; no forma parte del build ni descarga fuentes en ejecución.

El símbolo multicolor de Google procede de [los recursos oficiales de identidad](https://developers.google.com/identity/branding-guidelines). Se conserva sin recolorear y sobre blanco. Archivo local: `composeApp/src/commonMain/composeResources/drawable/google_g.png`.

Las traducciones nuevas están agrupadas en `values/redesign.xml`, `values-es/redesign.xml` y `values-fr/redesign.xml`. No se ha sincronizado Loco. Sus tareas manuales anteriores solo procesan `strings.xml`: coordinar la incorporación de las claves `rd_*` antes de una futura sincronización remota.

## Dependencias acotadas

- Animación Compose explícita, misma versión del stack existente.
- Runner AndroidX 1.7.0, exclusivo de instrumentación.
- Host `ui-test-manifest` Android 1.10.5, exclusivo de debug y alineado con el runtime resuelto por Compose Multiplatform 1.10.3.
- No se cambian Kotlin, Compose, Material3, AGP, SDK, Firebase, credenciales ni applicationId.

Referencias de verificación: [pruebas Compose](https://developer.android.com/develop/ui/compose/testing), [accesibilidad de componentes](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test).
