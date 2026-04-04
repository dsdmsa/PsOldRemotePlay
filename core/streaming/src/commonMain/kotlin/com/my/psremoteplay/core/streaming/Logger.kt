package com.my.psremoteplay.core.streaming

interface Logger {
    fun log(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
