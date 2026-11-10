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
 * SpeexDSP speech preprocessor: noise suppression, automatic gain control,
 * voice activity detection and optional residual echo suppression.
 *
 * One state processes one channel; create one per channel. The frame size
 * should be 10-20 ms at [samplingRate], and it must match the echo canceller's
 * frame size when [setEchoState] is used.
 *
 * ```kotlin
 * SpeexPreprocessor.create(frameSize = 160, samplingRate = 8_000).use { preprocessor ->
 *     preprocessor.denoise = true
 *     preprocessor.agc = true
 *     val voice = preprocessor.process(pcm)   // pcm is modified in place
 * }
 * ```
 *
 * Instances are stateful and not thread-safe.
 */
class SpeexPreprocessor private constructor(
    private val handle: SpeexPreprocessorHandle,
    /** Samples consumed per [process] call. */
    val frameSize: Int,
    /** Sampling rate used to configure the filters. */
    val samplingRate: Int,
) : AutoCloseable {
    private var closed = false

    var denoise: Boolean
        get() = getInt(SpeexPreprocessRequest.GET_DENOISE) != 0
        set(value) = setInt(SpeexPreprocessRequest.SET_DENOISE, if (value) 1 else 0)

    var agc: Boolean
        get() = getInt(SpeexPreprocessRequest.GET_AGC) != 0
        set(value) = setInt(SpeexPreprocessRequest.SET_AGC, if (value) 1 else 0)

    var vad: Boolean
        get() = getInt(SpeexPreprocessRequest.GET_VAD) != 0
        set(value) = setInt(SpeexPreprocessRequest.SET_VAD, if (value) 1 else 0)

    var dereverb: Boolean
        get() = getInt(SpeexPreprocessRequest.GET_DEREVERB) != 0
        set(value) = setInt(SpeexPreprocessRequest.SET_DEREVERB, if (value) 1 else 0)

    /** AGC target level (float request). */
    var agcLevel: Float
        get() = SpeexDspNative.preprocessGetFloat(openHandle(), SpeexPreprocessFloatRequest.GET_AGC_LEVEL)
        set(value) {
            val result = SpeexDspNative.preprocessCtlFloat(
                openHandle(),
                SpeexPreprocessFloatRequest.SET_AGC_LEVEL,
                value,
            )
            checkResult(result, "AGC level")
        }

    /** Maximum gain increase in dB/s. */
    var agcIncrement: Int
        get() = getInt(SpeexPreprocessRequest.GET_AGC_INCREMENT)
        set(value) = setInt(SpeexPreprocessRequest.SET_AGC_INCREMENT, value)

    /** Maximum gain decrease in dB/s. */
    var agcDecrement: Int
        get() = getInt(SpeexPreprocessRequest.GET_AGC_DECREMENT)
        set(value) = setInt(SpeexPreprocessRequest.SET_AGC_DECREMENT, value)

    /** Maximum gain in dB. */
    var agcMaxGain: Int
        get() = getInt(SpeexPreprocessRequest.GET_AGC_MAX_GAIN)
        set(value) = setInt(SpeexPreprocessRequest.SET_AGC_MAX_GAIN, value)

    /** Integer AGC target level. */
    var agcTarget: Int
        get() = getInt(SpeexPreprocessRequest.GET_AGC_TARGET)
        set(value) = setInt(SpeexPreprocessRequest.SET_AGC_TARGET, value)

    /** Probability (0-100) required to enter the voice state. */
    var probStart: Int
        get() = getInt(SpeexPreprocessRequest.GET_PROB_START)
        set(value) {
            require(value in 0..100) { "probStart must be 0..100, got $value" }
            setInt(SpeexPreprocessRequest.SET_PROB_START, value)
        }

    /** Probability (0-100) required to stay in the voice state. */
    var probContinue: Int
        get() = getInt(SpeexPreprocessRequest.GET_PROB_CONTINUE)
        set(value) {
            require(value in 0..100) { "probContinue must be 0..100, got $value" }
            setInt(SpeexPreprocessRequest.SET_PROB_CONTINUE, value)
        }

    /** Maximum noise attenuation in dB (negative). */
    var noiseSuppress: Int
        get() = getInt(SpeexPreprocessRequest.GET_NOISE_SUPPRESS)
        set(value) = setInt(SpeexPreprocessRequest.SET_NOISE_SUPPRESS, value)

    /** Maximum residual echo attenuation in dB (negative). */
    var echoSuppress: Int
        get() = getInt(SpeexPreprocessRequest.GET_ECHO_SUPPRESS)
        set(value) = setInt(SpeexPreprocessRequest.SET_ECHO_SUPPRESS, value)

    /** Maximum residual echo attenuation while near-end is active, in dB (negative). */
    var echoSuppressActive: Int
        get() = getInt(SpeexPreprocessRequest.GET_ECHO_SUPPRESS_ACTIVE)
        set(value) = setInt(SpeexPreprocessRequest.SET_ECHO_SUPPRESS_ACTIVE, value)

    /** AGC loudness estimate of the last frame. */
    val agcLoudness: Int get() = getInt(SpeexPreprocessRequest.GET_AGC_LOUDNESS)

    /** Current AGC gain in dB. */
    val agcGain: Int get() = getInt(SpeexPreprocessRequest.GET_AGC_GAIN)

    /** Speech probability of the last frame, 0-100. */
    val speechProbability: Int get() = getInt(SpeexPreprocessRequest.GET_PROB)

    /** Number of bins in [powerSpectrum]. */
    val psdSize: Int get() = SpeexDspNative.preprocessPsdSize(openHandle())

    /** Number of bins in [noisePowerSpectrum]. */
    val noisePsdSize: Int get() = SpeexDspNative.preprocessNoisePsdSize(openHandle())

    /**
     * Processes one frame of [frameSize] samples starting at [offset] in place.
     * Returns the VAD decision: `true` for speech, `false` for noise/silence.
     */
    fun process(samples: ShortArray, offset: Int = 0): Boolean {
        val handle = openHandle()
        require(offset >= 0 && offset <= samples.size - frameSize) {
            "need $frameSize samples at offset $offset, got ${samples.size}"
        }
        val result = SpeexDspNative.preprocessRun(handle, samples, offset, frameSize)
        if (result < 0) throw SpeexException("SpeexDSP preprocessing failed (code $result)")
        return result != 0
    }

    /**
     * Updates the noise estimate from a frame without producing output. Use it
     * while the far end is silent and the near end is only background noise.
     */
    fun estimateUpdate(samples: ShortArray, offset: Int = 0) {
        val handle = openHandle()
        require(offset >= 0 && offset <= samples.size - frameSize) {
            "need $frameSize samples at offset $offset, got ${samples.size}"
        }
        SpeexDspNative.preprocessEstimateUpdate(handle, samples, offset, frameSize)
    }

    /**
     * Attaches the echo canceller whose residual echo should be suppressed, or
     * `null` to detach it. The canceller must outlive this preprocessor while
     * attached.
     */
    fun setEchoState(echo: SpeexEchoCanceller?) {
        val result = SpeexDspNative.preprocessSetEcho(openHandle(), echo?.requireHandle())
        if (result < 0) throw SpeexException("SpeexDSP rejected the echo state (code $result)")
    }

    /** Power spectrum of the last processed frame. */
    fun powerSpectrum(): IntArray = SpeexDspNative.preprocessPsd(openHandle()) ?: IntArray(0)

    /** Noise power-spectrum estimate of the last processed frame. */
    fun noisePowerSpectrum(): IntArray =
        SpeexDspNative.preprocessNoisePsd(openHandle()) ?: IntArray(0)

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.preprocessFree(handle)
    }

    override fun toString(): String =
        "SpeexPreprocessor(frameSize=$frameSize, samplingRate=$samplingRate, denoise=$denoise, agc=$agc)"

    private fun openHandle(): SpeexPreprocessorHandle {
        check(!closed) { "SpeexPreprocessor is closed" }
        return handle
    }

    private fun getInt(request: Int): Int {
        val value = SpeexDspNative.preprocessGetInt(openHandle(), request)
        if (value == XDSP_INVALID) throw SpeexException("SpeexDSP rejected preprocessor request $request")
        return value
    }

    private fun setInt(request: Int, value: Int) {
        checkResult(SpeexDspNative.preprocessCtlInt(openHandle(), request, value), "request $request")
    }

    private fun checkResult(result: Int, what: String) {
        if (result < 0) throw SpeexException("SpeexDSP rejected preprocessor $what (code $result)")
    }

    companion object {
        /** Creates a preprocessor for one channel. */
        fun create(frameSize: Int, samplingRate: Int): SpeexPreprocessor {
            require(frameSize > 0) { "frameSize must be positive, got $frameSize" }
            require(samplingRate > 0) { "samplingRate must be positive, got $samplingRate" }
            val handle = SpeexDspNative.preprocessCreate(frameSize, samplingRate)
                ?: throw SpeexException("SpeexDSP could not allocate a preprocessor")
            return SpeexPreprocessor(handle, frameSize, samplingRate)
        }
    }
}
