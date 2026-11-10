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

@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package cn.enaium.speex

import kotlin.io.encoding.Base64

internal actual class SpeexBitsHandle actual constructor() {
    /** WebAssembly pointer, always 32-bit. */
    internal var pointer: Int = 0
}

internal actual class SpeexEncoderHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexDecoderHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexStereoHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexHeaderHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual object SpeexNative {
    private var initialised = false

    private fun ensureInitialised() {
        if (!initialised) {
            webInit()
            initialised = true
        }
    }

    actual fun versionString(): String {
        ensureInitialised()
        return webVersionString()
    }

    actual fun versionMajor(): Int {
        ensureInitialised()
        return webVersionMajor()
    }

    actual fun versionMinor(): Int {
        ensureInitialised()
        return webVersionMinor()
    }

    actual fun versionMicro(): Int {
        ensureInitialised()
        return webVersionMicro()
    }

    actual fun versionExtra(): String {
        ensureInitialised()
        return webVersionExtra()
    }

    actual fun modeFrameSize(mode: Int): Int {
        ensureInitialised()
        return webModeFrameSize(mode)
    }

    actual fun modeSubmodeBits(mode: Int, submode: Int): Int {
        ensureInitialised()
        return webModeSubmodeBits(mode, submode)
    }

    actual fun modeName(mode: Int): String {
        ensureInitialised()
        return webModeName(mode)
    }

    actual fun bitsCreate(): SpeexBitsHandle? {
        ensureInitialised()
        val pointer = webBitsCreate()
        if (pointer == 0) return null
        return SpeexBitsHandle().also { it.pointer = pointer }
    }

    actual fun bitsFree(handle: SpeexBitsHandle) {
        ensureInitialised()
        webBitsFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun bitsReset(handle: SpeexBitsHandle) {
        ensureInitialised()
        webBitsReset(handle.pointer)
    }

    actual fun bitsRewind(handle: SpeexBitsHandle) {
        ensureInitialised()
        webBitsRewind(handle.pointer)
    }

    actual fun bitsReadFrom(handle: SpeexBitsHandle, data: ByteArray): Int {
        ensureInitialised()
        return webBitsReadFrom(handle.pointer, Base64.encode(data))
    }

    actual fun bitsReadWholeBytes(handle: SpeexBitsHandle, data: ByteArray): Int {
        ensureInitialised()
        return webBitsReadWholeBytes(handle.pointer, Base64.encode(data))
    }

    actual fun bitsPack(handle: SpeexBitsHandle, data: Int, nbits: Int) {
        ensureInitialised()
        webBitsPack(handle.pointer, data, nbits)
    }

    actual fun bitsUnpackSigned(handle: SpeexBitsHandle, nbits: Int): Int {
        ensureInitialised()
        return webBitsUnpackSigned(handle.pointer, nbits)
    }

    actual fun bitsUnpackUnsigned(handle: SpeexBitsHandle, nbits: Int): Int {
        ensureInitialised()
        return webBitsUnpackUnsigned(handle.pointer, nbits)
    }

    actual fun bitsPeek(handle: SpeexBitsHandle): Int {
        ensureInitialised()
        return webBitsPeek(handle.pointer)
    }

    actual fun bitsPeekUnsigned(handle: SpeexBitsHandle, nbits: Int): Int {
        ensureInitialised()
        return webBitsPeekUnsigned(handle.pointer, nbits)
    }

    actual fun bitsAdvance(handle: SpeexBitsHandle, nbits: Int) {
        ensureInitialised()
        webBitsAdvance(handle.pointer, nbits)
    }

    actual fun bitsRemaining(handle: SpeexBitsHandle): Int {
        ensureInitialised()
        return webBitsRemaining(handle.pointer)
    }

    actual fun bitsNbytes(handle: SpeexBitsHandle): Int {
        ensureInitialised()
        return webBitsNbytes(handle.pointer)
    }

    actual fun bitsInsertTerminator(handle: SpeexBitsHandle) {
        ensureInitialised()
        webBitsInsertTerminator(handle.pointer)
    }

    actual fun bitsWrite(handle: SpeexBitsHandle): ByteArray {
        ensureInitialised()
        return Base64.decode(webBitsWrite(handle.pointer))
    }

    actual fun encoderCreate(mode: Int): SpeexEncoderHandle? {
        ensureInitialised()
        val pointer = webEncoderCreate(mode)
        if (pointer == 0) return null
        return SpeexEncoderHandle().also { it.pointer = pointer }
    }

    actual fun encoderFree(handle: SpeexEncoderHandle) {
        ensureInitialised()
        webEncoderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun encoderFrameSize(handle: SpeexEncoderHandle): Int {
        ensureInitialised()
        return webEncoderFrameSize(handle.pointer)
    }

    actual fun encoderEncode(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int {
        ensureInitialised()
        return webEncoderEncode(handle.pointer, SpeexWebPacking.encodeShorts(samples, offset, count))
    }

    actual fun encoderEncodeStereo(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int {
        ensureInitialised()
        val result = webEncoderEncodeStereo(handle.pointer, SpeexWebPacking.encodeShorts(samples, offset, count))
        val code = parseWebCode(result)
        val payload = parseWebPayload(result)
        if (payload.isNotEmpty()) {
            SpeexWebPacking.decodeShorts(payload).copyInto(samples, offset)
        }
        return code
    }

    actual fun encoderPacket(handle: SpeexEncoderHandle): ByteArray? {
        ensureInitialised()
        val encoded = webEncoderPacket(handle.pointer)
        return if (encoded.isEmpty()) null else Base64.decode(encoded)
    }

    actual fun encoderCtl(handle: SpeexEncoderHandle, request: Int, value: Int): Int {
        ensureInitialised()
        return webEncoderCtl(handle.pointer, request, value)
    }

    actual fun encoderGet(handle: SpeexEncoderHandle, request: Int): Int {
        ensureInitialised()
        return webEncoderGet(handle.pointer, request)
    }

    actual fun encoderSetVbrQuality(handle: SpeexEncoderHandle, value: Float): Int {
        ensureInitialised()
        return webEncoderSetVbrQuality(handle.pointer, value)
    }

    actual fun encoderGetVbrQuality(handle: SpeexEncoderHandle): Float {
        ensureInitialised()
        return webEncoderGetVbrQuality(handle.pointer)
    }

    actual fun encoderGetRelativeQuality(handle: SpeexEncoderHandle): Float {
        ensureInitialised()
        return webEncoderGetRelativeQuality(handle.pointer)
    }

    actual fun decoderCreate(mode: Int): SpeexDecoderHandle? {
        ensureInitialised()
        val pointer = webDecoderCreate(mode)
        if (pointer == 0) return null
        return SpeexDecoderHandle().also { it.pointer = pointer }
    }

    actual fun decoderFree(handle: SpeexDecoderHandle) {
        ensureInitialised()
        webDecoderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun decoderFrameSize(handle: SpeexDecoderHandle): Int {
        ensureInitialised()
        return webDecoderFrameSize(handle.pointer)
    }

    actual fun decoderDecode(handle: SpeexDecoderHandle, packet: ByteArray?): Int {
        ensureInitialised()
        val encoded = if (packet == null) "" else Base64.encode(packet)
        return webDecoderDecode(handle.pointer, encoded)
    }

    actual fun decoderDecodeStereo(
        handle: SpeexDecoderHandle,
        packet: ByteArray?,
        stereo: SpeexStereoHandle,
    ): Int {
        ensureInitialised()
        val encoded = if (packet == null) "" else Base64.encode(packet)
        return webDecoderDecodeStereo(handle.pointer, encoded, stereo.pointer)
    }

    actual fun decoderSamples(handle: SpeexDecoderHandle): ShortArray? {
        ensureInitialised()
        val encoded = webDecoderSamples(handle.pointer)
        return if (encoded.isEmpty()) null else SpeexWebPacking.decodeShorts(encoded)
    }

    actual fun decoderCtl(handle: SpeexDecoderHandle, request: Int, value: Int): Int {
        ensureInitialised()
        return webDecoderCtl(handle.pointer, request, value)
    }

    actual fun decoderGet(handle: SpeexDecoderHandle, request: Int): Int {
        ensureInitialised()
        return webDecoderGet(handle.pointer, request)
    }

    actual fun stereoCreate(): SpeexStereoHandle? {
        ensureInitialised()
        val pointer = webStereoCreate()
        if (pointer == 0) return null
        return SpeexStereoHandle().also { it.pointer = pointer }
    }

    actual fun stereoFree(handle: SpeexStereoHandle) {
        ensureInitialised()
        webStereoFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun stereoReset(handle: SpeexStereoHandle) {
        ensureInitialised()
        webStereoReset(handle.pointer)
    }

    actual fun stereoEncode(
        handle: SpeexStereoHandle,
        samples: ShortArray,
        offset: Int,
        count: Int,
        bits: SpeexBitsHandle,
    ): Int {
        ensureInitialised()
        val result = webStereoEncode(
            handle.pointer,
            SpeexWebPacking.encodeShorts(samples, offset, count),
            offset,
            count,
            bits.pointer,
        )
        val code = parseWebCode(result)
        val payload = parseWebPayload(result)
        if (payload.isNotEmpty()) {
            SpeexWebPacking.decodeShorts(payload).copyInto(samples, offset)
        }
        return code
    }

    actual fun stereoDecode(handle: SpeexStereoHandle, samples: ShortArray, count: Int): Int {
        ensureInitialised()
        val input = if (samples.size == count) samples else samples.copyOfRange(0, count)
        val result = webStereoDecode(handle.pointer, SpeexWebPacking.encodeShorts(input), count)
        val code = parseWebCode(result)
        val payload = parseWebPayload(result)
        if (payload.isNotEmpty()) {
            SpeexWebPacking.decodeShorts(payload).copyInto(samples, 0)
        }
        return code
    }

    actual fun headerCreate(rate: Int, channels: Int, mode: Int): SpeexHeaderHandle? {
        ensureInitialised()
        val pointer = webHeaderCreate(rate, channels, mode)
        if (pointer == 0) return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerParse(packet: ByteArray): SpeexHeaderHandle? {
        ensureInitialised()
        val pointer = webHeaderParse(Base64.encode(packet))
        if (pointer == 0) return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerFree(handle: SpeexHeaderHandle) {
        ensureInitialised()
        webHeaderFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun headerGetInt(handle: SpeexHeaderHandle, field: Int): Int {
        ensureInitialised()
        return webHeaderGetInt(handle.pointer, field)
    }

    actual fun headerSetInt(handle: SpeexHeaderHandle, field: Int, value: Int): Int {
        ensureInitialised()
        return webHeaderSetInt(handle.pointer, field, value)
    }

    actual fun headerString(handle: SpeexHeaderHandle, which: Int): String {
        ensureInitialised()
        return webHeaderString(handle.pointer, which)
    }

    actual fun headerPacket(handle: SpeexHeaderHandle): ByteArray? {
        ensureInitialised()
        val encoded = webHeaderPacket(handle.pointer)
        return if (encoded.isEmpty()) null else Base64.decode(encoded)
    }
}
