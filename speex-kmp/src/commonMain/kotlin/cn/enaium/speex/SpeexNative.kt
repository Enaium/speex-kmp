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

/** Thrown when Speex rejects a call: an invalid parameter, a failed allocation or a corrupt stream. */
class SpeexException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Opaque bit-stream owned by the native library. */
internal expect class SpeexBitsHandle()

/** Opaque encoder state owned by the native library. */
internal expect class SpeexEncoderHandle()

/** Opaque decoder state owned by the native library. */
internal expect class SpeexDecoderHandle()

/** Opaque intensity-stereo state owned by the native library. */
internal expect class SpeexStereoHandle()

/** Opaque Ogg/Speex header owned by the native library. */
internal expect class SpeexHeaderHandle()

/**
 * Bridge to the C shim, implemented for JNI, Kotlin/Native cinterop and the
 * WebAssembly module. Every implementation follows the same contract from
 * `jni/include/speex_kmp.h`:
 *
 * - `encoderEncode` returns [XK_PACKET], [XK_NO_PACKET] (DTX) or [XK_ERROR];
 * - `decoderDecode` returns 0, [XK_DECODE_END_OF_STREAM], [XK_DECODE_CORRUPT] or [XK_INVALID];
 * - handles are never reused after their `free` call.
 */
internal expect object SpeexNative {
    fun versionString(): String

    fun versionMajor(): Int

    fun versionMinor(): Int

    fun versionMicro(): Int

    fun versionExtra(): String

    fun modeFrameSize(mode: Int): Int

    fun modeSubmodeBits(mode: Int, submode: Int): Int

    fun modeName(mode: Int): String

    fun bitsCreate(): SpeexBitsHandle?

    fun bitsFree(handle: SpeexBitsHandle)

    fun bitsReset(handle: SpeexBitsHandle)

    fun bitsRewind(handle: SpeexBitsHandle)

    fun bitsReadFrom(handle: SpeexBitsHandle, data: ByteArray): Int

    fun bitsReadWholeBytes(handle: SpeexBitsHandle, data: ByteArray): Int

    fun bitsPack(handle: SpeexBitsHandle, data: Int, nbits: Int)

    fun bitsUnpackSigned(handle: SpeexBitsHandle, nbits: Int): Int

    fun bitsUnpackUnsigned(handle: SpeexBitsHandle, nbits: Int): Int

    fun bitsPeek(handle: SpeexBitsHandle): Int

    fun bitsPeekUnsigned(handle: SpeexBitsHandle, nbits: Int): Int

    fun bitsAdvance(handle: SpeexBitsHandle, nbits: Int)

    fun bitsRemaining(handle: SpeexBitsHandle): Int

    fun bitsNbytes(handle: SpeexBitsHandle): Int

    fun bitsInsertTerminator(handle: SpeexBitsHandle)

    fun bitsWrite(handle: SpeexBitsHandle): ByteArray

    fun encoderCreate(mode: Int): SpeexEncoderHandle?

    fun encoderFree(handle: SpeexEncoderHandle)

    fun encoderFrameSize(handle: SpeexEncoderHandle): Int

    fun encoderEncode(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int

    fun encoderEncodeStereo(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int

    fun encoderPacket(handle: SpeexEncoderHandle): ByteArray?

    fun encoderCtl(handle: SpeexEncoderHandle, request: Int, value: Int): Int

    fun encoderGet(handle: SpeexEncoderHandle, request: Int): Int

    fun encoderSetVbrQuality(handle: SpeexEncoderHandle, value: Float): Int

    fun encoderGetVbrQuality(handle: SpeexEncoderHandle): Float

    fun encoderGetRelativeQuality(handle: SpeexEncoderHandle): Float

    fun decoderCreate(mode: Int): SpeexDecoderHandle?

    fun decoderFree(handle: SpeexDecoderHandle)

    fun decoderFrameSize(handle: SpeexDecoderHandle): Int

    fun decoderDecode(handle: SpeexDecoderHandle, packet: ByteArray?): Int

    fun decoderDecodeStereo(handle: SpeexDecoderHandle, packet: ByteArray?, stereo: SpeexStereoHandle): Int

    fun decoderSamples(handle: SpeexDecoderHandle): ShortArray?

    fun decoderCtl(handle: SpeexDecoderHandle, request: Int, value: Int): Int

    fun decoderGet(handle: SpeexDecoderHandle, request: Int): Int

    fun stereoCreate(): SpeexStereoHandle?

    fun stereoFree(handle: SpeexStereoHandle)

    fun stereoReset(handle: SpeexStereoHandle)

    fun stereoEncode(handle: SpeexStereoHandle, samples: ShortArray, offset: Int, count: Int, bits: SpeexBitsHandle): Int

    fun stereoDecode(handle: SpeexStereoHandle, samples: ShortArray, count: Int): Int

    fun headerCreate(rate: Int, channels: Int, mode: Int): SpeexHeaderHandle?

    fun headerParse(packet: ByteArray): SpeexHeaderHandle?

    fun headerFree(handle: SpeexHeaderHandle)

    fun headerGetInt(handle: SpeexHeaderHandle, field: Int): Int

    fun headerSetInt(handle: SpeexHeaderHandle, field: Int, value: Int): Int

    fun headerString(handle: SpeexHeaderHandle, which: Int): String

    fun headerPacket(handle: SpeexHeaderHandle): ByteArray?
}

/** `XK_PACKET`: the encoder produced a packet. */
internal const val XK_PACKET: Int = 1

/** `XK_NO_PACKET`: DTX dropped the frame. */
internal const val XK_NO_PACKET: Int = 0

/** `XK_ERROR`: the shim or Speex failed. */
internal const val XK_ERROR: Int = -1

/** `XK_INVALID`: an argument was rejected before reaching Speex. */
internal const val XK_INVALID: Int = -3

/** `XK_DECODE_END_OF_STREAM`. */
internal const val XK_DECODE_END_OF_STREAM: Int = -1

/** `XK_DECODE_CORRUPT`: concealment was applied to a corrupt packet. */
internal const val XK_DECODE_CORRUPT: Int = -2

/** Readable/writable `SpeexHeader` fields, mirroring `XK_HEADER_*`. */
internal enum class XKHeaderField(val value: Int) {
    VERSION_ID(1),
    HEADER_SIZE(2),
    RATE(3),
    MODE(4),
    MODE_BITSTREAM_VERSION(5),
    CHANNELS(6),
    BITRATE(7),
    FRAME_SIZE(8),
    VBR(9),
    FRAMES_PER_PACKET(10),
    EXTRA_HEADERS(11),
    RESERVED1(12),
    RESERVED2(13),
}

internal const val XK_HEADER_STRING: Int = 1

internal const val XK_HEADER_VERSION_STRING: Int = 2

/**
 * Integer-valued `SPEEX_*` control requests. The float-valued requests
 * (`SPEEX_SET_VBR_QUALITY`, `SPEEX_GET_VBR_QUALITY`,
 * `SPEEX_GET_RELATIVE_QUALITY`) have dedicated bridge functions.
 */
internal object SpeexCtl {
    const val SET_ENH: Int = 0
    const val GET_ENH: Int = 1
    const val GET_FRAME_SIZE: Int = 3
    const val SET_QUALITY: Int = 4
    const val SET_MODE: Int = 6
    const val GET_MODE: Int = 7
    const val SET_LOW_MODE: Int = 8
    const val GET_LOW_MODE: Int = 9
    const val SET_HIGH_MODE: Int = 10
    const val GET_HIGH_MODE: Int = 11
    const val SET_VBR: Int = 12
    const val GET_VBR: Int = 13
    const val SET_COMPLEXITY: Int = 16
    const val GET_COMPLEXITY: Int = 17
    const val SET_BITRATE: Int = 18
    const val GET_BITRATE: Int = 19
    const val SET_SAMPLING_RATE: Int = 24
    const val GET_SAMPLING_RATE: Int = 25
    const val RESET_STATE: Int = 26
    const val SET_VAD: Int = 30
    const val GET_VAD: Int = 31
    const val SET_ABR: Int = 32
    const val GET_ABR: Int = 33
    const val SET_DTX: Int = 34
    const val GET_DTX: Int = 35
    const val SET_SUBMODE_ENCODING: Int = 36
    const val GET_SUBMODE_ENCODING: Int = 37
    const val GET_LOOKAHEAD: Int = 39
    const val SET_PLC_TUNING: Int = 40
    const val GET_PLC_TUNING: Int = 41
    const val SET_VBR_MAX_BITRATE: Int = 42
    const val GET_VBR_MAX_BITRATE: Int = 43
    const val SET_HIGHPASS: Int = 44
    const val GET_HIGHPASS: Int = 45
    const val GET_ACTIVITY: Int = 47
}
