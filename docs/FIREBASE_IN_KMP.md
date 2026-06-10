# Firebase Integration in Kotlin Multiplatform

This project uses Firebase services for auth, cloud database, remote config, and push messaging.
All Firebase SDKs are Android-only (in `androidMain`). Domain interfaces are defined in `commonMain`
with `expect`/`actual` implementations per platform.

## Firebase Setup (Android)

### 1. Dependencies (`libs.versions.toml`)

```toml
[versions]
firebaseBom = "34.12.0"
firebaseAuth = "23.2.1"
firebaseFirestore = "25.1.4"
firebaseConfigKtx = "22.1.2"
firebase-messaging-ktx = "24.0.0"
kotlinxCoroutinesPlayServices = "1.9.0"

[libraries]
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx", version.ref = "firebaseAuth" }
firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx", version.ref = "firebaseFirestore" }
firebase-config-ktx = { module = "com.google.firebase:firebase-config-ktx", version.ref = "firebaseConfigKtx" }
firebase-messaging-ktx = { module = "com.google.firebase:firebase-messaging-ktx", version.ref = "firebase-messaging-ktx" }
kotlinx-coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutinesPlayServices" }

[plugins]
google-gms-google-services = { id = "com.google.gms.google-services" }
```

### 2. Build Configuration

```kotlin
// composeApp/build.gradle.kts
plugins {
    alias(libs.plugins.google.gms.google.services)
}

sourceSets {
    androidMain.dependencies {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.firestore.ktx)
        implementation(libs.firebase.config.ktx)
        implementation(libs.firebase.messaging.ktx)
        implementation(libs.kotlinx.coroutines.play.services)
    }
}
```

### 3. Services Used

| Service | Purpose | Location |
|---------|---------|----------|
| Firebase Auth | Google Sign-In | `androidMain/` - AndroidAuthService |
| Cloud Firestore | Todo persistence | `androidMain/` - TodoRepositoryImpl |
| Firebase Remote Config | Feature flags | `androidMain/` - FirebaseRemoteConfigService |
| Firebase Cloud Messaging | Push notifications | `androidMain/` - FirebaseMessagingService |
| Firebase Functions | Test notification endpoint | `functions/src/index.ts` |

## Push Notifications (FCM)

### Service Class (`androidMain`)

```kotlin
class FirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Send token to app server
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle received messages
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }
}
```

### Manifest Registration

```xml
<service
    android:name=".FirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## Architecture Pattern

All Firebase services are abstracted behind domain interfaces in `commonMain`:

```kotlin
// commonMain - domain interface
interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun fetchAndActivate(): Result<Unit>
}

// androidMain - actual implementation
class FirebaseRemoteConfigService : RemoteConfigService {
    private val firebase = Firebase.remoteConfig
    // ... implementation
}

// iosMain - stub implementation
class StubRemoteConfigService : RemoteConfigService {
    // ... returns defaults on iOS
}
```
