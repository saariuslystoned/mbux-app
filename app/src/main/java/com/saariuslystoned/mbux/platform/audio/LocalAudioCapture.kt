package com.saariuslystoned.mbux.platform.audio

sealed interface CaptureStartResult {
    data object Started : CaptureStartResult

    data object PermissionMissing : CaptureStartResult

    data class Failed(val userMessage: String) : CaptureStartResult
}

enum class CaptureInterruption {
    AudioFocusLost,
    TimeLimitReached,
    ReadFailed,
}

/** Local capture boundary. Implementations must not retain, persist, or transmit audio. */
interface LocalAudioCapture : AutoCloseable {
    fun start(onInterrupted: (CaptureInterruption) -> Unit): CaptureStartResult

    /** Stops normally and discards captured data. */
    fun stop()

    /** Stops an incomplete interaction and discards captured data. */
    fun cancel()

    override fun close()
}
