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
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "CoreStreaming"; isStatic = true }
    }

    sourceSets {
        commonMain.dependencies {
            // Re-export all core submodules for backward compatibility
            api(projects.core.model)
            api(projects.core.video)
            api(projects.core.audio)
            api(projects.core.upscale)
            api(projects.core.crypto)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}

android {
    namespace = "com.my.psremoteplay.core.streaming"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
