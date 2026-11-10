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
 * A stateful Speex decoder.
 *
 * [decode] returns [frameSize] samples of 16-bit PCM per packet. Passing
 * `null` (or an empty packet) runs packet-loss concealment instead of consuming
 * a packet, which is what a jitter buffer should do for a lost frame.
 *
 * ```kotlin
 * SpeexDecoder.open(SpeexMode.WIDEBAND).use { decoder ->
 *     val pcm = if (packet != null) decoder.decode(packet) else decoder.decode(null)
 *     speaker.write(pcm)
 * }
 * ```
 *
 * The decoder is not thread-safe: decode from one thread at a time, and pass
 * one [SpeexStereo] state to every [decodeStereo] call of a stream.
 */
class SpeexDecoder private constructor(
    private val handle: SpeexDecoderHandle,
    /** The mode this decoder was created with. */
    val mode: SpeexMode,
) : AutoCloseable {
    private var closed = false

    /** Samples produced per [decode] call. */
    val frameSize: Int = SpeexNative.decoderFrameSize(handle)

    /** Total codec lookahead in samples; add the encoder's value for end-to-end delay. */
    val lookahead: Int get() = get(SpeexCtl.GET_LOOKAHEAD)

    /** Perceptual enhancement, on by default. */
    var enhancement: Boolean
        get() = get(SpeexCtl.GET_ENH) != 0
        set(value) = ctl(SpeexCtl.SET_ENH, if (value) 1 else 0)

    /** Sampling rate used for bit-rate computation, 8000-32000 Hz. */
    var samplingRate: Int
        get() = get(SpeexCtl.GET_SAMPLING_RATE)
        set(value) {
            require(value > 0) { "samplingRate must be positive, got $value" }
            ctl(SpeexCtl.SET_SAMPLING_RATE, value)
        }

    /** Output high-pass filter. */
    var highpass: Boolean
        get() = get(SpeexCtl.GET_HIGHPASS) != 0
        set(value) = ctl(SpeexCtl.SET_HIGHPASS, if (value) 1 else 0)

    /** Bit-rate of the stream in bit/s. */
    val bitrate: Int get() = get(SpeexCtl.GET_BITRATE)

    /** Activity level of the last decoded frame, 0-100. */
    val activity: Int get() = get(SpeexCtl.GET_ACTIVITY)

    /**
     * Decodes one packet.
     *
     * A `null` packet triggers packet-loss concealment. Throws [SpeexException]
     * on a corrupt stream; callers driving a jitter buffer may prefer to treat
     * that packet as lost and call [decode] with `null` next.
     */
    fun decode(packet: ByteArray?): ShortArray {
        val handle = openHandle()
        return when (val result = SpeexNative.decoderDecode(handle, packet)) {
            0 -> SpeexNative.decoderSamples(handle)
                ?: throw SpeexException("Speex decoded a frame but returned no samples")
            XK_DECODE_END_OF_STREAM -> throw SpeexException("end of Speex stream")
            XK_DECODE_CORRUPT -> throw SpeexException("corrupt Speex packet")
            else -> throw SpeexException("Speex failed to decode (code $result)")
        }
    }

    /**
     * Decodes one packet and expands it to interleaved stereo using [stereo]'s
     * running state. Returns `2 * frameSize` samples.
     */
    fun decodeStereo(packet: ByteArray?, stereo: SpeexStereo): ShortArray {
        val handle = openHandle()
        return when (val result = SpeexNative.decoderDecodeStereo(handle, packet, stereo.requireHandle())) {
            0 -> SpeexNative.decoderSamples(handle)
                ?: throw SpeexException("Speex decoded a stereo frame but returned no samples")
            XK_DECODE_END_OF_STREAM -> throw SpeexException("end of Speex stream")
            XK_DECODE_CORRUPT -> throw SpeexException("corrupt Speex packet")
            else -> throw SpeexException("Speex failed to decode stereo (code $result)")
        }
    }

    /** Zeroes the decoder memory without changing its configuration. */
    fun reset() {
        ctl(SpeexCtl.RESET_STATE, 0)
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexNative.decoderFree(handle)
    }

    override fun toString(): String = "SpeexDecoder(${mode.name.lowercase()}, frameSize=$frameSize)"

    private fun ctl(request: Int, value: Int) {
        val handle = openHandle()
        val result = SpeexNative.decoderCtl(handle, request, value)
        if (result < 0) throw SpeexException("Speex rejected decoder request $request=$value (code $result)")
    }

    private fun get(request: Int): Int {
        val handle = openHandle()
        val value = SpeexNative.decoderGet(handle, request)
        if (value == XK_INVALID) throw SpeexException("Speex rejected decoder request $request")
        return value
    }

    private fun openHandle(): SpeexDecoderHandle {
        check(!closed) { "SpeexDecoder is closed" }
        return handle
    }

    companion object {
        /** Creates a decoder for [mode] with Speex's defaults. */
        fun open(mode: SpeexMode = SpeexMode.NARROWBAND): SpeexDecoder {
            val handle = SpeexNative.decoderCreate(mode.id)
                ?: throw SpeexException("Speex failed to create a ${mode.name.lowercase()} decoder")
            return SpeexDecoder(handle, mode)
        }
    }
}
