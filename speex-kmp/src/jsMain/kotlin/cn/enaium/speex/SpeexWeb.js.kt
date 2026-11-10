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

import kotlin.js.js

// The glue is instantiated lazily: decoding and compiling the embedded module
// is the only expensive step, and it should not run for a process that never
// calls Speex.
private val speexWebInitialized: Unit by lazy {
    js(SPEEX_WEB_INIT_JS)(SPEEX_WASM_BASE64)
}

internal actual fun webInit() {
    speexWebInitialized
}

internal actual fun webVersionString(): String =
    js("() => globalThis.__speexWeb.versionString()")() as String

internal actual fun webVersionMajor(): Int =
    js("() => globalThis.__speexWeb.versionMajor()")() as Int

internal actual fun webVersionMinor(): Int =
    js("() => globalThis.__speexWeb.versionMinor()")() as Int

internal actual fun webVersionMicro(): Int =
    js("() => globalThis.__speexWeb.versionMicro()")() as Int

internal actual fun webVersionExtra(): String =
    js("() => globalThis.__speexWeb.versionExtra()")() as String

internal actual fun webModeFrameSize(mode: Int): Int =
    js("(m) => globalThis.__speexWeb.modeFrameSize(m)")(mode) as Int

internal actual fun webModeSubmodeBits(mode: Int, submode: Int): Int =
    js("(m, s) => globalThis.__speexWeb.modeSubmodeBits(m, s)")(mode, submode) as Int

internal actual fun webModeName(mode: Int): String =
    js("(m) => globalThis.__speexWeb.modeName(m)")(mode) as String

internal actual fun webBitsCreate(): Int =
    js("() => globalThis.__speexWeb.bitsCreate()")() as Int

internal actual fun webBitsFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.bitsFree(h)")(handle)
}

internal actual fun webBitsReset(handle: Int) {
    js("(h) => globalThis.__speexWeb.bitsReset(h)")(handle)
}

internal actual fun webBitsRewind(handle: Int) {
    js("(h) => globalThis.__speexWeb.bitsRewind(h)")(handle)
}

internal actual fun webBitsReadFrom(handle: Int, data: String): Int =
    js("(h, d) => globalThis.__speexWeb.bitsReadFrom(h, d)")(handle, data) as Int

internal actual fun webBitsReadWholeBytes(handle: Int, data: String): Int =
    js("(h, d) => globalThis.__speexWeb.bitsReadWholeBytes(h, d)")(handle, data) as Int

internal actual fun webBitsPack(handle: Int, data: Int, nbits: Int) {
    js("(h, d, n) => globalThis.__speexWeb.bitsPack(h, d, n)")(handle, data, nbits)
}

internal actual fun webBitsUnpackSigned(handle: Int, nbits: Int): Int =
    js("(h, n) => globalThis.__speexWeb.bitsUnpackSigned(h, n)")(handle, nbits) as Int

internal actual fun webBitsUnpackUnsigned(handle: Int, nbits: Int): Int =
    js("(h, n) => globalThis.__speexWeb.bitsUnpackUnsigned(h, n)")(handle, nbits) as Int

internal actual fun webBitsPeek(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.bitsPeek(h)")(handle) as Int

internal actual fun webBitsPeekUnsigned(handle: Int, nbits: Int): Int =
    js("(h, n) => globalThis.__speexWeb.bitsPeekUnsigned(h, n)")(handle, nbits) as Int

internal actual fun webBitsAdvance(handle: Int, nbits: Int) {
    js("(h, n) => globalThis.__speexWeb.bitsAdvance(h, n)")(handle, nbits)
}

internal actual fun webBitsRemaining(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.bitsRemaining(h)")(handle) as Int

internal actual fun webBitsNbytes(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.bitsNbytes(h)")(handle) as Int

internal actual fun webBitsInsertTerminator(handle: Int) {
    js("(h) => globalThis.__speexWeb.bitsInsertTerminator(h)")(handle)
}

internal actual fun webBitsWrite(handle: Int): String =
    js("(h) => globalThis.__speexWeb.bitsWrite(h)")(handle) as String

internal actual fun webEncoderCreate(mode: Int): Int =
    js("(m) => globalThis.__speexWeb.encoderCreate(m)")(mode) as Int

internal actual fun webEncoderFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.encoderFree(h)")(handle)
}

internal actual fun webEncoderFrameSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.encoderFrameSize(h)")(handle) as Int

internal actual fun webEncoderEncode(handle: Int, samples: String): Int =
    js("(h, s) => globalThis.__speexWeb.encoderEncode(h, s)")(handle, samples) as Int

internal actual fun webEncoderEncodeStereo(handle: Int, samples: String): String =
    js("(h, s) => globalThis.__speexWeb.encoderEncodeStereo(h, s)")(handle, samples) as String

internal actual fun webEncoderPacket(handle: Int): String =
    js("(h) => globalThis.__speexWeb.encoderPacket(h)")(handle) as String

internal actual fun webEncoderCtl(handle: Int, request: Int, value: Int): Int =
    js("(h, r, v) => globalThis.__speexWeb.encoderCtl(h, r, v)")(handle, request, value) as Int

internal actual fun webEncoderGet(handle: Int, request: Int): Int =
    js("(h, r) => globalThis.__speexWeb.encoderGet(h, r)")(handle, request) as Int

internal actual fun webEncoderSetVbrQuality(handle: Int, value: Float): Int =
    js("(h, v) => globalThis.__speexWeb.encoderSetVbrQuality(h, v)")(handle, value) as Int

internal actual fun webEncoderGetVbrQuality(handle: Int): Float =
    (js("(h) => globalThis.__speexWeb.encoderGetVbrQuality(h)")(handle) as Double).toFloat()

internal actual fun webEncoderGetRelativeQuality(handle: Int): Float =
    (js("(h) => globalThis.__speexWeb.encoderGetRelativeQuality(h)")(handle) as Double).toFloat()

internal actual fun webDecoderCreate(mode: Int): Int =
    js("(m) => globalThis.__speexWeb.decoderCreate(m)")(mode) as Int

internal actual fun webDecoderFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.decoderFree(h)")(handle)
}

internal actual fun webDecoderFrameSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.decoderFrameSize(h)")(handle) as Int

internal actual fun webDecoderDecode(handle: Int, packet: String): Int =
    js("(h, p) => globalThis.__speexWeb.decoderDecode(h, p)")(handle, packet) as Int

internal actual fun webDecoderDecodeStereo(handle: Int, packet: String, stereo: Int): Int =
    js("(h, p, s) => globalThis.__speexWeb.decoderDecodeStereo(h, p, s)")(handle, packet, stereo) as Int

internal actual fun webDecoderSamples(handle: Int): String =
    js("(h) => globalThis.__speexWeb.decoderSamples(h)")(handle) as String

internal actual fun webDecoderCtl(handle: Int, request: Int, value: Int): Int =
    js("(h, r, v) => globalThis.__speexWeb.decoderCtl(h, r, v)")(handle, request, value) as Int

internal actual fun webDecoderGet(handle: Int, request: Int): Int =
    js("(h, r) => globalThis.__speexWeb.decoderGet(h, r)")(handle, request) as Int

internal actual fun webStereoCreate(): Int =
    js("() => globalThis.__speexWeb.stereoCreate()")() as Int

internal actual fun webStereoFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.stereoFree(h)")(handle)
}

internal actual fun webStereoReset(handle: Int) {
    js("(h) => globalThis.__speexWeb.stereoReset(h)")(handle)
}

internal actual fun webStereoEncode(handle: Int, samples: String, offset: Int, count: Int, bits: Int): String =
    js("(h, s, o, c, b) => globalThis.__speexWeb.stereoEncode(h, s, o, c, b)")(
        handle,
        samples,
        offset,
        count,
        bits,
    ) as String

internal actual fun webStereoDecode(handle: Int, samples: String, count: Int): String =
    js("(h, s, c) => globalThis.__speexWeb.stereoDecode(h, s, c)")(handle, samples, count) as String

internal actual fun webHeaderCreate(rate: Int, channels: Int, mode: Int): Int =
    js("(r, c, m) => globalThis.__speexWeb.headerCreate(r, c, m)")(rate, channels, mode) as Int

internal actual fun webHeaderParse(packet: String): Int =
    js("(p) => globalThis.__speexWeb.headerParse(p)")(packet) as Int

internal actual fun webHeaderFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.headerFree(h)")(handle)
}

internal actual fun webHeaderGetInt(handle: Int, field: Int): Int =
    js("(h, f) => globalThis.__speexWeb.headerGetInt(h, f)")(handle, field) as Int

internal actual fun webHeaderSetInt(handle: Int, field: Int, value: Int): Int =
    js("(h, f, v) => globalThis.__speexWeb.headerSetInt(h, f, v)")(handle, field, value) as Int

internal actual fun webHeaderString(handle: Int, which: Int): String =
    js("(h, w) => globalThis.__speexWeb.headerString(h, w)")(handle, which) as String

internal actual fun webHeaderPacket(handle: Int): String =
    js("(h) => globalThis.__speexWeb.headerPacket(h)")(handle) as String

/* ------------------------------------------------------------------ */
/* SpeexDSP                                                           */
/* ------------------------------------------------------------------ */

internal actual fun webDspVersionString(): String =
    js("() => globalThis.__speexWeb.dspVersionString()")() as String

internal actual fun webDspResamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): Int =
    js("(c, i, o, q) => globalThis.__speexWeb.dspResamplerCreate(c, i, o, q)")(
        channels,
        inRate,
        outRate,
        quality,
    ) as Int

internal actual fun webDspResamplerCreateFrac(
    channels: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
    quality: Int,
): Int = js("(c, n, d, i, o, q) => globalThis.__speexWeb.dspResamplerCreateFrac(c, n, d, i, o, q)")(
    channels,
    ratioNum,
    ratioDen,
    inRate,
    outRate,
    quality,
) as Int

internal actual fun webDspResamplerError(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerError(h)")(handle) as Int

internal actual fun webDspResamplerFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspResamplerFree(h)")(handle)
}

internal actual fun webDspResamplerChannels(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerChannels(h)")(handle) as Int

internal actual fun webDspResamplerSetRate(handle: Int, inRate: Int, outRate: Int): Int =
    js("(h, i, o) => globalThis.__speexWeb.dspResamplerSetRate(h, i, o)")(handle, inRate, outRate) as Int

internal actual fun webDspResamplerSetRateFrac(
    handle: Int,
    ratioNum: Int,
    ratioDen: Int,
    inRate: Int,
    outRate: Int,
): Int = js("(h, n, d, i, o) => globalThis.__speexWeb.dspResamplerSetRateFrac(h, n, d, i, o)")(
    handle,
    ratioNum,
    ratioDen,
    inRate,
    outRate,
) as Int

internal actual fun webDspResamplerInputRate(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerInputRate(h)")(handle) as Int

internal actual fun webDspResamplerOutputRate(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerOutputRate(h)")(handle) as Int

internal actual fun webDspResamplerRatioNum(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerRatioNum(h)")(handle) as Int

internal actual fun webDspResamplerRatioDen(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerRatioDen(h)")(handle) as Int

internal actual fun webDspResamplerSetQuality(handle: Int, quality: Int): Int =
    js("(h, q) => globalThis.__speexWeb.dspResamplerSetQuality(h, q)")(handle, quality) as Int

internal actual fun webDspResamplerQuality(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerQuality(h)")(handle) as Int

internal actual fun webDspResamplerSetInputStride(handle: Int, stride: Int) {
    js("(h, s) => globalThis.__speexWeb.dspResamplerSetInputStride(h, s)")(handle, stride)
}

internal actual fun webDspResamplerInputStride(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerInputStride(h)")(handle) as Int

internal actual fun webDspResamplerSetOutputStride(handle: Int, stride: Int) {
    js("(h, s) => globalThis.__speexWeb.dspResamplerSetOutputStride(h, s)")(handle, stride)
}

internal actual fun webDspResamplerOutputStride(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerOutputStride(h)")(handle) as Int

internal actual fun webDspResamplerInputLatency(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerInputLatency(h)")(handle) as Int

internal actual fun webDspResamplerOutputLatency(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerOutputLatency(h)")(handle) as Int

internal actual fun webDspResamplerSkipZeros(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerSkipZeros(h)")(handle) as Int

internal actual fun webDspResamplerResetMem(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspResamplerResetMem(h)")(handle) as Int

internal actual fun webDspResamplerProcess(handle: Int, samples: String): Int =
    js("(h, s) => globalThis.__speexWeb.dspResamplerProcess(h, s)")(handle, samples) as Int

internal actual fun webDspResamplerOutput(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspResamplerOutput(h)")(handle) as String

internal actual fun webDspResamplerStrerror(error: Int): String =
    js("(e) => globalThis.__speexWeb.dspResamplerStrerror(e)")(error) as String

internal actual fun webDspPreprocessCreate(frameSize: Int, samplingRate: Int): Int =
    js("(f, r) => globalThis.__speexWeb.dspPreprocessCreate(f, r)")(frameSize, samplingRate) as Int

internal actual fun webDspPreprocessFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspPreprocessFree(h)")(handle)
}

internal actual fun webDspPreprocessFrameSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspPreprocessFrameSize(h)")(handle) as Int

internal actual fun webDspPreprocessRun(handle: Int, samples: String): String =
    js("(h, s) => globalThis.__speexWeb.dspPreprocessRun(h, s)")(handle, samples) as String

internal actual fun webDspPreprocessEstimateUpdate(handle: Int, samples: String) {
    js("(h, s) => globalThis.__speexWeb.dspPreprocessEstimateUpdate(h, s)")(handle, samples)
}

internal actual fun webDspPreprocessCtlInt(handle: Int, request: Int, value: Int): Int =
    js("(h, r, v) => globalThis.__speexWeb.dspPreprocessCtlInt(h, r, v)")(handle, request, value) as Int

internal actual fun webDspPreprocessGetInt(handle: Int, request: Int): Int =
    js("(h, r) => globalThis.__speexWeb.dspPreprocessGetInt(h, r)")(handle, request) as Int

internal actual fun webDspPreprocessCtlFloat(handle: Int, request: Int, value: Float): Int =
    js("(h, r, v) => globalThis.__speexWeb.dspPreprocessCtlFloat(h, r, v)")(handle, request, value) as Int

internal actual fun webDspPreprocessGetFloat(handle: Int, request: Int): Float =
    (js("(h, r) => globalThis.__speexWeb.dspPreprocessGetFloat(h, r)")(handle, request) as Double).toFloat()

internal actual fun webDspPreprocessSetEcho(handle: Int, echo: Int): Int =
    js("(h, e) => globalThis.__speexWeb.dspPreprocessSetEcho(h, e)")(handle, echo) as Int

internal actual fun webDspPreprocessPsdSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspPreprocessPsdSize(h)")(handle) as Int

internal actual fun webDspPreprocessPsd(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspPreprocessPsd(h)")(handle) as String

internal actual fun webDspPreprocessNoisePsdSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspPreprocessNoisePsdSize(h)")(handle) as Int

internal actual fun webDspPreprocessNoisePsd(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspPreprocessNoisePsd(h)")(handle) as String

internal actual fun webDspEchoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): Int =
    js("(f, l, m, s) => globalThis.__speexWeb.dspEchoCreate(f, l, m, s)")(
        frameSize,
        filterLength,
        microphones,
        speakers,
    ) as Int

internal actual fun webDspEchoFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspEchoFree(h)")(handle)
}

internal actual fun webDspEchoReset(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspEchoReset(h)")(handle)
}

internal actual fun webDspEchoFrameSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspEchoFrameSize(h)")(handle) as Int

internal actual fun webDspEchoMicrophones(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspEchoMicrophones(h)")(handle) as Int

internal actual fun webDspEchoSpeakers(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspEchoSpeakers(h)")(handle) as Int

internal actual fun webDspEchoCancellation(handle: Int, rec: String, play: String): Int =
    js("(h, r, p) => globalThis.__speexWeb.dspEchoCancellation(h, r, p)")(handle, rec, play) as Int

internal actual fun webDspEchoCapture(handle: Int, rec: String): Int =
    js("(h, r) => globalThis.__speexWeb.dspEchoCapture(h, r)")(handle, rec) as Int

internal actual fun webDspEchoPlayback(handle: Int, play: String): Int =
    js("(h, p) => globalThis.__speexWeb.dspEchoPlayback(h, p)")(handle, play) as Int

internal actual fun webDspEchoOutput(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspEchoOutput(h)")(handle) as String

internal actual fun webDspEchoCtlInt(handle: Int, request: Int, value: Int): Int =
    js("(h, r, v) => globalThis.__speexWeb.dspEchoCtlInt(h, r, v)")(handle, request, value) as Int

internal actual fun webDspEchoGetInt(handle: Int, request: Int): Int =
    js("(h, r) => globalThis.__speexWeb.dspEchoGetInt(h, r)")(handle, request) as Int

internal actual fun webDspEchoImpulseResponseSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspEchoImpulseResponseSize(h)")(handle) as Int

internal actual fun webDspEchoImpulseResponse(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspEchoImpulseResponse(h)")(handle) as String

internal actual fun webDspDecorrCreate(rate: Int, channels: Int, frameSize: Int): Int =
    js("(r, c, f) => globalThis.__speexWeb.dspDecorrCreate(r, c, f)")(rate, channels, frameSize) as Int

internal actual fun webDspDecorrFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspDecorrFree(h)")(handle)
}

internal actual fun webDspDecorrChannels(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspDecorrChannels(h)")(handle) as Int

internal actual fun webDspDecorrFrameSize(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspDecorrFrameSize(h)")(handle) as Int

internal actual fun webDspDecorrProcess(handle: Int, input: String, strength: Int): Int =
    js("(h, i, s) => globalThis.__speexWeb.dspDecorrProcess(h, i, s)")(handle, input, strength) as Int

internal actual fun webDspDecorrOutput(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspDecorrOutput(h)")(handle) as String

internal actual fun webDspJitterCreate(stepSize: Int): Int =
    js("(s) => globalThis.__speexWeb.dspJitterCreate(s)")(stepSize) as Int

internal actual fun webDspJitterFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspJitterFree(h)")(handle)
}

internal actual fun webDspJitterReset(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspJitterReset(h)")(handle)
}

internal actual fun webDspJitterPut(
    handle: Int,
    data: String,
    timestamp: Int,
    span: Int,
    sequence: Int,
    userData: Int,
): Int = js("(h, d, t, s, q, u) => globalThis.__speexWeb.dspJitterPut(h, d, t, s, q, u)")(
    handle,
    data,
    timestamp,
    span,
    sequence,
    userData,
) as Int

internal actual fun webDspJitterGet(handle: Int, desiredSpan: Int): Int =
    js("(h, s) => globalThis.__speexWeb.dspJitterGet(h, s)")(handle, desiredSpan) as Int

internal actual fun webDspJitterGetAnother(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterGetAnother(h)")(handle) as Int

internal actual fun webDspJitterPacket(handle: Int): String =
    js("(h) => globalThis.__speexWeb.dspJitterPacket(h)")(handle) as String

internal actual fun webDspJitterTimestamp(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterTimestamp(h)")(handle) as Int

internal actual fun webDspJitterSpan(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterSpan(h)")(handle) as Int

internal actual fun webDspJitterSequence(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterSequence(h)")(handle) as Int

internal actual fun webDspJitterUserData(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterUserData(h)")(handle) as Int

internal actual fun webDspJitterPointerTimestamp(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspJitterPointerTimestamp(h)")(handle) as Int

internal actual fun webDspJitterTick(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspJitterTick(h)")(handle)
}

internal actual fun webDspJitterRemainingSpan(handle: Int, remaining: Int) {
    js("(h, r) => globalThis.__speexWeb.dspJitterRemainingSpan(h, r)")(handle, remaining)
}

internal actual fun webDspJitterCtlInt(handle: Int, request: Int, value: Int): Int =
    js("(h, r, v) => globalThis.__speexWeb.dspJitterCtlInt(h, r, v)")(handle, request, value) as Int

internal actual fun webDspJitterGetInt(handle: Int, request: Int): Int =
    js("(h, r) => globalThis.__speexWeb.dspJitterGetInt(h, r)")(handle, request) as Int

internal actual fun webDspRingBufferCreate(size: Int): Int =
    js("(s) => globalThis.__speexWeb.dspRingBufferCreate(s)")(size) as Int

internal actual fun webDspRingBufferFree(handle: Int) {
    js("(h) => globalThis.__speexWeb.dspRingBufferFree(h)")(handle)
}

internal actual fun webDspRingBufferWrite(handle: Int, data: String): Int =
    js("(h, d) => globalThis.__speexWeb.dspRingBufferWrite(h, d)")(handle, data) as Int

internal actual fun webDspRingBufferWriteZeros(handle: Int, len: Int): Int =
    js("(h, l) => globalThis.__speexWeb.dspRingBufferWriteZeros(h, l)")(handle, len) as Int

internal actual fun webDspRingBufferRead(handle: Int, len: Int): String =
    js("(h, l) => globalThis.__speexWeb.dspRingBufferRead(h, l)")(handle, len) as String

internal actual fun webDspRingBufferAvailable(handle: Int): Int =
    js("(h) => globalThis.__speexWeb.dspRingBufferAvailable(h)")(handle) as Int

internal actual fun webDspRingBufferResize(handle: Int, len: Int): Int =
    js("(h, l) => globalThis.__speexWeb.dspRingBufferResize(h, l)")(handle, len) as Int
