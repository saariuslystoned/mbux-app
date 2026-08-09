package com.saariuslystoned.mbux.platform.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.content.ContextCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Captures 16 kHz mono PCM into one reusable short-lived buffer and immediately overwrites it.
 * No captured bytes leave this class or are written to storage.
 */
class AndroidLocalAudioCapture(context: Context) : LocalAudioCapture {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mbux-local-mic").apply { isDaemon = true }
    }
    private val lock = Any()
    private var activeSession: CaptureSession? = null
    private var closed = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (
            change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            stopActive(CaptureInterruption.AudioFocusLost)
        }
    }
    private val focusRequest = AudioFocusRequest.Builder(
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
    )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setAcceptsDelayedFocusGain(false)
        .setOnAudioFocusChangeListener(focusListener, mainHandler)
        .build()

    override fun start(onInterrupted: (CaptureInterruption) -> Unit): CaptureStartResult {
        if (!hasRecordAudioPermission()) {
            return CaptureStartResult.PermissionMissing
        }

        synchronized(lock) {
            if (closed) return CaptureStartResult.Failed("Microphone capture is closed")
            if (activeSession != null) {
                return CaptureStartResult.Failed("A previous capture is still stopping")
            }
        }

        val minimumBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBufferBytes <= 0) {
            return CaptureStartResult.Failed("This device could not configure the microphone")
        }

        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return CaptureStartResult.Failed("Audio focus is unavailable")
        }

        val recorder = createRecorder(max(minimumBufferBytes, READ_BUFFER_BYTES * 4))
        if (recorder == null) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return if (hasRecordAudioPermission()) {
                CaptureStartResult.Failed("This device could not initialize the microphone")
            } else {
                CaptureStartResult.PermissionMissing
            }
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return focusReleasedFailure("This device could not initialize the microphone")
        }

        lateinit var captureSession: CaptureSession
        val timeout = Runnable { stopActive(CaptureInterruption.TimeLimitReached) }
        captureSession = CaptureSession(
            recorder = recorder,
            onInterrupted = onInterrupted,
            timeout = timeout,
        )

        synchronized(lock) {
            if (closed || activeSession != null) {
                recorder.release()
                audioManager.abandonAudioFocusRequest(focusRequest)
                return CaptureStartResult.Failed("Microphone capture is unavailable")
            }
            activeSession = captureSession
        }

        try {
            recorder.startRecording()
            mainHandler.postDelayed(timeout, MAX_CAPTURE_MILLIS)
            executor.execute { readAndDiscard(captureSession) }
        } catch (_: SecurityException) {
            finishFailedStart(captureSession)
            return CaptureStartResult.PermissionMissing
        } catch (_: IllegalStateException) {
            finishFailedStart(captureSession)
            return CaptureStartResult.Failed("The microphone could not start")
        } catch (_: RejectedExecutionException) {
            finishFailedStart(captureSession)
            return CaptureStartResult.Failed("Microphone capture is closed")
        }

        return CaptureStartResult.Started
    }

    override fun stop() {
        stopActive(interruption = null)
    }

    override fun cancel() {
        stopActive(interruption = null)
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }
        stopActive(interruption = null)
        executor.shutdown()
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(bufferSizeBytes: Int): AudioRecord? = try {
        // start() performs the runtime permission check immediately before calling this method.
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .build()
    } catch (_: SecurityException) {
        null
    } catch (_: UnsupportedOperationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun readAndDiscard(session: CaptureSession) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val buffer = ByteArray(READ_BUFFER_BYTES)
        var readFailed = false

        try {
            while (session.active.get()) {
                val bytesRead = try {
                    session.recorder.read(
                        buffer,
                        0,
                        buffer.size,
                        AudioRecord.READ_BLOCKING,
                    )
                } catch (_: IllegalStateException) {
                    AudioRecord.ERROR_INVALID_OPERATION
                }

                if (bytesRead > 0) {
                    buffer.fill(0, fromIndex = 0, toIndex = bytesRead)
                } else if (bytesRead < 0 && session.active.compareAndSet(true, false)) {
                    readFailed = true
                }
            }
        } finally {
            buffer.fill(0)
            finishSession(session)
            if (readFailed) {
                mainHandler.post { session.onInterrupted(CaptureInterruption.ReadFailed) }
            }
        }
    }

    private fun stopActive(interruption: CaptureInterruption?) {
        val session = synchronized(lock) { activeSession } ?: return
        if (!session.active.compareAndSet(true, false)) return

        safeStop(session.recorder)
        session.finished.await(STOP_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        if (interruption != null) session.onInterrupted(interruption)
    }

    private fun finishSession(session: CaptureSession) {
        mainHandler.removeCallbacks(session.timeout)
        safeStop(session.recorder)
        session.recorder.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
        synchronized(lock) {
            if (activeSession === session) activeSession = null
        }
        session.finished.countDown()
    }

    private fun finishFailedStart(session: CaptureSession) {
        session.active.set(false)
        mainHandler.removeCallbacks(session.timeout)
        safeStop(session.recorder)
        session.recorder.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
        synchronized(lock) {
            if (activeSession === session) activeSession = null
        }
        session.finished.countDown()
    }

    private fun focusReleasedFailure(message: String): CaptureStartResult.Failed {
        audioManager.abandonAudioFocusRequest(focusRequest)
        return CaptureStartResult.Failed(message)
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun safeStop(recorder: AudioRecord) {
        try {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        } catch (_: IllegalStateException) {
            // The worker still releases the recorder and abandons focus.
        }
    }

    private class CaptureSession(
        val recorder: AudioRecord,
        val onInterrupted: (CaptureInterruption) -> Unit,
        val timeout: Runnable,
        val active: AtomicBoolean = AtomicBoolean(true),
        val finished: CountDownLatch = CountDownLatch(1),
    )

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val BYTES_PER_SAMPLE = 2
        const val READ_SLICE_MILLIS = 20
        const val READ_BUFFER_BYTES = SAMPLE_RATE_HZ * BYTES_PER_SAMPLE * READ_SLICE_MILLIS / 1_000
        const val MAX_CAPTURE_MILLIS = 30_000L
        const val STOP_WAIT_MILLIS = 500L
    }
}
