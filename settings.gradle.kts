rootProject.name = "PsRemotePlay"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Core submodules
include(":core:model")
include(":core:video")
include(":core:audio")
include(":core:upscale")
include(":core:crypto")
include(":core:streaming") // umbrella — re-exports all core submodules for backward compat
include(":core:ui")

// Feature modules
include(":feature:ps3")
include(":feature:ps2")
include(":feature:ps2:protocol")

// App modules
include(":app:ps3")
include(":app:ps2server")
include(":app:ps2client")