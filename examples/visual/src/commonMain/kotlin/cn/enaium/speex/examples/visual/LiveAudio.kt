/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.enaium.speex.examples.visual

import cn.enaium.audio.AudioException
import cn.enaium.audio.AudioFormat
import cn.enaium.audio.AudioInput
import cn.enaium.audio.AudioOutput
import cn.enaium.audio.AudioSystem
import cn.enaium.audio.audioSystem
import cn.enaium.sdl.SDL
import kotlin.concurrent.Volatile

/**
 * Owns the real microphone and playback devices for the live page.
 *
 * The render thread only pulls streams that are already open and moves samples
 * with non-blocking reads/writes; opening and closing - the calls that can
 * block on a permission prompt or a device start - happen on [DeviceThread] and
 * the streams are handed between the two threads.
 */
internal class LiveAudio : AutoCloseable {

    /** The format the live pipeline works at: 48 kHz mono speech. */
    val format: AudioFormat = AudioFormat.SPEECH

    private val system: AudioSystem? = runCatching { audioSystem() }.getOrNull()

    /** Backend serving the devices, e.g. `"Core Audio"`. */
    val systemName: String = system?.name ?: "audio unavailable"

    private val deviceThread = DeviceThread("speex-kmp-visual-audio") { deviceLoop() }

    @Volatile
    private var captureWanted = false

    @Volatile
    private var outputWanted = false

    @Volatile
    private var loopClosed = false

    @Volatile
    private var openedCapture: AudioInput? = null

    @Volatile
    private var openedOutput: AudioOutput? = null

    @Volatile
    private var captureOwned = false

    @Volatile
    private var outputOwned = false

    @Volatile
    private var closingCapture: AudioInput? = null

    @Volatile
    private var closingOutput: AudioOutput? = null

    /** Why the capture device is not open, or `null` while it is. */
    @Volatile
    var captureError: String? = null
        private set

    /** Why the playback device is not open, or `null` while it is. */
    @Volatile
    var playbackError: String? = null
        private set

    @Volatile
    var captureDeviceName: String = "system default"
        private set

    @Volatile
    var playbackDeviceName: String = "system default"
        private set

    /** Frames the capture buffer accepts in one read: 100 ms at 48 kHz. */
    val captureBufferFrames: Int = 4_800

    /** Frames handed to the playback device in one non-blocking write. */
    val playbackBufferFrames: Int = 1_024

    private var closed = false

    init {
        deviceThread.start()
    }

    /** Asks the device thread to open ([enabled]) or release the microphone. */
    fun requestCapture(enabled: Boolean) {
        if (closed) return
        captureWanted = enabled
        if (!enabled) closeCapture()
    }

    /** Asks the device thread to open ([enabled]) or release the playback device. */
    fun requestOutput(enabled: Boolean) {
        if (closed) return
        outputWanted = enabled
        if (!enabled) closeOutput()
    }

    /** Takes the capture stream opened by the device thread, if one is ready. */
    fun takeCapture(): AudioInput? {
        val stream = openedCapture ?: return null
        openedCapture = null
        captureOwned = true
        return stream
    }

    /** Takes the playback stream opened by the device thread, if one is ready. */
    fun takeOutput(): AudioOutput? {
        val stream = openedOutput ?: return null
        openedOutput = null
        outputOwned = true
        return stream
    }

    /** Hands a render-owned capture stream back to the device thread to close. */
    fun releaseCapture(stream: AudioInput) {
        captureOwned = false
        closingCapture = stream
    }

    /** Hands a render-owned playback stream back to the device thread to close. */
    fun releaseOutput(stream: AudioOutput) {
        outputOwned = false
        closingOutput = stream
    }

    override fun close() {
        if (closed) return
        closed = true
        captureWanted = false
        outputWanted = false
        closeCapture()
        closeOutput()
        loopClosed = true
        deviceThread.join()
        system?.close()
    }

    private fun deviceLoop() {
        while (!loopClosed) {
            var waited = false

            closingCapture?.let { stream ->
                closingCapture = null
                stream.close()
            }
            closingOutput?.let { stream ->
                closingOutput = null
                stream.close()
            }

            if (captureWanted && openedCapture == null && !captureOwned) {
                val stream = openCapture()
                if (stream == null) {
                    waited = true
                } else if (captureWanted) {
                    openedCapture = stream
                } else {
                    stream.close()
                }
            }

            if (outputWanted && openedOutput == null && !outputOwned) {
                val stream = openOutput()
                if (stream == null) {
                    waited = true
                } else if (outputWanted) {
                    openedOutput = stream
                } else {
                    stream.close()
                }
            }

            // A failed open is retried slowly: a permission prompt can take a
            // while and hammering it would keep the machine busy for nothing.
            SDL.delay(if (waited) RETRY_MILLIS else POLL_MILLIS)
        }

        closingCapture?.close()
        closingOutput?.close()
        openedCapture?.close()
        openedOutput?.close()
    }

    private fun openCapture(): AudioInput? {
        val system = system ?: run {
            captureError = "no audio system available"
            return null
        }
        return try {
            val stream = system.openInput(format, bufferFrames = captureBufferFrames)
            if (stream.format != format) {
                stream.close()
                captureError = "device opened as ${stream.format}, expected $format"
                report("capture unavailable: $captureError")
                null
            } else {
                stream.start()
                captureDeviceName = stream.device?.name ?: system.defaultInputDevice()?.name ?: "system default"
                captureError = null
                report("microphone: $captureDeviceName [$systemName] $format")
                stream
            }
        } catch (e: AudioException) {
            if (captureError == null) {
                val available = system.inputDevices().joinToString { it.name }
                captureError = "${e.message} - inputs: [$available]"
                report("capture unavailable: $captureError; retrying")
            }
            null
        }
    }

    private fun openOutput(): AudioOutput? {
        val system = system ?: run {
            playbackError = "no audio system available"
            return null
        }
        return try {
            val stream = system.openOutput(format, bufferFrames = captureBufferFrames)
            if (stream.format != format) {
                stream.close()
                playbackError = "device opened as ${stream.format}, expected $format"
                report("playback unavailable: $playbackError")
                null
            } else {
                stream.start()
                playbackDeviceName = stream.device?.name ?: system.defaultOutputDevice()?.name ?: "system default"
                playbackError = null
                report("playback: $playbackDeviceName [$systemName] $format")
                stream
            }
        } catch (e: AudioException) {
            if (playbackError == null) {
                val available = system.outputDevices().joinToString { it.name }
                playbackError = "${e.message} - outputs: [$available]"
                report("playback unavailable: $playbackError; retrying")
            }
            null
        }
    }

    private fun closeCapture() {
        openedCapture?.let { stream ->
            openedCapture = null
            closingCapture = stream
        }
    }

    private fun closeOutput() {
        openedOutput?.let { stream ->
            openedOutput = null
            closingOutput = stream
        }
    }

    private companion object {
        const val POLL_MILLIS = 20
        const val RETRY_MILLIS = 250
    }
}
