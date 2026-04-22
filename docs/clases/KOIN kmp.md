**KOIN \- KMP**

1. Agregar las variables en el archivo **libs.versions.toml**

**\[versions\]**

**koin \= "4.1.1"**

**\[libraries\]**

**koin-core \= { module \= "io.insert-koin:koin-core", version.ref \= "koin" }**  
**koin-android \= { module \= "io.insert-koin:koin-android", version.ref \= "koin" }**  
**koin-androidx-compose \= { module \= "io.insert-koin:koin-androidx-compose" , version.ref \= "koin"}**  
**koin-compose \= { module \= "io.insert-koin:koin-compose" , version.ref \= "koin"}**  
**koin-compose-viewmodel \= { module \= "io.insert-koin:koin-compose-viewmodel" , version.ref \= "koin"}**

2. Sincronizar el proyecto  
3. Agregar la librería de koin en el archivo **build.gradle.kts (Lo que esta en amarillo) ComposeApp**

***sourceSets*** **{**  
   ***androidMain*.dependencies {**  
       **implementation(*libs*.*compose*.*uiToolingPreview*)**  
       **implementation(*libs*.*androidx*.*activity*.*compose*)**  
       **implementation(*libs*.*koin*.*android*)**  
       **implementation(*libs*.*koin*.*androidx*.*compose*)**  
   **}**  
   ***commonMain*.dependencies {**  
       **implementation(*libs*.*compose*.*runtime*)**  
       **implementation(*libs*.*compose*.*foundation*)**  
       **implementation(*libs*.*compose*.*material3*)**  
       **implementation(*libs*.*compose*.*ui*)**  
       **implementation(*libs*.*compose*.*components*.*resources*)**  
       **implementation(*libs*.*compose*.*uiToolingPreview*)**  
       **implementation(*libs*.*androidx*.*lifecycle*.*viewmodelCompose*)**  
       **implementation(*libs*.*androidx*.*lifecycle*.*runtimeCompose*)**

       ***//koin***  
       **implementation(*libs*.*koin*.*core*)**  
       **implementation(*libs*.*koin*.*compose*)**  
 **implementation(*libs*.*koin*.*compose*.*viewmodel*)**

   **}**  
   ***commonTest*.dependencies {**  
       **implementation(*libs*.*kotlin*.*test*)**  
   **}**  
**}**

4. Sincronizar el proyecto  
5. Crear el paquete **di,** con los siguiente archivos

**![][image1]**

[**InitKoin.kt**](http://InitKoin.kt)

**fun getModules() \= *listOf*(**  
   ***domainModule*,**  
   ***presentationModule*,**  
   ***dataModule***  
**)**

[**PresentationModule.kt**](http://PresentationModule.kt)

**val *presentationModule* \= module {**

**}**

[**DomainModule.kt**](http://DomainModule.kt)

**val *domainModule* \= module {**

**}**

[**DataModuel.kt**](http://DataModuel.kt)

**val *dataModule* \= module {**

**}**

6. Crear el archivo **AndroidApp** en el modulo **androidMain ![][image2], dentro del identificador del tu app**  
   

   
**class AndroidApp: Application() {**

   **override fun onCreate() {**  
       **super.onCreate()**  
       **startKoin {**  
           ***androidLogger*(Level.*ERROR*)**  
           ***androidContext*(this@AndroidApp)**  
           **modules(*getModules*())**  
       **}**  
   **}**  
**}**

7. **Asociar la clase AndroidApp en el archivo AndroidManifest.xml**

***\<?*****xml version\="1.0" encoding\="utf-8"*?\>***  
**\<manifest xmlns:android\="http://schemas.android.com/apk/res/android"\>**

   **\<application**  
       **android:allowBackup\="true"**  
       **android:name\=".AndroidApp"**  
       **android:icon\="@mipmap/ic\_launcher"**

8. **La forma de injectar los ViewModel es:**

**val *presentationModule* \= module {**  
   **viewModelOf(**  
       **::ProductDetailViewModel)**  
**}**

9. **La forma de utilizar el ViewModel en el Screen es**

**@Composable**  
**fun ProductDetailScreen(modifier: Modifier, viewModel: ProductDetailViewModel \= koinViewModel()) {**  


[image1]: <data:image/png;base64,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>

[image2]: <data:image/png;base64,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>