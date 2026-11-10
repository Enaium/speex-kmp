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
 * SpeexDSP channel decorrelator for multi-channel echo cancellation.
 *
 * It removes correlation between channels by modifying the phase and adding a
 * small amount of noise. Input and output are interleaved 16-bit PCM with
 * `channels * frameSize` samples.
 */
class SpeexDecorrelator private constructor(
    private val handle: SpeexDecorrelatorHandle,
    /** Number of interleaved channels. */
    val channels: Int,
    /** Samples per channel and frame. */
    val frameSize: Int,
) : AutoCloseable {
    private var closed = false

    /**
     * Decorrelates one frame. [strength] is 0-100 and controls how much the
     * signal is altered.
     */
    fun process(input: ShortArray, strength: Int = 100, offset: Int = 0): ShortArray {
        val handle = openHandle()
        require(strength in 0..100) { "strength must be 0..100, got $strength" }
        val needed = channels * frameSize
        require(offset >= 0 && offset <= input.size - needed) {
            "need $needed samples at offset $offset, got ${input.size}"
        }
        val result = SpeexDspNative.decorrProcess(handle, input, offset, needed, strength)
        if (result < 0) throw SpeexException("SpeexDSP decorrelation failed (code $result)")
        return SpeexDspNative.decorrOutput(handle) ?: ShortArray(0)
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.decorrFree(handle)
    }

    override fun toString(): String = "SpeexDecorrelator(channels=$channels, frameSize=$frameSize)"

    private fun openHandle(): SpeexDecorrelatorHandle {
        check(!closed) { "SpeexDecorrelator is closed" }
        return handle
    }

    companion object {
        /** Creates a decorrelator for interleaved [channels]-channel audio. */
        fun create(rate: Int, channels: Int, frameSize: Int): SpeexDecorrelator {
            require(rate > 0) { "rate must be positive, got $rate" }
            require(channels > 0) { "channels must be positive, got $channels" }
            require(frameSize > 0) { "frameSize must be positive, got $frameSize" }
            val handle = SpeexDspNative.decorrCreate(rate, channels, frameSize)
                ?: throw SpeexException("SpeexDSP could not allocate a decorrelator")
            return SpeexDecorrelator(handle, channels, frameSize)
        }
    }
}
