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
 * SpeexDSP sample-rate converter for 16-bit PCM.
 *
 * ```kotlin
 * SpeexResampler.create(channels = 1, inRate = 16_000, outRate = 8_000).use { resampler ->
 *     val output = resampler.resample(pcm16k)   // interleaved 16-bit PCM
 * }
 * ```
 *
 * The input length is per channel, so a multi-channel buffer must contain
 * exactly `channels * n` samples. The first calls may return fewer samples
 * than the rate ratio suggests: the filter has latency and keeps internal
 * state between calls. Feed consecutive frames to the same instance.
 *
 * Instances are stateful and not thread-safe.
 */
class SpeexResampler private constructor(
    private val handle: SpeexResamplerHandle,
    /** Number of interleaved channels. */
    val channels: Int,
) : AutoCloseable {
    private var closed = false

    val inputRate: Int get() = SpeexDspNative.resamplerInputRate(openHandle())

    val outputRate: Int get() = SpeexDspNative.resamplerOutputRate(openHandle())

    /** Numerator of the reduced resampling ratio. */
    val ratioNum: Int get() = SpeexDspNative.resamplerRatioNum(openHandle())

    /** Denominator of the reduced resampling ratio. */
    val ratioDen: Int get() = SpeexDspNative.resamplerRatioDen(openHandle())

    /** Resampling quality, 0 (fastest) to 10 (best). */
    var quality: Int
        get() = SpeexDspNative.resamplerQuality(openHandle())
        set(value) {
            require(value in QUALITY_MIN..QUALITY_MAX) { "quality must be $QUALITY_MIN..$QUALITY_MAX, got $value" }
            checkResampler(SpeexDspNative.resamplerSetQuality(openHandle(), value))
        }

    /** Input stride in samples between channels. */
    var inputStride: Int
        get() = SpeexDspNative.resamplerInputStride(openHandle())
        set(value) {
            require(value > 0) { "inputStride must be positive, got $value" }
            SpeexDspNative.resamplerSetInputStride(openHandle(), value)
        }

    /** Output stride in samples between channels. */
    var outputStride: Int
        get() = SpeexDspNative.resamplerOutputStride(openHandle())
        set(value) {
            require(value > 0) { "outputStride must be positive, got $value" }
            SpeexDspNative.resamplerSetOutputStride(openHandle(), value)
        }

    /** Filter latency in input samples. */
    val inputLatency: Int get() = SpeexDspNative.resamplerInputLatency(openHandle())

    /** Filter latency in output samples. */
    val outputLatency: Int get() = SpeexDspNative.resamplerOutputLatency(openHandle())

    /** Changes the integer rate pair; the internal state is preserved. */
    fun setRate(inRate: Int, outRate: Int) {
        require(inRate > 0 && outRate > 0) { "rates must be positive, got $inRate -> $outRate" }
        checkResampler(SpeexDspNative.resamplerSetRate(openHandle(), inRate, outRate))
    }

    /** Changes the rate pair and an explicit rational ratio. */
    fun setRateFractional(ratioNum: Int, ratioDen: Int, inRate: Int, outRate: Int) {
        require(ratioNum > 0 && ratioDen > 0) { "ratio must be positive, got $ratioNum/$ratioDen" }
        checkResampler(
            SpeexDspNative.resamplerSetRateFrac(openHandle(), ratioNum, ratioDen, inRate, outRate),
        )
    }

    /** Drops the leading zeros of a newly created resampler. */
    fun skipZeros() {
        checkResampler(SpeexDspNative.resamplerSkipZeros(openHandle()))
    }

    /** Resets the filter memory so a new, unrelated stream can be processed. */
    fun reset() {
        checkResampler(SpeexDspNative.resamplerResetMem(openHandle()))
    }

    /**
     * Resamples all complete frames in [samples] starting at [offset].
     *
     * The remaining length must be a multiple of [channels]. Returns the
     * produced interleaved samples, which may be empty while the filter fills.
     */
    fun resample(samples: ShortArray, offset: Int = 0): ShortArray {
        val handle = openHandle()
        require(offset >= 0 && offset <= samples.size) {
            "offset $offset is outside a ${samples.size}-sample buffer"
        }
        val available = samples.size - offset
        require(available % channels == 0) {
            "buffer has $available samples, not a multiple of $channels channels"
        }
        val perChannel = available / channels
        val result = SpeexDspNative.resamplerProcess(handle, samples, offset, perChannel)
        checkResampler(result)
        return SpeexDspNative.resamplerOutput(handle) ?: ShortArray(0)
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.resamplerFree(handle)
    }

    override fun toString(): String =
        "SpeexResampler(${inputRate}Hz -> ${outputRate}Hz, channels=$channels, quality=$quality)"

    private fun openHandle(): SpeexResamplerHandle {
        check(!closed) { "SpeexResampler is closed" }
        return handle
    }

    private fun checkResampler(code: Int) {
        if (code != XDSP_RESAMPLER_OK) {
            throw SpeexException("SpeexDSP resampler failed (code $code): ${SpeexDsp.resamplerErrorString(code)}")
        }
    }

    companion object {
        const val QUALITY_MIN: Int = 0
        const val QUALITY_MAX: Int = 10

        /** SpeexDSP's default quality. */
        const val QUALITY_DEFAULT: Int = 4

        /** Quality recommended for VoIP. */
        const val QUALITY_VOIP: Int = 3

        /** Quality recommended for desktop audio. */
        const val QUALITY_DESKTOP: Int = 5

        /** Creates a resampler for integer input and output rates. */
        fun create(
            channels: Int,
            inRate: Int,
            outRate: Int,
            quality: Int = QUALITY_DEFAULT,
        ): SpeexResampler {
            require(channels > 0) { "channels must be positive, got $channels" }
            require(inRate > 0 && outRate > 0) { "rates must be positive, got $inRate -> $outRate" }
            require(quality in QUALITY_MIN..QUALITY_MAX) { "quality must be $QUALITY_MIN..$QUALITY_MAX, got $quality" }
            val handle = SpeexDspNative.resamplerCreate(channels, inRate, outRate, quality)
                ?: throw SpeexException("SpeexDSP could not allocate a resampler")
            return validate(handle, channels)
        }

        /** Creates a resampler with an explicit rational rate ratio. */
        fun createFractional(
            channels: Int,
            ratioNum: Int,
            ratioDen: Int,
            inRate: Int,
            outRate: Int,
            quality: Int = QUALITY_DEFAULT,
        ): SpeexResampler {
            require(channels > 0) { "channels must be positive, got $channels" }
            require(ratioNum > 0 && ratioDen > 0) { "ratio must be positive, got $ratioNum/$ratioDen" }
            require(quality in QUALITY_MIN..QUALITY_MAX) { "quality must be $QUALITY_MIN..$QUALITY_MAX, got $quality" }
            val handle = SpeexDspNative.resamplerCreateFrac(
                channels,
                ratioNum,
                ratioDen,
                inRate,
                outRate,
                quality,
            ) ?: throw SpeexException("SpeexDSP could not allocate a resampler")
            return validate(handle, channels)
        }

        private fun validate(handle: SpeexResamplerHandle, channels: Int): SpeexResampler {
            val error = SpeexDspNative.resamplerError(handle)
            if (error != XDSP_RESAMPLER_OK) {
                SpeexDspNative.resamplerFree(handle)
                throw SpeexException(
                    "SpeexDSP rejected the resampler (code $error): ${SpeexDsp.resamplerErrorString(error)}",
                )
            }
            return SpeexResampler(handle, channels)
        }
    }
}
