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

// =========================================================================
// JNI bridge: declarations are shared by the JVM and Android targets.
// =========================================================================

internal object JniSpeex {
    init {
        NativeLoader.load()
    }

    external fun nativeVersionString(): String

    external fun nativeVersionMajor(): Int

    external fun nativeVersionMinor(): Int

    external fun nativeVersionMicro(): Int

    external fun nativeVersionExtra(): String

    external fun nativeModeFrameSize(mode: Int): Int

    external fun nativeModeSubmodeBits(mode: Int, submode: Int): Int

    external fun nativeModeName(mode: Int): String

    external fun nativeBitsCreate(): Long

    external fun nativeBitsFree(handle: Long)

    external fun nativeBitsReset(handle: Long)

    external fun nativeBitsRewind(handle: Long)

    external fun nativeBitsReadFrom(handle: Long, data: ByteArray): Int

    external fun nativeBitsReadWholeBytes(handle: Long, data: ByteArray): Int

    external fun nativeBitsPack(handle: Long, data: Int, nbits: Int)

    external fun nativeBitsUnpackSigned(handle: Long, nbits: Int): Int

    external fun nativeBitsUnpackUnsigned(handle: Long, nbits: Int): Int

    external fun nativeBitsPeek(handle: Long): Int

    external fun nativeBitsPeekUnsigned(handle: Long, nbits: Int): Int

    external fun nativeBitsAdvance(handle: Long, nbits: Int)

    external fun nativeBitsRemaining(handle: Long): Int

    external fun nativeBitsNbytes(handle: Long): Int

    external fun nativeBitsInsertTerminator(handle: Long)

    external fun nativeBitsWrite(handle: Long): ByteArray

    external fun nativeEncoderCreate(mode: Int): Long

    external fun nativeEncoderFree(handle: Long)

    external fun nativeEncoderFrameSize(handle: Long): Int

    external fun nativeEncoderEncode(handle: Long, samples: ShortArray, offset: Int, count: Int): Int

    external fun nativeEncoderEncodeStereo(handle: Long, samples: ShortArray, offset: Int, count: Int): Int

    external fun nativeEncoderPacket(handle: Long): ByteArray?

    external fun nativeEncoderCtl(handle: Long, request: Int, value: Int): Int

    external fun nativeEncoderGet(handle: Long, request: Int): Int

    external fun nativeEncoderSetVbrQuality(handle: Long, value: Float): Int

    external fun nativeEncoderGetVbrQuality(handle: Long): Float

    external fun nativeEncoderGetRelativeQuality(handle: Long): Float

    external fun nativeDecoderCreate(mode: Int): Long

    external fun nativeDecoderFree(handle: Long)

    external fun nativeDecoderFrameSize(handle: Long): Int

    external fun nativeDecoderDecode(handle: Long, packet: ByteArray?): Int

    external fun nativeDecoderDecodeStereo(handle: Long, packet: ByteArray?, stereo: Long): Int

    external fun nativeDecoderSamples(handle: Long): ShortArray?

    external fun nativeDecoderCtl(handle: Long, request: Int, value: Int): Int

    external fun nativeDecoderGet(handle: Long, request: Int): Int

    external fun nativeStereoCreate(): Long

    external fun nativeStereoFree(handle: Long)

    external fun nativeStereoReset(handle: Long)

    external fun nativeStereoEncode(handle: Long, samples: ShortArray, offset: Int, count: Int, bits: Long): Int

    external fun nativeStereoDecode(handle: Long, samples: ShortArray, count: Int): Int

    external fun nativeHeaderCreate(rate: Int, channels: Int, mode: Int): Long

    external fun nativeHeaderParse(packet: ByteArray): Long

    external fun nativeHeaderFree(handle: Long)

    external fun nativeHeaderGetInt(handle: Long, field: Int): Int

    external fun nativeHeaderSetInt(handle: Long, field: Int, value: Int): Int

    external fun nativeHeaderString(handle: Long, which: Int): String

    external fun nativeHeaderPacket(handle: Long): ByteArray?
}

internal actual class SpeexBitsHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexEncoderHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexDecoderHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexStereoHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexHeaderHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual object SpeexNative {
    actual fun versionString(): String = JniSpeex.nativeVersionString()

    actual fun versionMajor(): Int = JniSpeex.nativeVersionMajor()

    actual fun versionMinor(): Int = JniSpeex.nativeVersionMinor()

    actual fun versionMicro(): Int = JniSpeex.nativeVersionMicro()

    actual fun versionExtra(): String = JniSpeex.nativeVersionExtra()

    actual fun modeFrameSize(mode: Int): Int = JniSpeex.nativeModeFrameSize(mode)

    actual fun modeSubmodeBits(mode: Int, submode: Int): Int = JniSpeex.nativeModeSubmodeBits(mode, submode)

    actual fun modeName(mode: Int): String = JniSpeex.nativeModeName(mode)

    actual fun bitsCreate(): SpeexBitsHandle? {
        val pointer = JniSpeex.nativeBitsCreate()
        if (pointer == 0L) return null
        return SpeexBitsHandle().also { it.pointer = pointer }
    }

    actual fun bitsFree(handle: SpeexBitsHandle) {
        JniSpeex.nativeBitsFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun bitsReset(handle: SpeexBitsHandle) = JniSpeex.nativeBitsReset(handle.pointer)

    actual fun bitsRewind(handle: SpeexBitsHandle) = JniSpeex.nativeBitsRewind(handle.pointer)

    actual fun bitsReadFrom(handle: SpeexBitsHandle, data: ByteArray): Int =
        JniSpeex.nativeBitsReadFrom(handle.pointer, data)

    actual fun bitsReadWholeBytes(handle: SpeexBitsHandle, data: ByteArray): Int =
        JniSpeex.nativeBitsReadWholeBytes(handle.pointer, data)

    actual fun bitsPack(handle: SpeexBitsHandle, data: Int, nbits: Int) =
        JniSpeex.nativeBitsPack(handle.pointer, data, nbits)

    actual fun bitsUnpackSigned(handle: SpeexBitsHandle, nbits: Int): Int =
        JniSpeex.nativeBitsUnpackSigned(handle.pointer, nbits)

    actual fun bitsUnpackUnsigned(handle: SpeexBitsHandle, nbits: Int): Int =
        JniSpeex.nativeBitsUnpackUnsigned(handle.pointer, nbits)

    actual fun bitsPeek(handle: SpeexBitsHandle): Int = JniSpeex.nativeBitsPeek(handle.pointer)

    actual fun bitsPeekUnsigned(handle: SpeexBitsHandle, nbits: Int): Int =
        JniSpeex.nativeBitsPeekUnsigned(handle.pointer, nbits)

    actual fun bitsAdvance(handle: SpeexBitsHandle, nbits: Int) =
        JniSpeex.nativeBitsAdvance(handle.pointer, nbits)

    actual fun bitsRemaining(handle: SpeexBitsHandle): Int = JniSpeex.nativeBitsRemaining(handle.pointer)

    actual fun bitsNbytes(handle: SpeexBitsHandle): Int = JniSpeex.nativeBitsNbytes(handle.pointer)

    actual fun bitsInsertTerminator(handle: SpeexBitsHandle) =
        JniSpeex.nativeBitsInsertTerminator(handle.pointer)

    actual fun bitsWrite(handle: SpeexBitsHandle): ByteArray = JniSpeex.nativeBitsWrite(handle.pointer)

    actual fun encoderCreate(mode: Int): SpeexEncoderHandle? {
        val pointer = JniSpeex.nativeEncoderCreate(mode)
        if (pointer == 0L) return null
        return SpeexEncoderHandle().also { it.pointer = pointer }
    }

    actual fun encoderFree(handle: SpeexEncoderHandle) {
        JniSpeex.nativeEncoderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun encoderFrameSize(handle: SpeexEncoderHandle): Int =
        JniSpeex.nativeEncoderFrameSize(handle.pointer)

    actual fun encoderEncode(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int =
        JniSpeex.nativeEncoderEncode(handle.pointer, samples, offset, count)

    actual fun encoderEncodeStereo(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int =
        JniSpeex.nativeEncoderEncodeStereo(handle.pointer, samples, offset, count)

    actual fun encoderPacket(handle: SpeexEncoderHandle): ByteArray? =
        JniSpeex.nativeEncoderPacket(handle.pointer)

    actual fun encoderCtl(handle: SpeexEncoderHandle, request: Int, value: Int): Int =
        JniSpeex.nativeEncoderCtl(handle.pointer, request, value)

    actual fun encoderGet(handle: SpeexEncoderHandle, request: Int): Int =
        JniSpeex.nativeEncoderGet(handle.pointer, request)

    actual fun encoderSetVbrQuality(handle: SpeexEncoderHandle, value: Float): Int =
        JniSpeex.nativeEncoderSetVbrQuality(handle.pointer, value)

    actual fun encoderGetVbrQuality(handle: SpeexEncoderHandle): Float =
        JniSpeex.nativeEncoderGetVbrQuality(handle.pointer)

    actual fun encoderGetRelativeQuality(handle: SpeexEncoderHandle): Float =
        JniSpeex.nativeEncoderGetRelativeQuality(handle.pointer)

    actual fun decoderCreate(mode: Int): SpeexDecoderHandle? {
        val pointer = JniSpeex.nativeDecoderCreate(mode)
        if (pointer == 0L) return null
        return SpeexDecoderHandle().also { it.pointer = pointer }
    }

    actual fun decoderFree(handle: SpeexDecoderHandle) {
        JniSpeex.nativeDecoderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun decoderFrameSize(handle: SpeexDecoderHandle): Int =
        JniSpeex.nativeDecoderFrameSize(handle.pointer)

    actual fun decoderDecode(handle: SpeexDecoderHandle, packet: ByteArray?): Int =
        JniSpeex.nativeDecoderDecode(handle.pointer, packet)

    actual fun decoderDecodeStereo(
        handle: SpeexDecoderHandle,
        packet: ByteArray?,
        stereo: SpeexStereoHandle,
    ): Int = JniSpeex.nativeDecoderDecodeStereo(handle.pointer, packet, stereo.pointer)

    actual fun decoderSamples(handle: SpeexDecoderHandle): ShortArray? =
        JniSpeex.nativeDecoderSamples(handle.pointer)

    actual fun decoderCtl(handle: SpeexDecoderHandle, request: Int, value: Int): Int =
        JniSpeex.nativeDecoderCtl(handle.pointer, request, value)

    actual fun decoderGet(handle: SpeexDecoderHandle, request: Int): Int =
        JniSpeex.nativeDecoderGet(handle.pointer, request)

    actual fun stereoCreate(): SpeexStereoHandle? {
        val pointer = JniSpeex.nativeStereoCreate()
        if (pointer == 0L) return null
        return SpeexStereoHandle().also { it.pointer = pointer }
    }

    actual fun stereoFree(handle: SpeexStereoHandle) {
        JniSpeex.nativeStereoFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun stereoReset(handle: SpeexStereoHandle) = JniSpeex.nativeStereoReset(handle.pointer)

    actual fun stereoEncode(
        handle: SpeexStereoHandle,
        samples: ShortArray,
        offset: Int,
        count: Int,
        bits: SpeexBitsHandle,
    ): Int = JniSpeex.nativeStereoEncode(handle.pointer, samples, offset, count, bits.pointer)

    actual fun stereoDecode(handle: SpeexStereoHandle, samples: ShortArray, count: Int): Int =
        JniSpeex.nativeStereoDecode(handle.pointer, samples, count)

    actual fun headerCreate(rate: Int, channels: Int, mode: Int): SpeexHeaderHandle? {
        val pointer = JniSpeex.nativeHeaderCreate(rate, channels, mode)
        if (pointer == 0L) return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerParse(packet: ByteArray): SpeexHeaderHandle? {
        val pointer = JniSpeex.nativeHeaderParse(packet)
        if (pointer == 0L) return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerFree(handle: SpeexHeaderHandle) {
        JniSpeex.nativeHeaderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun headerGetInt(handle: SpeexHeaderHandle, field: Int): Int =
        JniSpeex.nativeHeaderGetInt(handle.pointer, field)

    actual fun headerSetInt(handle: SpeexHeaderHandle, field: Int, value: Int): Int =
        JniSpeex.nativeHeaderSetInt(handle.pointer, field, value)

    actual fun headerString(handle: SpeexHeaderHandle, which: Int): String =
        JniSpeex.nativeHeaderString(handle.pointer, which)

    actual fun headerPacket(handle: SpeexHeaderHandle): ByteArray? =
        JniSpeex.nativeHeaderPacket(handle.pointer)
}
