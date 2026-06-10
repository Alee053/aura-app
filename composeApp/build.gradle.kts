import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.mockative)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.navigation.compose)
            
            // Firebase Android dependencies - using string notation for BOM due to Kotlin 2.3+ Provider issues
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.config.ktx)
            implementation(libs.firebase.auth.ktx)
            implementation(libs.firebase.firestore.ktx)
            implementation(libs.firebase.messaging.ktx)
            implementation(libs.firebase.database.ktx)
            
            implementation(libs.credentials)
            implementation(libs.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.objenesis)
            implementation(libs.javassist)
            
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.workmanager.ktx)
            implementation(libs.datastore.preferences)
            implementation(libs.androidx.lifecycle.process)
        }
        commonMain.dependencies {
            implementation(project(":designsystem"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.mockative)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mockative)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

android {
    namespace = "com.programovil.aura"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.programovil.aura"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val pullTranslations by tasks.registering {
    group = "localization"
    description = "Downloads es/fr translations from Loco and writes them to values-es/ and values-fr/."

    val resourcesDir = layout.projectDirectory.dir("src/commonMain/composeResources")

    val targets = listOf(
        "es" to "values-es/strings.xml",
        "fr" to "values-fr/strings.xml"
    )

    doLast {
        // Resolution order for LOCO_API_KEY:
        //   1. Real environment variable (so CI / shell overrides work).
        //   2. Project root .env file (developer convenience, gitignored).
        //   3. gradle.properties (escape hatch for systems without .env support).
        val key = providers.environmentVariable("LOCO_API_KEY").orNull
            ?: run {
                val envFile = rootProject.file(".env")
                if (envFile.exists()) {
                    envFile.readLines()
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("LOCO_API_KEY=") }
                        ?.substringAfter("=")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                } else null
            }
            ?: providers.gradleProperty("LOCO_API_KEY").orNull

        if (key.isNullOrBlank()) {
            throw GradleException(
                "LOCO_API_KEY is not set. Add it to a .env file at the repo root, " +
                "export it in your shell, or add `LOCO_API_KEY=...` to gradle.properties. " +
                "Get a key from https://localise.biz (Developer Tools → API Keys)."
            )
        }

        for ((locale, relativePath) in targets) {
            val outFile = resourcesDir.file(relativePath).asFile
            val tmpFile = File(outFile.parentFile, "${outFile.name}.tmp")
            val url = "https://localise.biz/api/export/locale/$locale.xml?key=$key"

            println("[pullTranslations] GET $url → ${outFile.relativeTo(rootDir)}")
            val process = ProcessBuilder(
                "curl", "--silent", "--show-error", "--fail",
                "--output", tmpFile.absolutePath,
                url
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) {
                if (tmpFile.exists()) tmpFile.delete()
                throw GradleException(
                    "[pullTranslations] curl failed for locale '$locale' (exit $exit):\n$output"
                )
            }

            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                throw GradleException(
                    "[pullTranslations] Loco returned an empty file for locale '$locale'. " +
                    "Check that the locale exists in your Loco project."
                )
            }
            if (outFile.exists() && outFile.readText() == tmpFile.readText()) {
                tmpFile.delete()
                println("[pullTranslations] $locale unchanged.")
            } else {
                if (outFile.exists()) outFile.delete()
                tmpFile.renameTo(outFile)
                println("[pullTranslations] $locale updated (${outFile.length()} bytes).")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(pullTranslations)
}
