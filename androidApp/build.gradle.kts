import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Reads a key from local.properties, which is git-ignored and per-developer. */
fun localProperty(key: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return null
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props.getProperty(key)?.takeIf { it.isNotBlank() }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.material:material-icons-extended")
    // Runs sync when the device has a connection again, even if the app is closed.
    implementation(libs.androidx.work.runtime)
    // Google Sign-In through Credential Manager, which is the supported route
    // now that GoogleSignInClient is deprecated.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
}

android {
    // Renamed off com.example, which Google Play rejects as a reserved namespace.
    // applicationId is permanent once an app is listed, so this had to change before a
    // release rather than after — and it is the name that must be registered against the
    // Android OAuth client in Google Cloud (see docs/google-credentials.md).
    //
    // The Kotlin package names are still com.example.studyplatform. That is cosmetic:
    // it has no external consequence and can be renamed at leisure.
    namespace = "tg.edunova.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "tg.edunova.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // The *web* client id, not the Android one — that is what
        // GetGoogleIdOption wants, and passing the Android id there is the
        // well-known cause of an opaque "ApiException: 10".
        //
        // Read from local.properties, which git ignores. It is not a secret (it
        // ships in every app that uses Google Sign-In and is visible in the web
        // client's page source), but it differs per environment, and a value
        // baked into source is one somebody eventually forgets to change.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"" + (localProperty("google.webClientId") ?: "") + "\""
        )
    }

    buildFeatures {
        buildConfig = true
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