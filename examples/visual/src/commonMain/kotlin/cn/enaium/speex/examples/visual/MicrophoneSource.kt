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

import cn.enaium.audio.AudioBuffer
import cn.enaium.audio.AudioInput
import cn.enaium.audio.AudioOutput
import cn.enaium.speex.SpeexResampler

/** The signal a page analyses. */
internal enum class PageSource(val label: String) {
    SYNTHETIC("synthetic speech"),
    MICROPHONE("microphone"),
}

/**
 * One page's view of the shared microphone.
 *
 * [LiveAudio] owns the device; this class converts its 48 kHz mono stream to the
 * page rate and delivers complete frames. It also keeps a short capture window
 * for Replay and can loop the raw microphone to the speakers for monitoring.
 *
 * Only the selected page is pumped, so at most one instance records at a time.
 * The 48 kHz device stream is opened by [LiveAudio]; the page side only reads a
 * ready stream with non-blocking calls.
 */
internal class MicrophoneSource(private val audio: LiveAudio) : AutoCloseable {

    /** Rate the page consumes. */
    var targetRate: Int = audio.format.sampleRate
        private set

    /** Frames per callback at [targetRate]. */
    var frameSize: Int = 480
        private set

    private var resampler: SpeexResampler? = null
    private var capture: AudioInput? = null
    private var output: AudioOutput? = null

    private val captureBuffer = AudioBuffer(audio.format, audio.captureBufferFrames)
    private val playbackBuffer = AudioBuffer(audio.format, audio.playbackBufferFrames)
    private val capturePcm = ShortArray(audio.captureBufferFrames)
    private var pending = ShortArray(audio.captureBufferFrames + frameSize)
    private var pendingCount = 0

    /** Raw 48 kHz capture kept for Replay: two seconds. */
    private val record = ShortArray(audio.format.sampleRate * 2)
    private var recordWrite = 0
    private var recordSize = 0

    /** Raw 48 kHz samples waiting for the playback device. */
    private val monitorQueue = ShortArray(audio.format.sampleRate * 2)
    private var monitorRead = 0
    private var monitorWrite = 0
    private var monitorSize = 0

    private var replay: ShortArray? = null
    private var replayOffset = 0

    private val playBlock = ShortArray(audio.playbackBufferFrames)
    private val playOut = ShortArray(audio.playbackBufferFrames)
    private var playCount = 0
    private var playOffset = 0

    /** `true` while the capture stream is requested and driven. */
    var recording: Boolean = false
        private set

    /** `true` while raw microphone audio is looped to the speakers. */
    val monitoring: Boolean get() = monitorEnabled
    private var monitorEnabled = false

    private var replaying = false

    /** `true` when a replay or monitoring needs the output stream. */
    private val needsOutput: Boolean get() = monitorEnabled || replaying

    /** Last capture/playback problem for the page's status line. */
    val status: String
        get() {
            val error = audio.captureError
            if (error != null) return "microphone unavailable: $error"
            val playback = audio.playbackError
            if (playback != null && needsOutput) return "playback unavailable: $playback"
            val state = when {
                !recording -> "paused"
                replaying -> "replaying"
                else -> "recording"
            }
            return "${audio.captureDeviceName} @ ${audio.format.sampleRate} Hz -> $targetRate Hz, $state"
        }

    /**
     * Sets the page rate and frame size. Safe to call from `recreate()`: an
     * active capture is restarted so the next pump uses the new frame size.
     */
    fun configure(rate: Int, frames: Int) {
        require(rate > 0) { "rate must be positive: $rate" }
        require(frames > 0) { "frames must be positive: $frames" }
        if (rate == targetRate && frames == frameSize && resamplerReady()) return

        val wasRecording = recording
        stop()
        targetRate = rate
        frameSize = frames
        resampler?.close()
        resampler = if (rate == audio.format.sampleRate) {
            null
        } else {
            SpeexResampler.create(
                channels = 1,
                inRate = audio.format.sampleRate,
                outRate = rate,
                quality = SpeexResampler.QUALITY_VOIP,
            )
        }
        pending = ShortArray(audio.captureBufferFrames + frames)
        pendingCount = 0
        playCount = 0
        playOffset = 0
        if (wasRecording) start()
    }

    private fun resamplerReady(): Boolean =
        targetRate == audio.format.sampleRate || resampler != null

    /** Requests the microphone and starts delivering frames. */
    fun start() {
        if (recording) {
            audio.requestCapture(true)
            return
        }
        recording = true
        replay = null
        replayOffset = 0
        replaying = false
        audio.requestCapture(true)
    }

    /** Stops capture, monitoring and replay; the recorded window is kept. */
    fun stop() {
        recording = false
        monitorEnabled = false
        replaying = false
        replay = null
        replayOffset = 0
        playCount = 0
        playOffset = 0
        pendingCount = 0
        monitorSize = 0
        monitorRead = 0
        monitorWrite = 0
        audio.requestCapture(false)
        capture?.let {
            audio.releaseCapture(it)
            capture = null
        }
        releaseOutput()
        audio.requestOutput(false)
    }

    /** Clears the capture window and all pending playback state. */
    fun reset() {
        stop()
        recordWrite = 0
        recordSize = 0
        captureBuffer.clear()
        playbackBuffer.clear()
    }

    /** Enables or disables the raw microphone monitor loop. */
    fun setMonitoring(enabled: Boolean) {
        if (enabled && !recording) start()
        monitorEnabled = enabled
        audio.requestOutput(enabled || replaying)
        if (!enabled && !replaying) {
            monitorSize = 0
            releaseOutput()
        }
    }

    /** Plays the newest [maxMillis] of captured microphone audio. */
    fun replayLast(maxMillis: Int = 2_000) {
        if (recordSize == 0) return
        val wanted = minOf(recordSize, audio.format.millisToFrames(maxMillis.toDouble()))
        val data = ShortArray(wanted)
        var index = recordWrite - wanted
        if (index < 0) index += record.size
        for (i in 0 until wanted) {
            data[i] = record[index]
            if (++index == record.size) index = 0
        }
        replay = data
        replayOffset = 0
        replaying = true
        playCount = 0
        playOffset = 0
        audio.requestOutput(true)
    }

    /**
     * Reads whatever the microphone has buffered and invokes [onFrame] for every
     * complete frame at [targetRate]. Also drains playback.
     */
    fun pump(onFrame: (ShortArray) -> Unit) {
        val input = capture ?: audio.takeCapture()?.also { capture = it }
        if (input != null && recording) {
            while (true) {
                val frames = input.readNonBlocking(captureBuffer)
                if (frames <= 0) break
                captureBuffer.toShorts(capturePcm, frames)
                appendRecord(capturePcm, frames)
                if (monitorEnabled) queueMonitor(capturePcm, frames)
                if (resampler == null) {
                    deliver(capturePcm, frames, onFrame)
                } else {
                    val resampled = resampler!!.resample(capturePcm.copyOf(frames))
                    deliver(resampled, resampled.size, onFrame)
                }
            }
        }
        pumpOutput()
    }

    override fun close() {
        stop()
        resampler?.close()
        resampler = null
    }

    private fun appendRecord(samples: ShortArray, count: Int) {
        var index = recordWrite
        var size = recordSize
        for (i in 0 until count) {
            record[index] = samples[i]
            if (++index == record.size) index = 0
            if (size < record.size) size++
        }
        recordWrite = index
        recordSize = size
    }

    private fun queueMonitor(samples: ShortArray, count: Int) {
        for (i in 0 until count) {
            if (monitorSize == monitorQueue.size) {
                // Keep monitoring close to live: drop the oldest sample.
                if (++monitorRead == monitorQueue.size) monitorRead = 0
                monitorSize--
            }
            monitorQueue[monitorWrite] = samples[i]
            if (++monitorWrite == monitorQueue.size) monitorWrite = 0
            monitorSize++
        }
    }

    private fun deliver(samples: ShortArray, count: Int, onFrame: (ShortArray) -> Unit) {
        var offset = 0
        while (offset < count) {
            val need = frameSize - pendingCount
            val take = minOf(need, count - offset)
            samples.copyInto(pending, pendingCount, offset, offset + take)
            pendingCount += take
            offset += take
            if (pendingCount == frameSize) {
                val frame = ShortArray(frameSize)
                pending.copyInto(frame, 0, 0, frameSize)
                pendingCount = 0
                onFrame(frame)
            }
        }
    }

    private fun pumpOutput() {
        if (!needsOutput) {
            releaseOutput()
            return
        }
        val stream = output ?: audio.takeOutput()?.also { output = it } ?: return

        while (true) {
            if (playCount == 0) {
                fillPlayBlock()
                if (playCount == 0) break
            }
            val count = playCount
            playBlock.copyInto(playOut, 0, playOffset, playOffset + count)
            playbackBuffer.putShorts(playOut, count)
            val written = stream.writeNonBlocking(playbackBuffer)
            if (written <= 0) break
            playOffset += written
            playCount -= written
            if (playCount == 0) playOffset = 0
        }

        if (!needsOutput) releaseOutput()
    }

    private fun fillPlayBlock() {
        val source = replay
        if (source != null) {
            if (replayOffset >= source.size) {
                replay = null
                replayOffset = 0
                replaying = false
                return
            }
            val count = minOf(playBlock.size, source.size - replayOffset)
            source.copyInto(playBlock, 0, replayOffset, replayOffset + count)
            replayOffset += count
            playCount = count
            playOffset = 0
            return
        }

        if (monitorSize == 0) return
        val count = minOf(playBlock.size, monitorSize)
        var index = monitorRead
        for (i in 0 until count) {
            playBlock[i] = monitorQueue[index]
            if (++index == monitorQueue.size) index = 0
        }
        monitorRead = index
        monitorSize -= count
        playCount = count
        playOffset = 0
    }

    private fun releaseOutput() {
        output?.let {
            audio.releaseOutput(it)
            output = null
        }
        playbackBuffer.clear()
        audio.requestOutput(false)
    }
}
