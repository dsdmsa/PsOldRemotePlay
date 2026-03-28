package com.my.psoldremoteplay.protocol

interface PremoLogger {
    fun log(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
