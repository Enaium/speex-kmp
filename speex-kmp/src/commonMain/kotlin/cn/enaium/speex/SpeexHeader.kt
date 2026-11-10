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
 * The Ogg/Speex identification header (`SpeexHeader`), 80 bytes on the wire.
 *
 * Speex over RTP does not use this header; it is needed for Ogg encapsulation
 * (`.spx` files) and for stream metadata such as the sample rate, channel count
 * and frame size.
 *
 * ```kotlin
 * val header = SpeexHeader.create(rate = 16_000, channels = 1, mode = SpeexMode.WIDEBAND)
 * header.bitrate = 27_800
 * val packet = header.toPacket()
 *
 * val parsed = SpeexHeader.fromPacket(packet)
 * ```
 */
class SpeexHeader private constructor(private val handle: SpeexHeaderHandle) : AutoCloseable {
    private var closed = false

    /** Magic string identifying a Speex stream; always `"Speex   "`. */
    val speexString: String get() = string(XK_HEADER_STRING)

    /** Speex version that produced the stream, e.g. `"1.2.1"`. */
    val speexVersion: String get() = string(XK_HEADER_VERSION_STRING)

    var versionId: Int
        get() = getInt(XKHeaderField.VERSION_ID)
        set(value) = setInt(XKHeaderField.VERSION_ID, value)

    /** Total header size in bytes; `sizeof(SpeexHeader)`. */
    var headerSize: Int
        get() = getInt(XKHeaderField.HEADER_SIZE)
        set(value) = setInt(XKHeaderField.HEADER_SIZE, value)

    /** Sampling rate of the stream in Hz. */
    var rate: Int
        get() = getInt(XKHeaderField.RATE)
        set(value) {
            require(value > 0) { "rate must be positive, got $value" }
            setInt(XKHeaderField.RATE, value)
        }

    /** Codec mode stored in the header. Changing it does not recompute [frameSize]. */
    var mode: SpeexMode
        get() = SpeexMode.fromId(getInt(XKHeaderField.MODE))
        set(value) = setInt(XKHeaderField.MODE, value.id)

    /** Bit-stream compatibility version of [mode]. */
    var modeBitstreamVersion: Int
        get() = getInt(XKHeaderField.MODE_BITSTREAM_VERSION)
        set(value) = setInt(XKHeaderField.MODE_BITSTREAM_VERSION, value)

    /** Number of channels, 1 or 2. */
    var channels: Int
        get() = getInt(XKHeaderField.CHANNELS)
        set(value) {
            require(value in 1..2) { "Speex supports 1 or 2 channels, got $value" }
            setInt(XKHeaderField.CHANNELS, value)
        }

    /** Bit-rate in bit/s; -1 when unspecified. */
    var bitrate: Int
        get() = getInt(XKHeaderField.BITRATE)
        set(value) = setInt(XKHeaderField.BITRATE, value)

    /** Samples per frame, e.g. 160 for narrowband. */
    var frameSize: Int
        get() = getInt(XKHeaderField.FRAME_SIZE)
        set(value) = setInt(XKHeaderField.FRAME_SIZE, value)

    /** Whether the stream was encoded with VBR. */
    var vbr: Boolean
        get() = getInt(XKHeaderField.VBR) != 0
        set(value) = setInt(XKHeaderField.VBR, if (value) 1 else 0)

    /** Frames stored per Ogg packet; 0 when unspecified. */
    var framesPerPacket: Int
        get() = getInt(XKHeaderField.FRAMES_PER_PACKET)
        set(value) = setInt(XKHeaderField.FRAMES_PER_PACKET, value)

    /** Number of extra headers following the comment header. */
    var extraHeaders: Int
        get() = getInt(XKHeaderField.EXTRA_HEADERS)
        set(value) = setInt(XKHeaderField.EXTRA_HEADERS, value)

    /** Reserved, must be zero. */
    var reserved1: Int
        get() = getInt(XKHeaderField.RESERVED1)
        set(value) = setInt(XKHeaderField.RESERVED1, value)

    /** Reserved, must be zero. */
    var reserved2: Int
        get() = getInt(XKHeaderField.RESERVED2)
        set(value) = setInt(XKHeaderField.RESERVED2, value)

    /** Serialises the header into its little-endian packet form. */
    fun toPacket(): ByteArray {
        val handle = openHandle()
        return SpeexNative.headerPacket(handle)
            ?: throw SpeexException("Speex could not serialise the header")
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexNative.headerFree(handle)
    }

    override fun toString(): String =
        "SpeexHeader(${mode.name.lowercase()}, rate=$rate, channels=$channels, frameSize=$frameSize)"

    private fun string(which: Int): String {
        val handle = openHandle()
        return SpeexNative.headerString(handle, which)
    }

    private fun getInt(field: XKHeaderField): Int {
        val handle = openHandle()
        val value = SpeexNative.headerGetInt(handle, field.value)
        if (value == XK_INVALID) throw SpeexException("Speex rejected header field ${field.name}")
        return value
    }

    private fun setInt(field: XKHeaderField, value: Int) {
        val handle = openHandle()
        val result = SpeexNative.headerSetInt(handle, field.value, value)
        if (result < 0) throw SpeexException("Speex rejected header field ${field.name}=$value")
    }

    private fun openHandle(): SpeexHeaderHandle {
        check(!closed) { "SpeexHeader is closed" }
        return handle
    }

    companion object {
        /**
         * Builds a header for [rate], [channels] and [mode], initialised by
         * `speex_init_header()` (version, bit-stream version and frame size).
         */
        fun create(
            rate: Int,
            channels: Int = 1,
            mode: SpeexMode = SpeexMode.fromSampleRate(rate),
        ): SpeexHeader {
            require(rate > 0) { "rate must be positive, got $rate" }
            require(channels in 1..2) { "Speex supports 1 or 2 channels, got $channels" }
            val handle = SpeexNative.headerCreate(rate, channels, mode.id)
                ?: throw SpeexException("Speex could not create a header for $rate Hz, $channels channel(s)")
            return SpeexHeader(handle)
        }

        /** Parses a packet; throws when it is not a Speex header. */
        fun fromPacket(packet: ByteArray): SpeexHeader {
            require(packet.size >= 80) { "a Speex header is 80 bytes, got ${packet.size}" }
            val handle = SpeexNative.headerParse(packet)
                ?: throw SpeexException("packet is not a valid Speex header")
            return SpeexHeader(handle)
        }
    }
}
