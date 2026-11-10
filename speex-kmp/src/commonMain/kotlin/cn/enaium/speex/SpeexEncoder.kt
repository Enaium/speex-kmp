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
 * A stateful Speex encoder.
 *
 * Input is 16-bit linear PCM with a +-32768 range. One call consumes exactly
 * [frameSize] samples (160, 320 or 640 depending on the mode) and produces one
 * packet, except when DTX drops the frame.
 *
 * ```kotlin
 * SpeexEncoder.open(SpeexMode.WIDEBAND).use { encoder ->
 *     encoder.quality = 8
 *     encoder.vbr = true
 *     val packet = encoder.encode(pcm, offset) ?: return  // DTX: nothing to send
 *     sink.write(packet)
 * }
 * ```
 *
 * The encoder is not thread-safe: encode from one thread at a time.
 */
class SpeexEncoder private constructor(
    private val handle: SpeexEncoderHandle,
    /** The mode this encoder was created with. */
    val mode: SpeexMode,
) : AutoCloseable {
    private var closed = false

    /** Samples consumed per [encode] call. */
    val frameSize: Int = SpeexNative.encoderFrameSize(handle)

    /** Encoder lookahead in samples (`SPEEX_GET_LOOKAHEAD`). */
    val lookahead: Int get() = get(SpeexCtl.GET_LOOKAHEAD)

    /**
     * Encoder quality, 0-10. Speex has no getter for this request, so the
     * property reports the last value assigned (8 until changed) even if
     * [bitrate] or [abr] later adjusted the internal sub-mode.
     */
    var quality: Int = 8
        set(value) {
            require(value in 0..10) { "quality must be 0..10, got $value" }
            ctl(SpeexCtl.SET_QUALITY, value)
            field = value
        }

    /** Encoder complexity, 0-10; Speex's default is 2. */
    var complexity: Int
        get() = get(SpeexCtl.GET_COMPLEXITY)
        set(value) {
            require(value >= 0) { "complexity must not be negative, got $value" }
            ctl(SpeexCtl.SET_COMPLEXITY, value)
        }

    /** Target bit-rate in bit/s; Speex selects the highest sub-mode that fits. */
    var bitrate: Int
        get() = get(SpeexCtl.GET_BITRATE)
        set(value) {
            require(value > 0) { "bitrate must be positive, got $value" }
            ctl(SpeexCtl.SET_BITRATE, value)
        }

    /** Variable bit-rate mode. */
    var vbr: Boolean
        get() = get(SpeexCtl.GET_VBR) != 0
        set(value) = ctl(SpeexCtl.SET_VBR, if (value) 1 else 0)

    /** VBR quality target, 0-10 (`SPEEX_SET_VBR_QUALITY`, a float request). */
    var vbrQuality: Float
        get() = SpeexNative.encoderGetVbrQuality(openHandle())
        set(value) {
            require(value in 0f..10f) { "vbrQuality must be 0..10, got $value" }
            val result = SpeexNative.encoderSetVbrQuality(openHandle(), value)
            if (result < 0) throw SpeexException("Speex rejected vbrQuality $value (code $result)")
        }

    /**
     * Average bit-rate target. A non-zero value enables VBR and drives it to
     * that rate; 0 disables ABR.
     */
    var abr: Int
        get() = get(SpeexCtl.GET_ABR)
        set(value) {
            require(value >= 0) { "abr must not be negative, got $value" }
            ctl(SpeexCtl.SET_ABR, value)
        }

    /** Voice activity detection. */
    var vad: Boolean
        get() = get(SpeexCtl.GET_VAD) != 0
        set(value) = ctl(SpeexCtl.SET_VAD, if (value) 1 else 0)

    /** Discontinuous transmission: silent frames make [encode] return `null`. */
    var dtx: Boolean
        get() = get(SpeexCtl.GET_DTX) != 0
        set(value) = ctl(SpeexCtl.SET_DTX, if (value) 1 else 0)

    /** Input high-pass filter; on by default. */
    var highpass: Boolean
        get() = get(SpeexCtl.GET_HIGHPASS) != 0
        set(value) = ctl(SpeexCtl.SET_HIGHPASS, if (value) 1 else 0)

    /** Sampling rate used for bit-rate computation, 8000-32000 Hz. */
    var samplingRate: Int
        get() = get(SpeexCtl.GET_SAMPLING_RATE)
        set(value) {
            require(value > 0) { "samplingRate must be positive, got $value" }
            ctl(SpeexCtl.SET_SAMPLING_RATE, value)
        }

    /** Low-band sub-mode (wideband and ultra-wideband; aliases the mode in narrowband). */
    var lowMode: Int
        get() = get(SpeexCtl.GET_LOW_MODE)
        set(value) {
            require(value >= 0) { "lowMode must not be negative, got $value" }
            ctl(SpeexCtl.SET_LOW_MODE, value)
        }

    /**
     * Selects the sub-mode directly. In narrowband this is the only sub-mode;
     * in wideband it is equivalent to setting a quality, and the high-band
     * sub-mode is chosen to match.
     */
    fun setSubMode(value: Int): SpeexEncoder {
        require(value >= 0) { "subMode must not be negative, got $value" }
        ctl(SpeexCtl.SET_MODE, value)
        return this
    }

    /** Selects the high-band sub-mode (wideband and ultra-wideband only). */
    fun setHighMode(value: Int): SpeexEncoder {
        require(value >= 0) { "highMode must not be negative, got $value" }
        if (mode == SpeexMode.NARROWBAND) {
            throw SpeexException("narrowband has no high-band sub-mode")
        }
        ctl(SpeexCtl.SET_HIGH_MODE, value)
        return this
    }

    /** Packet-loss-concealment tuning, 0-100; Speex's default is 2. */
    var plcTuning: Int
        get() = get(SpeexCtl.GET_PLC_TUNING)
        set(value) {
            require(value in 0..100) { "plcTuning must be 0..100, got $value" }
            ctl(SpeexCtl.SET_PLC_TUNING, value)
        }

    /** Maximum bit-rate in VBR mode; 0 means unlimited. */
    var vbrMaxBitrate: Int
        get() = get(SpeexCtl.GET_VBR_MAX_BITRATE)
        set(value) {
            require(value >= 0) { "vbrMaxBitrate must not be negative, got $value" }
            ctl(SpeexCtl.SET_VBR_MAX_BITRATE, value)
        }

    /** Writes the sub-mode number into every frame; on by default. */
    var submodeEncoding: Boolean
        get() = get(SpeexCtl.GET_SUBMODE_ENCODING) != 0
        set(value) = ctl(SpeexCtl.SET_SUBMODE_ENCODING, if (value) 1 else 0)

    /** Quality of the last encoded frame, 0-10 (VBR analysis output). */
    val relativeQuality: Float get() = SpeexNative.encoderGetRelativeQuality(openHandle())

    /**
     * Encodes [frameSize] samples starting at [offset].
     *
     * Returns the packet, or `null` when DTX dropped the frame. The input may
     * be overwritten by Speex.
     */
    fun encode(samples: ShortArray, offset: Int = 0): ByteArray? {
        val handle = openHandle()
        require(offset >= 0 && offset <= samples.size - frameSize) {
            "need $frameSize samples at offset $offset, got ${samples.size}"
        }
        return when (val result = SpeexNative.encoderEncode(handle, samples, offset, frameSize)) {
            XK_PACKET -> SpeexNative.encoderPacket(handle)
                ?: throw SpeexException("Speex produced a packet but no payload")
            XK_NO_PACKET -> null
            else -> throw SpeexException("Speex failed to encode (code $result)")
        }
    }

    /**
     * Encodes one interleaved stereo frame of `2 * frameSize` samples starting
     * at [offset]. The first `frameSize` samples are folded to mono in place.
     *
     * Forward the resulting packet together with a [SpeexDecoder.decodeStereo]
     * call on the receiving side.
     */
    fun encodeStereo(interleaved: ShortArray, offset: Int = 0): ByteArray? {
        val handle = openHandle()
        val needed = frameSize * 2
        require(offset >= 0 && offset <= interleaved.size - needed) {
            "need $needed interleaved samples at offset $offset, got ${interleaved.size}"
        }
        return when (val result = SpeexNative.encoderEncodeStereo(handle, interleaved, offset, needed)) {
            XK_PACKET -> SpeexNative.encoderPacket(handle)
                ?: throw SpeexException("Speex produced a packet but no payload")
            XK_NO_PACKET -> null
            else -> throw SpeexException("Speex failed to encode stereo (code $result)")
        }
    }

    /** Zeroes the encoder memory without changing its configuration. */
    fun reset() {
        ctl(SpeexCtl.RESET_STATE, 0)
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexNative.encoderFree(handle)
    }

    override fun toString(): String =
        "SpeexEncoder(${mode.name.lowercase()}, frameSize=$frameSize, quality=$quality, bitrate=$bitrate)"

    private fun ctl(request: Int, value: Int) {
        val handle = openHandle()
        val result = SpeexNative.encoderCtl(handle, request, value)
        if (result < 0) throw SpeexException("Speex rejected encoder request $request=$value (code $result)")
    }

    private fun get(request: Int): Int {
        val handle = openHandle()
        val value = SpeexNative.encoderGet(handle, request)
        if (value == XK_INVALID) throw SpeexException("Speex rejected encoder request $request")
        return value
    }

    private fun openHandle(): SpeexEncoderHandle {
        check(!closed) { "SpeexEncoder is closed" }
        return handle
    }

    companion object {
        /** Creates an encoder for [mode] with Speex's defaults. */
        fun open(mode: SpeexMode = SpeexMode.NARROWBAND): SpeexEncoder {
            val handle = SpeexNative.encoderCreate(mode.id)
                ?: throw SpeexException("Speex failed to create a ${mode.name.lowercase()} encoder")
            return SpeexEncoder(handle, mode)
        }
    }
}
