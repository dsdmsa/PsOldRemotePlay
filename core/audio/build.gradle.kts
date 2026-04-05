import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvm("desktop")

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "CoreAudio"; isStatic = true } }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}

android {
    namespace = "com.my.psremoteplay.core.audio"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
