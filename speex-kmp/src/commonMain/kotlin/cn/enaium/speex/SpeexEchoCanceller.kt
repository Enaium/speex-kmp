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

package cn.enaium.speex

/**
 * SpeexDSP acoustic echo canceller.
 *
 * Feed the microphone signal and the signal that was played to the speaker;
 * [cancellation] returns the near-end signal with the echo removed. All
 * buffers are 16-bit PCM and hold `frameSize * microphones` microphone
 * samples or `frameSize * speakers` playback samples.
 *
 * ```kotlin
 * SpeexEchoCanceller.create(frameSize = 160, filterLength = 800).use { canceller ->
 *     val clean = canceller.cancellation(microphone, speaker)
 * }
 * ```
 *
 * The canceller is stateful and not thread-safe.
 */
class SpeexEchoCanceller private constructor(
    private val handle: SpeexEchoHandle,
    /** Samples per microphone frame. */
    val frameSize: Int,
    /** Number of microphone channels. */
    val microphones: Int,
    /** Number of speaker channels. */
    val speakers: Int,
) : AutoCloseable {
    private var closed = false

    /** Sampling rate used by the adaptive filter. */
    var samplingRate: Int
        get() = getInt(SpeexEchoRequest.GET_SAMPLING_RATE)
        set(value) {
            require(value > 0) { "samplingRate must be positive, got $value" }
            val result = SpeexDspNative.echoCtlInt(openHandle(), SpeexEchoRequest.SET_SAMPLING_RATE, value)
            if (result < 0) throw SpeexException("SpeexDSP rejected sampling rate $value (code $result)")
        }

    /** Number of coefficients in [impulseResponse]. */
    val impulseResponseSize: Int get() = SpeexDspNative.echoImpulseResponseSize(openHandle())

    /**
     * Cancels the echo in [rec] using [play].
     *
     * [rec] must contain at least `frameSize * microphones` samples at
     * [recOffset]; [play] must contain at least `frameSize * speakers` samples
     * at [playOffset].
     */
    fun cancellation(
        rec: ShortArray,
        play: ShortArray,
        recOffset: Int = 0,
        playOffset: Int = 0,
    ): ShortArray {
        val handle = openHandle()
        require(recOffset >= 0 && recOffset <= rec.size - frameSize * microphones) {
            "need ${frameSize * microphones} microphone samples at offset $recOffset, got ${rec.size}"
        }
        require(playOffset >= 0 && playOffset <= play.size - frameSize * speakers) {
            "need ${frameSize * speakers} speaker samples at offset $playOffset, got ${play.size}"
        }
        val result = SpeexDspNative.echoCancellation(handle, rec, recOffset, play, playOffset)
        if (result < 0) throw SpeexException("SpeexDSP echo cancellation failed (code $result)")
        return SpeexDspNative.echoOutput(handle) ?: ShortArray(0)
    }

    /**
     * Capture half of the split API: processes a microphone frame before the
     * matching [playback] call supplies the far-end frame.
     */
    fun capture(rec: ShortArray, offset: Int = 0): ShortArray {
        val handle = openHandle()
        require(offset >= 0 && offset <= rec.size - frameSize * microphones) {
            "need ${frameSize * microphones} microphone samples at offset $offset, got ${rec.size}"
        }
        val result = SpeexDspNative.echoCapture(handle, rec, offset)
        if (result < 0) throw SpeexException("SpeexDSP echo capture failed (code $result)")
        return SpeexDspNative.echoOutput(handle) ?: ShortArray(0)
    }

    /** Playback half of the split API. */
    fun playback(play: ShortArray, offset: Int = 0) {
        val handle = openHandle()
        require(offset >= 0 && offset <= play.size - frameSize * speakers) {
            "need ${frameSize * speakers} speaker samples at offset $offset, got ${play.size}"
        }
        val result = SpeexDspNative.echoPlayback(handle, play, offset)
        if (result < 0) throw SpeexException("SpeexDSP echo playback failed (code $result)")
    }

    /** Current adaptive-filter impulse response. */
    fun impulseResponse(): IntArray = SpeexDspNative.echoImpulseResponse(openHandle()) ?: IntArray(0)

    /** Resets the adaptive filter and internal buffers. */
    fun reset() {
        SpeexDspNative.echoReset(openHandle())
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.echoFree(handle)
    }

    override fun toString(): String =
        "SpeexEchoCanceller(frameSize=$frameSize, microphones=$microphones, speakers=$speakers)"

    internal fun requireHandle(): SpeexEchoHandle = openHandle()

    private fun openHandle(): SpeexEchoHandle {
        check(!closed) { "SpeexEchoCanceller is closed" }
        return handle
    }

    private fun getInt(request: Int): Int {
        val value = SpeexDspNative.echoGetInt(openHandle(), request)
        if (value == XDSP_INVALID) throw SpeexException("SpeexDSP rejected echo request $request")
        return value
    }

    companion object {
        /**
         * Creates an echo canceller.
         *
         * [filterLength] is the number of samples of echo to cancel, usually
         * 100-500 ms (800-4000 samples at 8 kHz).
         */
        fun create(
            frameSize: Int,
            filterLength: Int,
            microphones: Int = 1,
            speakers: Int = 1,
        ): SpeexEchoCanceller {
            require(frameSize > 0) { "frameSize must be positive, got $frameSize" }
            require(filterLength > 0) { "filterLength must be positive, got $filterLength" }
            require(microphones > 0) { "microphones must be positive, got $microphones" }
            require(speakers > 0) { "speakers must be positive, got $speakers" }
            val handle = SpeexDspNative.echoCreate(frameSize, filterLength, microphones, speakers)
                ?: throw SpeexException("SpeexDSP could not allocate an echo canceller")
            return SpeexEchoCanceller(handle, frameSize, microphones, speakers)
        }
    }
}
