import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.example.studyplatform.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("io.ktor:ktor-client-core:2.3.9")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")
            implementation("io.ktor:ktor-client-logging:2.3.9")
            // Group chat over STOMP. The engine plugins (OkHttp, Darwin) provide the
            // transport; this is the client-side WebSocket plugin they need.
            implementation("io.ktor:ktor-client-websockets:2.3.9")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("com.russhwolf:multiplatform-settings:1.1.1")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
            // The local store the UI reads and writes; the network is reconciled later.
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.9")
            implementation(libs.sqldelight.android)
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.9")
            implementation(libs.sqldelight.native)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // The sync tests run on the JVM against an in-memory SQLite. That is the whole
        // point: every behaviour worth testing here — a lost reply, a conflict, a batch
        // that half fails — is defined by what the server said, and none of it can be
        // provoked against a real socket or a real device.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation("app.cash.sqldelight:sqlite-driver:${libs.versions.sqldelight.get()}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

sqldelight {
    databases {
        create("StudyPlatformDatabase") {
            packageName.set("com.example.studyplatform.db")
        }
    }
}
