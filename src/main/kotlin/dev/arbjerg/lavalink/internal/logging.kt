package dev.arbjerg.lavalink.internal

import org.slf4j.Logger

/**
 * Log a trace message to the console.
 * If [Logger.isTraceEnabled] returns false, it will log [message] as warning instead.
 */
fun Logger.warnOrTrace(message: String, t: Throwable) {
    if (this.isTraceEnabled) {
        this.trace(message, t)
    } else {
        this.warn(message)
    }
}
