package com.my.psoldremoteplay

class DesktopPlatform : Platform {
    override val name: String = "Desktop JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = DesktopPlatform()
