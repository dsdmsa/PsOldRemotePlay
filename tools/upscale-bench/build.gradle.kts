plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set(
        providers.gradleProperty("mainClass").getOrElse("bench.BenchmarkKt")
    )
}

dependencies {
    // No external deps needed - just java.awt for image processing
}

kotlin {
    jvmToolchain(17)
}
