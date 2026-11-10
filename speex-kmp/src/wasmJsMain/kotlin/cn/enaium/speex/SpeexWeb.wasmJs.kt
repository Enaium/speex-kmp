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

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package cn.enaium.speex

import kotlin.JsFun

private var speexWebInitialized = false

internal actual fun webInit() {
    if (!speexWebInitialized) {
        initSpeexWeb(SPEEX_WASM_BASE64)
        speexWebInitialized = true
    }
}

@JsFun(SPEEX_WEB_INIT_JS)
private external fun initSpeexWeb(wasmBase64: String)

@JsFun("() => globalThis.__speexWeb.versionString()")
private external fun versionString(): String

@JsFun("() => globalThis.__speexWeb.versionMajor()")
private external fun versionMajor(): Int

@JsFun("() => globalThis.__speexWeb.versionMinor()")
private external fun versionMinor(): Int

@JsFun("() => globalThis.__speexWeb.versionMicro()")
private external fun versionMicro(): Int

@JsFun("() => globalThis.__speexWeb.versionExtra()")
private external fun versionExtra(): String

@JsFun("(m) => globalThis.__speexWeb.modeFrameSize(m)")
private external fun modeFrameSize(mode: Int): Int

@JsFun("(m, s) => globalThis.__speexWeb.modeSubmodeBits(m, s)")
private external fun modeSubmodeBits(mode: Int, submode: Int): Int

@JsFun("(m) => globalThis.__speexWeb.modeName(m)")
private external fun modeName(mode: Int): String

@JsFun("() => globalThis.__speexWeb.bitsCreate()")
private external fun bitsCreate(): Int

@JsFun("(h) => globalThis.__speexWeb.bitsFree(h)")
private external fun bitsFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.bitsReset(h)")
private external fun bitsReset(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.bitsRewind(h)")
private external fun bitsRewind(handle: Int)

@JsFun("(h, d) => globalThis.__speexWeb.bitsReadFrom(h, d)")
private external fun bitsReadFrom(handle: Int, data: String): Int

@JsFun("(h, d) => globalThis.__speexWeb.bitsReadWholeBytes(h, d)")
private external fun bitsReadWholeBytes(handle: Int, data: String): Int

@JsFun("(h, d, n) => globalThis.__speexWeb.bitsPack(h, d, n)")
private external fun bitsPack(handle: Int, data: Int, nbits: Int)

@JsFun("(h, n) => globalThis.__speexWeb.bitsUnpackSigned(h, n)")
private external fun bitsUnpackSigned(handle: Int, nbits: Int): Int

@JsFun("(h, n) => globalThis.__speexWeb.bitsUnpackUnsigned(h, n)")
private external fun bitsUnpackUnsigned(handle: Int, nbits: Int): Int

@JsFun("(h) => globalThis.__speexWeb.bitsPeek(h)")
private external fun bitsPeek(handle: Int): Int

@JsFun("(h, n) => globalThis.__speexWeb.bitsPeekUnsigned(h, n)")
private external fun bitsPeekUnsigned(handle: Int, nbits: Int): Int

@JsFun("(h, n) => globalThis.__speexWeb.bitsAdvance(h, n)")
private external fun bitsAdvance(handle: Int, nbits: Int)

@JsFun("(h) => globalThis.__speexWeb.bitsRemaining(h)")
private external fun bitsRemaining(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.bitsNbytes(h)")
private external fun bitsNbytes(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.bitsInsertTerminator(h)")
private external fun bitsInsertTerminator(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.bitsWrite(h)")
private external fun bitsWrite(handle: Int): String

@JsFun("(m) => globalThis.__speexWeb.encoderCreate(m)")
private external fun encoderCreate(mode: Int): Int

@JsFun("(h) => globalThis.__speexWeb.encoderFree(h)")
private external fun encoderFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.encoderFrameSize(h)")
private external fun encoderFrameSize(handle: Int): Int

@JsFun("(h, s) => globalThis.__speexWeb.encoderEncode(h, s)")
private external fun encoderEncode(handle: Int, samples: String): Int

@JsFun("(h, s) => globalThis.__speexWeb.encoderEncodeStereo(h, s)")
private external fun encoderEncodeStereo(handle: Int, samples: String): String

@JsFun("(h) => globalThis.__speexWeb.encoderPacket(h)")
private external fun encoderPacket(handle: Int): String

@JsFun("(h, r, v) => globalThis.__speexWeb.encoderCtl(h, r, v)")
private external fun encoderCtl(handle: Int, request: Int, value: Int): Int

@JsFun("(h, r) => globalThis.__speexWeb.encoderGet(h, r)")
private external fun encoderGet(handle: Int, request: Int): Int

@JsFun("(h, v) => globalThis.__speexWeb.encoderSetVbrQuality(h, v)")
private external fun encoderSetVbrQuality(handle: Int, value: Float): Int

@JsFun("(h) => globalThis.__speexWeb.encoderGetVbrQuality(h)")
private external fun encoderGetVbrQuality(handle: Int): Float

@JsFun("(h) => globalThis.__speexWeb.encoderGetRelativeQuality(h)")
private external fun encoderGetRelativeQuality(handle: Int): Float

@JsFun("(m) => globalThis.__speexWeb.decoderCreate(m)")
private external fun decoderCreate(mode: Int): Int

@JsFun("(h) => globalThis.__speexWeb.decoderFree(h)")
private external fun decoderFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.decoderFrameSize(h)")
private external fun decoderFrameSize(handle: Int): Int

@JsFun("(h, p) => globalThis.__speexWeb.decoderDecode(h, p)")
private external fun decoderDecode(handle: Int, packet: String): Int

@JsFun("(h, p, s) => globalThis.__speexWeb.decoderDecodeStereo(h, p, s)")
private external fun decoderDecodeStereo(handle: Int, packet: String, stereo: Int): Int

@JsFun("(h) => globalThis.__speexWeb.decoderSamples(h)")
private external fun decoderSamples(handle: Int): String

@JsFun("(h, r, v) => globalThis.__speexWeb.decoderCtl(h, r, v)")
private external fun decoderCtl(handle: Int, request: Int, value: Int): Int

@JsFun("(h, r) => globalThis.__speexWeb.decoderGet(h, r)")
private external fun decoderGet(handle: Int, request: Int): Int

@JsFun("() => globalThis.__speexWeb.stereoCreate()")
private external fun stereoCreate(): Int

@JsFun("(h) => globalThis.__speexWeb.stereoFree(h)")
private external fun stereoFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.stereoReset(h)")
private external fun stereoReset(handle: Int)

@JsFun("(h, s, o, c, b) => globalThis.__speexWeb.stereoEncode(h, s, o, c, b)")
private external fun stereoEncode(handle: Int, samples: String, offset: Int, count: Int, bits: Int): String

@JsFun("(h, s, c) => globalThis.__speexWeb.stereoDecode(h, s, c)")
private external fun stereoDecode(handle: Int, samples: String, count: Int): String

@JsFun("(r, c, m) => globalThis.__speexWeb.headerCreate(r, c, m)")
private external fun headerCreate(rate: Int, channels: Int, mode: Int): Int

@JsFun("(p) => globalThis.__speexWeb.headerParse(p)")
private external fun headerParse(packet: String): Int

@JsFun("(h) => globalThis.__speexWeb.headerFree(h)")
private external fun headerFree(handle: Int)

@JsFun("(h, f) => globalThis.__speexWeb.headerGetInt(h, f)")
private external fun headerGetInt(handle: Int, field: Int): Int

@JsFun("(h, f, v) => globalThis.__speexWeb.headerSetInt(h, f, v)")
private external fun headerSetInt(handle: Int, field: Int, value: Int): Int

@JsFun("(h, w) => globalThis.__speexWeb.headerString(h, w)")
private external fun headerString(handle: Int, which: Int): String

@JsFun("(h) => globalThis.__speexWeb.headerPacket(h)")
private external fun headerPacket(handle: Int): String

@JsFun("() => globalThis.__speexWeb.dspVersionString()")
private external fun dspVersionString(): String

@JsFun("(c, i, o, q) => globalThis.__speexWeb.dspResamplerCreate(c, i, o, q)")
private external fun dspResamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): Int

@JsFun("(c, n, d, i, o, q) => globalThis.__speexWeb.dspResamplerCreateFrac(c, n, d, i, o, q)")
private external fun dspResamplerCreateFrac(
    channels: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
    quality: Int,
): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerError(h)")
private external fun dspResamplerError(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerFree(h)")
private external fun dspResamplerFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspResamplerChannels(h)")
private external fun dspResamplerChannels(handle: Int): Int

@JsFun("(h, i, o) => globalThis.__speexWeb.dspResamplerSetRate(h, i, o)")
private external fun dspResamplerSetRate(handle: Int, inRate: Int, outRate: Int): Int

@JsFun("(h, n, d, i, o) => globalThis.__speexWeb.dspResamplerSetRateFrac(h, n, d, i, o)")
private external fun dspResamplerSetRateFrac(
    handle: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerInputRate(h)")
private external fun dspResamplerInputRate(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerOutputRate(h)")
private external fun dspResamplerOutputRate(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerRatioNum(h)")
private external fun dspResamplerRatioNum(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerRatioDen(h)")
private external fun dspResamplerRatioDen(handle: Int): Int

@JsFun("(h, q) => globalThis.__speexWeb.dspResamplerSetQuality(h, q)")
private external fun dspResamplerSetQuality(handle: Int, quality: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerQuality(h)")
private external fun dspResamplerQuality(handle: Int): Int

@JsFun("(h, s) => globalThis.__speexWeb.dspResamplerSetInputStride(h, s)")
private external fun dspResamplerSetInputStride(handle: Int, stride: Int)

@JsFun("(h) => globalThis.__speexWeb.dspResamplerInputStride(h)")
private external fun dspResamplerInputStride(handle: Int): Int

@JsFun("(h, s) => globalThis.__speexWeb.dspResamplerSetOutputStride(h, s)")
private external fun dspResamplerSetOutputStride(handle: Int, stride: Int)

@JsFun("(h) => globalThis.__speexWeb.dspResamplerOutputStride(h)")
private external fun dspResamplerOutputStride(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerInputLatency(h)")
private external fun dspResamplerInputLatency(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerOutputLatency(h)")
private external fun dspResamplerOutputLatency(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerSkipZeros(h)")
private external fun dspResamplerSkipZeros(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerResetMem(h)")
private external fun dspResamplerResetMem(handle: Int): Int

@JsFun("(h, s) => globalThis.__speexWeb.dspResamplerProcess(h, s)")
private external fun dspResamplerProcess(handle: Int, samples: String): Int

@JsFun("(h) => globalThis.__speexWeb.dspResamplerOutput(h)")
private external fun dspResamplerOutput(handle: Int): String

@JsFun("(e) => globalThis.__speexWeb.dspResamplerStrerror(e)")
private external fun dspResamplerStrerror(error: Int): String

@JsFun("(f, r) => globalThis.__speexWeb.dspPreprocessCreate(f, r)")
private external fun dspPreprocessCreate(frameSize: Int, samplingRate: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessFree(h)")
private external fun dspPreprocessFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessFrameSize(h)")
private external fun dspPreprocessFrameSize(handle: Int): Int

@JsFun("(h, s) => globalThis.__speexWeb.dspPreprocessRun(h, s)")
private external fun dspPreprocessRun(handle: Int, samples: String): String

@JsFun("(h, s) => globalThis.__speexWeb.dspPreprocessEstimateUpdate(h, s)")
private external fun dspPreprocessEstimateUpdate(handle: Int, samples: String)

@JsFun("(h, r, v) => globalThis.__speexWeb.dspPreprocessCtlInt(h, r, v)")
private external fun dspPreprocessCtlInt(handle: Int, request: Int, value: Int): Int

@JsFun("(h, r) => globalThis.__speexWeb.dspPreprocessGetInt(h, r)")
private external fun dspPreprocessGetInt(handle: Int, request: Int): Int

@JsFun("(h, r, v) => globalThis.__speexWeb.dspPreprocessCtlFloat(h, r, v)")
private external fun dspPreprocessCtlFloat(handle: Int, request: Int, value: Float): Int

@JsFun("(h, r) => globalThis.__speexWeb.dspPreprocessGetFloat(h, r)")
private external fun dspPreprocessGetFloat(handle: Int, request: Int): Float

@JsFun("(h, e) => globalThis.__speexWeb.dspPreprocessSetEcho(h, e)")
private external fun dspPreprocessSetEcho(handle: Int, echo: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessPsdSize(h)")
private external fun dspPreprocessPsdSize(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessPsd(h)")
private external fun dspPreprocessPsd(handle: Int): String

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessNoisePsdSize(h)")
private external fun dspPreprocessNoisePsdSize(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspPreprocessNoisePsd(h)")
private external fun dspPreprocessNoisePsd(handle: Int): String

@JsFun("(f, l, m, s) => globalThis.__speexWeb.dspEchoCreate(f, l, m, s)")
private external fun dspEchoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoFree(h)")
private external fun dspEchoFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspEchoReset(h)")
private external fun dspEchoReset(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspEchoFrameSize(h)")
private external fun dspEchoFrameSize(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoMicrophones(h)")
private external fun dspEchoMicrophones(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoSpeakers(h)")
private external fun dspEchoSpeakers(handle: Int): Int

@JsFun("(h, r, p) => globalThis.__speexWeb.dspEchoCancellation(h, r, p)")
private external fun dspEchoCancellation(handle: Int, rec: String, play: String): Int

@JsFun("(h, r) => globalThis.__speexWeb.dspEchoCapture(h, r)")
private external fun dspEchoCapture(handle: Int, rec: String): Int

@JsFun("(h, p) => globalThis.__speexWeb.dspEchoPlayback(h, p)")
private external fun dspEchoPlayback(handle: Int, play: String): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoOutput(h)")
private external fun dspEchoOutput(handle: Int): String

@JsFun("(h, r, v) => globalThis.__speexWeb.dspEchoCtlInt(h, r, v)")
private external fun dspEchoCtlInt(handle: Int, request: Int, value: Int): Int

@JsFun("(h, r) => globalThis.__speexWeb.dspEchoGetInt(h, r)")
private external fun dspEchoGetInt(handle: Int, request: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoImpulseResponseSize(h)")
private external fun dspEchoImpulseResponseSize(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspEchoImpulseResponse(h)")
private external fun dspEchoImpulseResponse(handle: Int): String

@JsFun("(r, c, f) => globalThis.__speexWeb.dspDecorrCreate(r, c, f)")
private external fun dspDecorrCreate(rate: Int, channels: Int, frameSize: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspDecorrFree(h)")
private external fun dspDecorrFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspDecorrChannels(h)")
private external fun dspDecorrChannels(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspDecorrFrameSize(h)")
private external fun dspDecorrFrameSize(handle: Int): Int

@JsFun("(h, i, s) => globalThis.__speexWeb.dspDecorrProcess(h, i, s)")
private external fun dspDecorrProcess(handle: Int, input: String, strength: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspDecorrOutput(h)")
private external fun dspDecorrOutput(handle: Int): String

@JsFun("(s) => globalThis.__speexWeb.dspJitterCreate(s)")
private external fun dspJitterCreate(stepSize: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterFree(h)")
private external fun dspJitterFree(handle: Int)

@JsFun("(h) => globalThis.__speexWeb.dspJitterReset(h)")
private external fun dspJitterReset(handle: Int)

@JsFun("(h, d, t, s, q, u) => globalThis.__speexWeb.dspJitterPut(h, d, t, s, q, u)")
private external fun dspJitterPut(
    handle: Int,
    data: String,
    timestamp: Int,
    span: Int,
    sequence: Int,
    userData: Int,
): Int

@JsFun("(h, s) => globalThis.__speexWeb.dspJitterGet(h, s)")
private external fun dspJitterGet(handle: Int, desiredSpan: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterGetAnother(h)")
private external fun dspJitterGetAnother(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterPacket(h)")
private external fun dspJitterPacket(handle: Int): String

@JsFun("(h) => globalThis.__speexWeb.dspJitterTimestamp(h)")
private external fun dspJitterTimestamp(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterSpan(h)")
private external fun dspJitterSpan(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterSequence(h)")
private external fun dspJitterSequence(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterUserData(h)")
private external fun dspJitterUserData(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterPointerTimestamp(h)")
private external fun dspJitterPointerTimestamp(handle: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspJitterTick(h)")
private external fun dspJitterTick(handle: Int)

@JsFun("(h, r) => globalThis.__speexWeb.dspJitterRemainingSpan(h, r)")
private external fun dspJitterRemainingSpan(handle: Int, remaining: Int)

@JsFun("(h, r, v) => globalThis.__speexWeb.dspJitterCtlInt(h, r, v)")
private external fun dspJitterCtlInt(handle: Int, request: Int, value: Int): Int

@JsFun("(h, r) => globalThis.__speexWeb.dspJitterGetInt(h, r)")
private external fun dspJitterGetInt(handle: Int, request: Int): Int

@JsFun("(s) => globalThis.__speexWeb.dspRingBufferCreate(s)")
private external fun dspRingBufferCreate(size: Int): Int

@JsFun("(h) => globalThis.__speexWeb.dspRingBufferFree(h)")
private external fun dspRingBufferFree(handle: Int)

@JsFun("(h, d) => globalThis.__speexWeb.dspRingBufferWrite(h, d)")
private external fun dspRingBufferWrite(handle: Int, data: String): Int

@JsFun("(h, l) => globalThis.__speexWeb.dspRingBufferWriteZeros(h, l)")
private external fun dspRingBufferWriteZeros(handle: Int, len: Int): Int

@JsFun("(h, l) => globalThis.__speexWeb.dspRingBufferRead(h, l)")
private external fun dspRingBufferRead(handle: Int, len: Int): String

@JsFun("(h) => globalThis.__speexWeb.dspRingBufferAvailable(h)")
private external fun dspRingBufferAvailable(handle: Int): Int

@JsFun("(h, l) => globalThis.__speexWeb.dspRingBufferResize(h, l)")
private external fun dspRingBufferResize(handle: Int, len: Int): Int

internal actual fun webVersionString(): String = versionString()

internal actual fun webVersionMajor(): Int = versionMajor()

internal actual fun webVersionMinor(): Int = versionMinor()

internal actual fun webVersionMicro(): Int = versionMicro()

internal actual fun webVersionExtra(): String = versionExtra()

internal actual fun webModeFrameSize(mode: Int): Int = modeFrameSize(mode)

internal actual fun webModeSubmodeBits(mode: Int, submode: Int): Int = modeSubmodeBits(mode, submode)

internal actual fun webModeName(mode: Int): String = modeName(mode)

internal actual fun webBitsCreate(): Int = bitsCreate()

internal actual fun webBitsFree(handle: Int) = bitsFree(handle)

internal actual fun webBitsReset(handle: Int) = bitsReset(handle)

internal actual fun webBitsRewind(handle: Int) = bitsRewind(handle)

internal actual fun webBitsReadFrom(handle: Int, data: String): Int = bitsReadFrom(handle, data)

internal actual fun webBitsReadWholeBytes(handle: Int, data: String): Int =
    bitsReadWholeBytes(handle, data)

internal actual fun webBitsPack(handle: Int, data: Int, nbits: Int) = bitsPack(handle, data, nbits)

internal actual fun webBitsUnpackSigned(handle: Int, nbits: Int): Int =
    bitsUnpackSigned(handle, nbits)

internal actual fun webBitsUnpackUnsigned(handle: Int, nbits: Int): Int =
    bitsUnpackUnsigned(handle, nbits)

internal actual fun webBitsPeek(handle: Int): Int = bitsPeek(handle)

internal actual fun webBitsPeekUnsigned(handle: Int, nbits: Int): Int =
    bitsPeekUnsigned(handle, nbits)

internal actual fun webBitsAdvance(handle: Int, nbits: Int) = bitsAdvance(handle, nbits)

internal actual fun webBitsRemaining(handle: Int): Int = bitsRemaining(handle)

internal actual fun webBitsNbytes(handle: Int): Int = bitsNbytes(handle)

internal actual fun webBitsInsertTerminator(handle: Int) = bitsInsertTerminator(handle)

internal actual fun webBitsWrite(handle: Int): String = bitsWrite(handle)

internal actual fun webEncoderCreate(mode: Int): Int = encoderCreate(mode)

internal actual fun webEncoderFree(handle: Int) = encoderFree(handle)

internal actual fun webEncoderFrameSize(handle: Int): Int = encoderFrameSize(handle)

internal actual fun webEncoderEncode(handle: Int, samples: String): Int = encoderEncode(handle, samples)

internal actual fun webEncoderEncodeStereo(handle: Int, samples: String): String =
    encoderEncodeStereo(handle, samples)

internal actual fun webEncoderPacket(handle: Int): String = encoderPacket(handle)

internal actual fun webEncoderCtl(handle: Int, request: Int, value: Int): Int =
    encoderCtl(handle, request, value)

internal actual fun webEncoderGet(handle: Int, request: Int): Int = encoderGet(handle, request)

internal actual fun webEncoderSetVbrQuality(handle: Int, value: Float): Int =
    encoderSetVbrQuality(handle, value)

internal actual fun webEncoderGetVbrQuality(handle: Int): Float = encoderGetVbrQuality(handle)

internal actual fun webEncoderGetRelativeQuality(handle: Int): Float =
    encoderGetRelativeQuality(handle)

internal actual fun webDecoderCreate(mode: Int): Int = decoderCreate(mode)

internal actual fun webDecoderFree(handle: Int) = decoderFree(handle)

internal actual fun webDecoderFrameSize(handle: Int): Int = decoderFrameSize(handle)

internal actual fun webDecoderDecode(handle: Int, packet: String): Int = decoderDecode(handle, packet)

internal actual fun webDecoderDecodeStereo(handle: Int, packet: String, stereo: Int): Int =
    decoderDecodeStereo(handle, packet, stereo)

internal actual fun webDecoderSamples(handle: Int): String = decoderSamples(handle)

internal actual fun webDecoderCtl(handle: Int, request: Int, value: Int): Int =
    decoderCtl(handle, request, value)

internal actual fun webDecoderGet(handle: Int, request: Int): Int = decoderGet(handle, request)

internal actual fun webStereoCreate(): Int = stereoCreate()

internal actual fun webStereoFree(handle: Int) = stereoFree(handle)

internal actual fun webStereoReset(handle: Int) = stereoReset(handle)

internal actual fun webStereoEncode(handle: Int, samples: String, offset: Int, count: Int, bits: Int): String =
    stereoEncode(handle, samples, offset, count, bits)

internal actual fun webStereoDecode(handle: Int, samples: String, count: Int): String =
    stereoDecode(handle, samples, count)

internal actual fun webHeaderCreate(rate: Int, channels: Int, mode: Int): Int =
    headerCreate(rate, channels, mode)

internal actual fun webHeaderParse(packet: String): Int = headerParse(packet)

internal actual fun webHeaderFree(handle: Int) = headerFree(handle)

internal actual fun webHeaderGetInt(handle: Int, field: Int): Int = headerGetInt(handle, field)

internal actual fun webHeaderSetInt(handle: Int, field: Int, value: Int): Int =
    headerSetInt(handle, field, value)

internal actual fun webHeaderString(handle: Int, which: Int): String = headerString(handle, which)

internal actual fun webHeaderPacket(handle: Int): String = headerPacket(handle)

internal actual fun webDspVersionString(): String = dspVersionString()

internal actual fun webDspResamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): Int =
    dspResamplerCreate(channels, inRate, outRate, quality)

internal actual fun webDspResamplerCreateFrac(
    channels: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
    quality: Int,
): Int = dspResamplerCreateFrac(channels, ratioNum, ratioDen, inRate, outRate, quality)

internal actual fun webDspResamplerError(handle: Int): Int = dspResamplerError(handle)

internal actual fun webDspResamplerFree(handle: Int) = dspResamplerFree(handle)

internal actual fun webDspResamplerChannels(handle: Int): Int = dspResamplerChannels(handle)

internal actual fun webDspResamplerSetRate(handle: Int, inRate: Int, outRate: Int): Int =
    dspResamplerSetRate(handle, inRate, outRate)

internal actual fun webDspResamplerSetRateFrac(
    handle: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
): Int = dspResamplerSetRateFrac(handle, ratioNum, ratioDen, inRate, outRate)

internal actual fun webDspResamplerInputRate(handle: Int): Int = dspResamplerInputRate(handle)

internal actual fun webDspResamplerOutputRate(handle: Int): Int = dspResamplerOutputRate(handle)

internal actual fun webDspResamplerRatioNum(handle: Int): Int = dspResamplerRatioNum(handle)

internal actual fun webDspResamplerRatioDen(handle: Int): Int = dspResamplerRatioDen(handle)

internal actual fun webDspResamplerSetQuality(handle: Int, quality: Int): Int =
    dspResamplerSetQuality(handle, quality)

internal actual fun webDspResamplerQuality(handle: Int): Int = dspResamplerQuality(handle)

internal actual fun webDspResamplerSetInputStride(handle: Int, stride: Int) =
    dspResamplerSetInputStride(handle, stride)

internal actual fun webDspResamplerInputStride(handle: Int): Int = dspResamplerInputStride(handle)

internal actual fun webDspResamplerSetOutputStride(handle: Int, stride: Int) =
    dspResamplerSetOutputStride(handle, stride)

internal actual fun webDspResamplerOutputStride(handle: Int): Int = dspResamplerOutputStride(handle)

internal actual fun webDspResamplerInputLatency(handle: Int): Int = dspResamplerInputLatency(handle)

internal actual fun webDspResamplerOutputLatency(handle: Int): Int = dspResamplerOutputLatency(handle)

internal actual fun webDspResamplerSkipZeros(handle: Int): Int = dspResamplerSkipZeros(handle)

internal actual fun webDspResamplerResetMem(handle: Int): Int = dspResamplerResetMem(handle)

internal actual fun webDspResamplerProcess(handle: Int, samples: String): Int =
    dspResamplerProcess(handle, samples)

internal actual fun webDspResamplerOutput(handle: Int): String = dspResamplerOutput(handle)

internal actual fun webDspResamplerStrerror(error: Int): String = dspResamplerStrerror(error)

internal actual fun webDspPreprocessCreate(frameSize: Int, samplingRate: Int): Int =
    dspPreprocessCreate(frameSize, samplingRate)

internal actual fun webDspPreprocessFree(handle: Int) = dspPreprocessFree(handle)

internal actual fun webDspPreprocessFrameSize(handle: Int): Int = dspPreprocessFrameSize(handle)

internal actual fun webDspPreprocessRun(handle: Int, samples: String): String =
    dspPreprocessRun(handle, samples)

internal actual fun webDspPreprocessEstimateUpdate(handle: Int, samples: String) =
    dspPreprocessEstimateUpdate(handle, samples)

internal actual fun webDspPreprocessCtlInt(handle: Int, request: Int, value: Int): Int =
    dspPreprocessCtlInt(handle, request, value)

internal actual fun webDspPreprocessGetInt(handle: Int, request: Int): Int =
    dspPreprocessGetInt(handle, request)

internal actual fun webDspPreprocessCtlFloat(handle: Int, request: Int, value: Float): Int =
    dspPreprocessCtlFloat(handle, request, value)

internal actual fun webDspPreprocessGetFloat(handle: Int, request: Int): Float =
    dspPreprocessGetFloat(handle, request)

internal actual fun webDspPreprocessSetEcho(handle: Int, echo: Int): Int =
    dspPreprocessSetEcho(handle, echo)

internal actual fun webDspPreprocessPsdSize(handle: Int): Int = dspPreprocessPsdSize(handle)

internal actual fun webDspPreprocessPsd(handle: Int): String = dspPreprocessPsd(handle)

internal actual fun webDspPreprocessNoisePsdSize(handle: Int): Int = dspPreprocessNoisePsdSize(handle)

internal actual fun webDspPreprocessNoisePsd(handle: Int): String = dspPreprocessNoisePsd(handle)

internal actual fun webDspEchoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): Int =
    dspEchoCreate(frameSize, filterLength, microphones, speakers)

internal actual fun webDspEchoFree(handle: Int) = dspEchoFree(handle)

internal actual fun webDspEchoReset(handle: Int) = dspEchoReset(handle)

internal actual fun webDspEchoFrameSize(handle: Int): Int = dspEchoFrameSize(handle)

internal actual fun webDspEchoMicrophones(handle: Int): Int = dspEchoMicrophones(handle)

internal actual fun webDspEchoSpeakers(handle: Int): Int = dspEchoSpeakers(handle)

internal actual fun webDspEchoCancellation(handle: Int, rec: String, play: String): Int =
    dspEchoCancellation(handle, rec, play)

internal actual fun webDspEchoCapture(handle: Int, rec: String): Int = dspEchoCapture(handle, rec)

internal actual fun webDspEchoPlayback(handle: Int, play: String): Int = dspEchoPlayback(handle, play)

internal actual fun webDspEchoOutput(handle: Int): String = dspEchoOutput(handle)

internal actual fun webDspEchoCtlInt(handle: Int, request: Int, value: Int): Int =
    dspEchoCtlInt(handle, request, value)

internal actual fun webDspEchoGetInt(handle: Int, request: Int): Int = dspEchoGetInt(handle, request)

internal actual fun webDspEchoImpulseResponseSize(handle: Int): Int = dspEchoImpulseResponseSize(handle)

internal actual fun webDspEchoImpulseResponse(handle: Int): String = dspEchoImpulseResponse(handle)

internal actual fun webDspDecorrCreate(rate: Int, channels: Int, frameSize: Int): Int =
    dspDecorrCreate(rate, channels, frameSize)

internal actual fun webDspDecorrFree(handle: Int) = dspDecorrFree(handle)

internal actual fun webDspDecorrChannels(handle: Int): Int = dspDecorrChannels(handle)

internal actual fun webDspDecorrFrameSize(handle: Int): Int = dspDecorrFrameSize(handle)

internal actual fun webDspDecorrProcess(handle: Int, input: String, strength: Int): Int =
    dspDecorrProcess(handle, input, strength)

internal actual fun webDspDecorrOutput(handle: Int): String = dspDecorrOutput(handle)

internal actual fun webDspJitterCreate(stepSize: Int): Int = dspJitterCreate(stepSize)

internal actual fun webDspJitterFree(handle: Int) = dspJitterFree(handle)

internal actual fun webDspJitterReset(handle: Int) = dspJitterReset(handle)

internal actual fun webDspJitterPut(
    handle: Int,
    data: String,
    timestamp: Int,
    span: Int,
    sequence: Int,
    userData: Int,
): Int = dspJitterPut(handle, data, timestamp, span, sequence, userData)

internal actual fun webDspJitterGet(handle: Int, desiredSpan: Int): Int = dspJitterGet(handle, desiredSpan)

internal actual fun webDspJitterGetAnother(handle: Int): Int = dspJitterGetAnother(handle)

internal actual fun webDspJitterPacket(handle: Int): String = dspJitterPacket(handle)

internal actual fun webDspJitterTimestamp(handle: Int): Int = dspJitterTimestamp(handle)

internal actual fun webDspJitterSpan(handle: Int): Int = dspJitterSpan(handle)

internal actual fun webDspJitterSequence(handle: Int): Int = dspJitterSequence(handle)

internal actual fun webDspJitterUserData(handle: Int): Int = dspJitterUserData(handle)

internal actual fun webDspJitterPointerTimestamp(handle: Int): Int = dspJitterPointerTimestamp(handle)

internal actual fun webDspJitterTick(handle: Int) = dspJitterTick(handle)

internal actual fun webDspJitterRemainingSpan(handle: Int, remaining: Int) =
    dspJitterRemainingSpan(handle, remaining)

internal actual fun webDspJitterCtlInt(handle: Int, request: Int, value: Int): Int =
    dspJitterCtlInt(handle, request, value)

internal actual fun webDspJitterGetInt(handle: Int, request: Int): Int = dspJitterGetInt(handle, request)

internal actual fun webDspRingBufferCreate(size: Int): Int = dspRingBufferCreate(size)

internal actual fun webDspRingBufferFree(handle: Int) = dspRingBufferFree(handle)

internal actual fun webDspRingBufferWrite(handle: Int, data: String): Int =
    dspRingBufferWrite(handle, data)

internal actual fun webDspRingBufferWriteZeros(handle: Int, len: Int): Int =
    dspRingBufferWriteZeros(handle, len)

internal actual fun webDspRingBufferRead(handle: Int, len: Int): String = dspRingBufferRead(handle, len)

internal actual fun webDspRingBufferAvailable(handle: Int): Int = dspRingBufferAvailable(handle)

internal actual fun webDspRingBufferResize(handle: Int, len: Int): Int = dspRingBufferResize(handle, len)
