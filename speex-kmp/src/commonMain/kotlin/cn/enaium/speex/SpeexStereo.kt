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
 * Speex intensity-stereo state.
 *
 * Speex encodes stereo as a mono stream plus one in-band stereo request per
 * frame. [SpeexEncoder.encodeStereo] performs the encoder half and carries the
 * request inside its packet; [SpeexDecoder.decodeStereo] needs a state object
 * that persists for the whole stream.
 *
 * The low-level [encode]/[decode] pair is for callers that build their own
 * framing with [SpeexBits]. Instances are not thread-safe.
 */
class SpeexStereo : AutoCloseable {
    internal val handle: SpeexStereoHandle = SpeexNative.stereoCreate()
        ?: throw SpeexException("Speex could not allocate a stereo state")

    private var closed = false

    /**
     * Folds [frameSize] interleaved stereo samples starting at [offset] to mono
     * in place and appends the intensity-stereo request to [bits].
     */
    fun encode(interleaved: ShortArray, frameSize: Int, bits: SpeexBits, offset: Int = 0) {
        val handle = openHandle()
        require(frameSize > 0) { "frameSize must be positive, got $frameSize" }
        val needed = frameSize * 2
        require(offset >= 0 && offset <= interleaved.size - needed) {
            "need $needed interleaved samples at offset $offset, got ${interleaved.size}"
        }
        val result = SpeexNative.stereoEncode(handle, interleaved, offset, needed, bits.requireHandle())
        if (result < 0) throw SpeexException("Speex rejected stereo encoding (code $result)")
    }

    /**
     * Expands the mono frame in the first [frameSize] samples of [interleaved]
     * to `2 * frameSize` interleaved stereo samples in place.
     */
    fun decode(interleaved: ShortArray, frameSize: Int) {
        val handle = openHandle()
        require(frameSize > 0) { "frameSize must be positive, got $frameSize" }
        val needed = frameSize * 2
        require(interleaved.size >= needed) {
            "need $needed interleaved samples, got ${interleaved.size}"
        }
        val result = SpeexNative.stereoDecode(handle, interleaved, needed)
        if (result < 0) throw SpeexException("Speex rejected stereo decoding (code $result)")
    }

    /** Resets the smoothed left/right gains to their initial values. */
    fun reset() {
        SpeexNative.stereoReset(openHandle())
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexNative.stereoFree(handle)
    }

    override fun toString(): String = if (closed) "SpeexStereo(closed)" else "SpeexStereo"

    /** The native handle, for objects that hand it to another shim call. */
    internal fun requireHandle(): SpeexStereoHandle = openHandle()

    private fun openHandle(): SpeexStereoHandle {
        check(!closed) { "SpeexStereo is closed" }
        return handle
    }
}
