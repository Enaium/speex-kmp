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

/**
 * Web bridge shared by the Kotlin/JS and Kotlin/Wasm targets.
 *
 * The JavaScript glue owns the WebAssembly instance and exposes one function
 * per C entry point on `globalThis.__speexWeb`. Pointers are 32-bit
 * WebAssembly addresses and therefore `Int`s here.
 *
 * Bulk data (PCM samples and packets) crosses the boundary as base64 strings
 * because Kotlin/JS and Kotlin/Wasm cannot share typed arrays with a module
 * compiled outside their own toolchain.
 */

internal expect fun webInit()

internal expect fun webVersionString(): String

internal expect fun webVersionMajor(): Int

internal expect fun webVersionMinor(): Int

internal expect fun webVersionMicro(): Int

internal expect fun webVersionExtra(): String

internal expect fun webModeFrameSize(mode: Int): Int

internal expect fun webModeSubmodeBits(mode: Int, submode: Int): Int

internal expect fun webModeName(mode: Int): String

internal expect fun webBitsCreate(): Int

internal expect fun webBitsFree(handle: Int)

internal expect fun webBitsReset(handle: Int)

internal expect fun webBitsRewind(handle: Int)

internal expect fun webBitsReadFrom(handle: Int, data: String): Int

internal expect fun webBitsReadWholeBytes(handle: Int, data: String): Int

internal expect fun webBitsPack(handle: Int, data: Int, nbits: Int)

internal expect fun webBitsUnpackSigned(handle: Int, nbits: Int): Int

internal expect fun webBitsUnpackUnsigned(handle: Int, nbits: Int): Int

internal expect fun webBitsPeek(handle: Int): Int

internal expect fun webBitsPeekUnsigned(handle: Int, nbits: Int): Int

internal expect fun webBitsAdvance(handle: Int, nbits: Int)

internal expect fun webBitsRemaining(handle: Int): Int

internal expect fun webBitsNbytes(handle: Int): Int

internal expect fun webBitsInsertTerminator(handle: Int)

/** Base64 of the serialised stream. */
internal expect fun webBitsWrite(handle: Int): String

internal expect fun webEncoderCreate(mode: Int): Int

internal expect fun webEncoderFree(handle: Int)

internal expect fun webEncoderFrameSize(handle: Int): Int

internal expect fun webEncoderEncode(handle: Int, samples: String): Int

internal expect fun webEncoderEncodeStereo(handle: Int, samples: String): String

/** Base64 of the last packet. */
internal expect fun webEncoderPacket(handle: Int): String

internal expect fun webEncoderCtl(handle: Int, request: Int, value: Int): Int

internal expect fun webEncoderGet(handle: Int, request: Int): Int

internal expect fun webEncoderSetVbrQuality(handle: Int, value: Float): Int

internal expect fun webEncoderGetVbrQuality(handle: Int): Float

internal expect fun webEncoderGetRelativeQuality(handle: Int): Float

internal expect fun webDecoderCreate(mode: Int): Int

internal expect fun webDecoderFree(handle: Int)

internal expect fun webDecoderFrameSize(handle: Int): Int

/** An empty [packet] triggers packet-loss concealment. */
internal expect fun webDecoderDecode(handle: Int, packet: String): Int

internal expect fun webDecoderDecodeStereo(handle: Int, packet: String, stereo: Int): Int

/** Base64 of the decoded 16-bit samples. */
internal expect fun webDecoderSamples(handle: Int): String

internal expect fun webDecoderCtl(handle: Int, request: Int, value: Int): Int

internal expect fun webDecoderGet(handle: Int, request: Int): Int

internal expect fun webStereoCreate(): Int

internal expect fun webStereoFree(handle: Int)

internal expect fun webStereoReset(handle: Int)

/** Returns `"<code>:<base64 samples>"`; the samples were folded to mono. */
internal expect fun webStereoEncode(handle: Int, samples: String, offset: Int, count: Int, bits: Int): String

/** Returns `"<code>:<base64 samples>"`; the mono frame was expanded to stereo. */
internal expect fun webStereoDecode(handle: Int, samples: String, count: Int): String

internal expect fun webHeaderCreate(rate: Int, channels: Int, mode: Int): Int

internal expect fun webHeaderParse(packet: String): Int

internal expect fun webHeaderFree(handle: Int)

internal expect fun webHeaderGetInt(handle: Int, field: Int): Int

internal expect fun webHeaderSetInt(handle: Int, field: Int, value: Int): Int

internal expect fun webHeaderString(handle: Int, which: Int): String

/** Base64 of the serialised header packet. */
internal expect fun webHeaderPacket(handle: Int): String

/* ------------------------------------------------------------------ */
/* SpeexDSP                                                           */
/* ------------------------------------------------------------------ */

internal expect fun webDspVersionString(): String

internal expect fun webDspResamplerCreate(
    channels: Int,
    inRate: Int,
    outRate: Int,
    quality: Int,
): Int

internal expect fun webDspResamplerCreateFrac(
    channels: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
    quality: Int,
): Int

internal expect fun webDspResamplerError(handle: Int): Int

internal expect fun webDspResamplerFree(handle: Int)

internal expect fun webDspResamplerChannels(handle: Int): Int

internal expect fun webDspResamplerSetRate(handle: Int, inRate: Int, outRate: Int): Int

internal expect fun webDspResamplerSetRateFrac(
    handle: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
): Int

internal expect fun webDspResamplerInputRate(handle: Int): Int

internal expect fun webDspResamplerOutputRate(handle: Int): Int

internal expect fun webDspResamplerRatioNum(handle: Int): Int

internal expect fun webDspResamplerRatioDen(handle: Int): Int

internal expect fun webDspResamplerSetQuality(handle: Int, quality: Int): Int

internal expect fun webDspResamplerQuality(handle: Int): Int

internal expect fun webDspResamplerSetInputStride(handle: Int, stride: Int)

internal expect fun webDspResamplerInputStride(handle: Int): Int

internal expect fun webDspResamplerSetOutputStride(handle: Int, stride: Int)

internal expect fun webDspResamplerOutputStride(handle: Int): Int

internal expect fun webDspResamplerInputLatency(handle: Int): Int

internal expect fun webDspResamplerOutputLatency(handle: Int): Int

internal expect fun webDspResamplerSkipZeros(handle: Int): Int

internal expect fun webDspResamplerResetMem(handle: Int): Int

internal expect fun webDspResamplerProcess(handle: Int, samples: String): Int

internal expect fun webDspResamplerOutput(handle: Int): String

internal expect fun webDspResamplerStrerror(error: Int): String

internal expect fun webDspPreprocessCreate(frameSize: Int, samplingRate: Int): Int

internal expect fun webDspPreprocessFree(handle: Int)

internal expect fun webDspPreprocessFrameSize(handle: Int): Int

/** Returns `"vad:base64"` with the processed samples. */
internal expect fun webDspPreprocessRun(handle: Int, samples: String): String

internal expect fun webDspPreprocessEstimateUpdate(handle: Int, samples: String)

internal expect fun webDspPreprocessCtlInt(handle: Int, request: Int, value: Int): Int

internal expect fun webDspPreprocessGetInt(handle: Int, request: Int): Int

internal expect fun webDspPreprocessCtlFloat(handle: Int, request: Int, value: Float): Int

internal expect fun webDspPreprocessGetFloat(handle: Int, request: Int): Float

internal expect fun webDspPreprocessSetEcho(handle: Int, echo: Int): Int

internal expect fun webDspPreprocessPsdSize(handle: Int): Int

internal expect fun webDspPreprocessPsd(handle: Int): String

internal expect fun webDspPreprocessNoisePsdSize(handle: Int): Int

internal expect fun webDspPreprocessNoisePsd(handle: Int): String

internal expect fun webDspEchoCreate(
    frameSize: Int,
    filterLength: Int,
    microphones: Int,
    speakers: Int,
): Int

internal expect fun webDspEchoFree(handle: Int)

internal expect fun webDspEchoReset(handle: Int)

internal expect fun webDspEchoFrameSize(handle: Int): Int

internal expect fun webDspEchoMicrophones(handle: Int): Int

internal expect fun webDspEchoSpeakers(handle: Int): Int

internal expect fun webDspEchoCancellation(handle: Int, rec: String, play: String): Int

internal expect fun webDspEchoCapture(handle: Int, rec: String): Int

internal expect fun webDspEchoPlayback(handle: Int, play: String): Int

internal expect fun webDspEchoOutput(handle: Int): String

internal expect fun webDspEchoCtlInt(handle: Int, request: Int, value: Int): Int

internal expect fun webDspEchoGetInt(handle: Int, request: Int): Int

internal expect fun webDspEchoImpulseResponseSize(handle: Int): Int

internal expect fun webDspEchoImpulseResponse(handle: Int): String

internal expect fun webDspDecorrCreate(rate: Int, channels: Int, frameSize: Int): Int

internal expect fun webDspDecorrFree(handle: Int)

internal expect fun webDspDecorrChannels(handle: Int): Int

internal expect fun webDspDecorrFrameSize(handle: Int): Int

internal expect fun webDspDecorrProcess(handle: Int, input: String, strength: Int): Int

internal expect fun webDspDecorrOutput(handle: Int): String

internal expect fun webDspJitterCreate(stepSize: Int): Int

internal expect fun webDspJitterFree(handle: Int)

internal expect fun webDspJitterReset(handle: Int)

internal expect fun webDspJitterPut(
    handle: Int,
    data: String,
    timestamp: Int,
    span: Int,
    sequence: Int,
    userData: Int,
): Int

internal expect fun webDspJitterGet(handle: Int, desiredSpan: Int): Int

internal expect fun webDspJitterGetAnother(handle: Int): Int

internal expect fun webDspJitterPacket(handle: Int): String

internal expect fun webDspJitterTimestamp(handle: Int): Int

internal expect fun webDspJitterSpan(handle: Int): Int

internal expect fun webDspJitterSequence(handle: Int): Int

internal expect fun webDspJitterUserData(handle: Int): Int

internal expect fun webDspJitterPointerTimestamp(handle: Int): Int

internal expect fun webDspJitterTick(handle: Int)

internal expect fun webDspJitterRemainingSpan(handle: Int, remaining: Int)

internal expect fun webDspJitterCtlInt(handle: Int, request: Int, value: Int): Int

internal expect fun webDspJitterGetInt(handle: Int, request: Int): Int

internal expect fun webDspRingBufferCreate(size: Int): Int

internal expect fun webDspRingBufferFree(handle: Int)

internal expect fun webDspRingBufferWrite(handle: Int, data: String): Int

internal expect fun webDspRingBufferWriteZeros(handle: Int, len: Int): Int

internal expect fun webDspRingBufferRead(handle: Int, len: Int): String

internal expect fun webDspRingBufferAvailable(handle: Int): Int

internal expect fun webDspRingBufferResize(handle: Int, len: Int): Int

/** Packs 16-bit samples into base64 little-endian bytes. */
internal object SpeexWebPacking {
    fun encodeShorts(samples: ShortArray, offset: Int = 0, count: Int = samples.size - offset): String {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) {
            "range $offset..${offset + count} is outside a ${samples.size}-sample buffer"
        }
        val bytes = ByteArray(count * 2)
        for (index in 0 until count) {
            val value = samples[offset + index].toInt()
            bytes[index * 2] = (value and 0xFF).toByte()
            bytes[index * 2 + 1] = ((value ushr 8) and 0xFF).toByte()
        }
        return Base64.encode(bytes)
    }

    fun decodeShorts(encoded: String): ShortArray = decodeShorts(Base64.decode(encoded))

    fun decodeShorts(bytes: ByteArray): ShortArray {
        require(bytes.size % 2 == 0) { "sample payload has an odd byte count: ${bytes.size}" }
        return ShortArray(bytes.size / 2) { index ->
            val low = bytes[index * 2].toInt() and 0xFF
            val high = bytes[index * 2 + 1].toInt() and 0xFF
            ((high shl 8) or low).toShort()
        }
    }

    fun encodeInts(values: IntArray, offset: Int = 0, count: Int = values.size - offset): String {
        require(offset >= 0 && count >= 0 && offset + count <= values.size) {
            "range $offset..${offset + count} is outside a ${values.size}-value buffer"
        }
        val bytes = ByteArray(count * 4)
        for (index in 0 until count) {
            val value = values[offset + index]
            val base = index * 4
            bytes[base] = (value and 0xFF).toByte()
            bytes[base + 1] = ((value ushr 8) and 0xFF).toByte()
            bytes[base + 2] = ((value ushr 16) and 0xFF).toByte()
            bytes[base + 3] = ((value ushr 24) and 0xFF).toByte()
        }
        return Base64.encode(bytes)
    }

    fun decodeInts(encoded: String): IntArray = decodeInts(Base64.decode(encoded))

    fun decodeInts(bytes: ByteArray): IntArray {
        require(bytes.size % 4 == 0) { "int payload has a byte count not divisible by 4: ${bytes.size}" }
        return IntArray(bytes.size / 4) { index ->
            val base = index * 4
            (bytes[base].toInt() and 0xFF) or
                ((bytes[base + 1].toInt() and 0xFF) shl 8) or
                ((bytes[base + 2].toInt() and 0xFF) shl 16) or
                ((bytes[base + 3].toInt() and 0xFF) shl 24)
        }
    }

    fun encodeBytes(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): String {
        require(offset >= 0 && count >= 0 && offset + count <= data.size) {
            "range $offset..${offset + count} is outside a ${data.size}-byte buffer"
        }
        return Base64.encode(if (offset == 0 && count == data.size) data else data.copyOfRange(offset, offset + count))
    }

    fun decodeBytes(encoded: String): ByteArray = Base64.decode(encoded)
}

/** Code part of the `"<code>:<base64>"` envelope the glue returns. */
internal fun parseWebCode(result: String): Int {
    val separator = result.indexOf(':')
    check(separator >= 0) { "malformed Speex web result: $result" }
    return result.substring(0, separator).toInt()
}

/** Payload part of the `"<code>:<base64>"` envelope; empty becomes `ByteArray(0)`. */
internal fun parseWebPayload(result: String): ByteArray {
    val separator = result.indexOf(':')
    check(separator >= 0) { "malformed Speex web result: $result" }
    val encoded = result.substring(separator + 1)
    return if (encoded.isEmpty()) ByteArray(0) else Base64.decode(encoded)
}
