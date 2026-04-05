import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")

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
            baseName = "FeaturePs2"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            api(projects.core.streaming)
            api(projects.core.ui)
            api(projects.feature.ps2.protocol)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        }

        desktopMain.dependencies {
            // JavaCV for capture + H.264 encode/decode + RTP transport
            implementation("org.bytedeco:javacv:1.5.11")
            implementation("org.bytedeco:ffmpeg:7.1-1.5.11:macosx-arm64")
            implementation("org.bytedeco:ffmpeg:7.1-1.5.11:linux-x86_64")
            implementation("org.bytedeco:javacpp:1.5.11:macosx-arm64")
            implementation("org.bytedeco:javacpp:1.5.11:linux-x86_64")
        }

        // Note: JavaCV removed from Android — JPEG_UDP preset uses only BitmapFactory.
        // Re-add bytedeco deps here when testing H.264/RTP presets on Android.
    }
}

android {
    namespace = "com.my.psremoteplay.feature.ps2"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // No packaging overrides needed — JavaCV removed from Android

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
