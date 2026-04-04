package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger
import java.awt.Robot

class KeyInjector(private val logger: Logger) {
    private val robot: Robot = try {
        Robot()
    } catch (e: Exception) {
        logger.error("INPUT", "Failed to create Robot (need Accessibility permission): ${e.message}", e)
        throw e
    }

    fun pressKey(keyCode: Int) {
        try {
            robot.keyPress(keyCode)
        } catch (e: Exception) {
            logger.error("INPUT", "keyPress($keyCode) failed: ${e.message}")
        }
    }

    fun releaseKey(keyCode: Int) {
        try {
            robot.keyRelease(keyCode)
        } catch (e: Exception) {
            logger.error("INPUT", "keyRelease($keyCode) failed: ${e.message}")
        }
    }
}
