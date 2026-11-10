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

internal actual class SpeexResamplerHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexPreprocessorHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexEchoHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexDecorrelatorHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexJitterHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual class SpeexRingBufferHandle actual constructor() {
    internal var pointer: Int = 0
}

internal actual object SpeexDspNative {
    private fun ensureInitialised() {
        webInit()
    }

    actual fun versionString(): String {
        ensureInitialised()
        return webDspVersionString()
    }

    actual fun resamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): SpeexResamplerHandle? {
        ensureInitialised()
        val pointer = webDspResamplerCreate(channels, inRate, outRate, quality)
        if (pointer == 0) return null
        return SpeexResamplerHandle().also { it.pointer = pointer }
    }

    actual fun resamplerCreateFrac(
        channels: Int,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
        quality: Int,
    ): SpeexResamplerHandle? {
        ensureInitialised()
        val pointer = webDspResamplerCreateFrac(channels, ratioNum, ratioDen, inRate, outRate, quality)
        if (pointer == 0) return null
        return SpeexResamplerHandle().also { it.pointer = pointer }
    }

    actual fun resamplerError(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerError(handle.pointer)
    }

    actual fun resamplerFree(handle: SpeexResamplerHandle) {
        ensureInitialised()
        webDspResamplerFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun resamplerChannels(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerChannels(handle.pointer)
    }

    actual fun resamplerSetRate(handle: SpeexResamplerHandle, inRate: Int, outRate: Int): Int {
        ensureInitialised()
        return webDspResamplerSetRate(handle.pointer, inRate, outRate)
    }

    actual fun resamplerSetRateFrac(
        handle: SpeexResamplerHandle,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
    ): Int {
        ensureInitialised()
        return webDspResamplerSetRateFrac(handle.pointer, ratioNum, ratioDen, inRate, outRate)
    }

    actual fun resamplerInputRate(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerInputRate(handle.pointer)
    }

    actual fun resamplerOutputRate(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerOutputRate(handle.pointer)
    }

    actual fun resamplerRatioNum(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerRatioNum(handle.pointer)
    }

    actual fun resamplerRatioDen(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerRatioDen(handle.pointer)
    }

    actual fun resamplerSetQuality(handle: SpeexResamplerHandle, quality: Int): Int {
        ensureInitialised()
        return webDspResamplerSetQuality(handle.pointer, quality)
    }

    actual fun resamplerQuality(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerQuality(handle.pointer)
    }

    actual fun resamplerSetInputStride(handle: SpeexResamplerHandle, stride: Int) {
        ensureInitialised()
        webDspResamplerSetInputStride(handle.pointer, stride)
    }

    actual fun resamplerInputStride(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerInputStride(handle.pointer)
    }

    actual fun resamplerSetOutputStride(handle: SpeexResamplerHandle, stride: Int) {
        ensureInitialised()
        webDspResamplerSetOutputStride(handle.pointer, stride)
    }

    actual fun resamplerOutputStride(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerOutputStride(handle.pointer)
    }

    actual fun resamplerInputLatency(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerInputLatency(handle.pointer)
    }

    actual fun resamplerOutputLatency(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerOutputLatency(handle.pointer)
    }

    actual fun resamplerSkipZeros(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerSkipZeros(handle.pointer)
    }

    actual fun resamplerResetMem(handle: SpeexResamplerHandle): Int {
        ensureInitialised()
        return webDspResamplerResetMem(handle.pointer)
    }

    actual fun resamplerProcess(handle: SpeexResamplerHandle, samples: ShortArray, offset: Int, count: Int): Int {
        ensureInitialised()
        val channels = webDspResamplerChannels(handle.pointer)
        val total = count * channels
        return webDspResamplerProcess(handle.pointer, SpeexWebPacking.encodeShorts(samples, offset, total))
    }

    actual fun resamplerOutput(handle: SpeexResamplerHandle): ShortArray? {
        ensureInitialised()
        val encoded = webDspResamplerOutput(handle.pointer)
        return if (encoded.isEmpty()) ShortArray(0) else SpeexWebPacking.decodeShorts(encoded)
    }

    actual fun resamplerStrerror(error: Int): String {
        ensureInitialised()
        return webDspResamplerStrerror(error)
    }

    actual fun preprocessCreate(frameSize: Int, samplingRate: Int): SpeexPreprocessorHandle? {
        ensureInitialised()
        val pointer = webDspPreprocessCreate(frameSize, samplingRate)
        if (pointer == 0) return null
        return SpeexPreprocessorHandle().also { it.pointer = pointer }
    }

    actual fun preprocessFree(handle: SpeexPreprocessorHandle) {
        ensureInitialised()
        webDspPreprocessFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun preprocessFrameSize(handle: SpeexPreprocessorHandle): Int {
        ensureInitialised()
        return webDspPreprocessFrameSize(handle.pointer)
    }

    actual fun preprocessRun(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int): Int {
        ensureInitialised()
        val result = webDspPreprocessRun(handle.pointer, SpeexWebPacking.encodeShorts(samples, offset, count))
        val code = parseWebCode(result)
        val payload = parseWebPayload(result)
        if (payload.isNotEmpty()) {
            SpeexWebPacking.decodeShorts(payload).copyInto(samples, offset)
        }
        return code
    }

    actual fun preprocessEstimateUpdate(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int) {
        ensureInitialised()
        webDspPreprocessEstimateUpdate(handle.pointer, SpeexWebPacking.encodeShorts(samples, offset, count))
    }

    actual fun preprocessCtlInt(handle: SpeexPreprocessorHandle, request: Int, value: Int): Int {
        ensureInitialised()
        return webDspPreprocessCtlInt(handle.pointer, request, value)
    }

    actual fun preprocessGetInt(handle: SpeexPreprocessorHandle, request: Int): Int {
        ensureInitialised()
        return webDspPreprocessGetInt(handle.pointer, request)
    }

    actual fun preprocessCtlFloat(handle: SpeexPreprocessorHandle, request: Int, value: Float): Int {
        ensureInitialised()
        return webDspPreprocessCtlFloat(handle.pointer, request, value)
    }

    actual fun preprocessGetFloat(handle: SpeexPreprocessorHandle, request: Int): Float {
        ensureInitialised()
        return webDspPreprocessGetFloat(handle.pointer, request)
    }

    actual fun preprocessSetEcho(handle: SpeexPreprocessorHandle, echo: SpeexEchoHandle?): Int {
        ensureInitialised()
        return webDspPreprocessSetEcho(handle.pointer, echo?.pointer ?: 0)
    }

    actual fun preprocessPsdSize(handle: SpeexPreprocessorHandle): Int {
        ensureInitialised()
        return webDspPreprocessPsdSize(handle.pointer)
    }

    actual fun preprocessPsd(handle: SpeexPreprocessorHandle): IntArray? {
        ensureInitialised()
        val encoded = webDspPreprocessPsd(handle.pointer)
        return if (encoded.isEmpty()) IntArray(0) else SpeexWebPacking.decodeInts(encoded)
    }

    actual fun preprocessNoisePsdSize(handle: SpeexPreprocessorHandle): Int {
        ensureInitialised()
        return webDspPreprocessNoisePsdSize(handle.pointer)
    }

    actual fun preprocessNoisePsd(handle: SpeexPreprocessorHandle): IntArray? {
        ensureInitialised()
        val encoded = webDspPreprocessNoisePsd(handle.pointer)
        return if (encoded.isEmpty()) IntArray(0) else SpeexWebPacking.decodeInts(encoded)
    }

    actual fun echoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): SpeexEchoHandle? {
        ensureInitialised()
        val pointer = webDspEchoCreate(frameSize, filterLength, microphones, speakers)
        if (pointer == 0) return null
        return SpeexEchoHandle().also { it.pointer = pointer }
    }

    actual fun echoFree(handle: SpeexEchoHandle) {
        ensureInitialised()
        webDspEchoFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun echoReset(handle: SpeexEchoHandle) {
        ensureInitialised()
        webDspEchoReset(handle.pointer)
    }

    actual fun echoFrameSize(handle: SpeexEchoHandle): Int {
        ensureInitialised()
        return webDspEchoFrameSize(handle.pointer)
    }

    actual fun echoMicrophones(handle: SpeexEchoHandle): Int {
        ensureInitialised()
        return webDspEchoMicrophones(handle.pointer)
    }

    actual fun echoSpeakers(handle: SpeexEchoHandle): Int {
        ensureInitialised()
        return webDspEchoSpeakers(handle.pointer)
    }

    actual fun echoCancellation(
        handle: SpeexEchoHandle,
        rec: ShortArray,
        recOffset: Int,
        play: ShortArray,
        playOffset: Int,
    ): Int {
        ensureInitialised()
        val recSlice = if (recOffset == 0) rec else rec.copyOfRange(recOffset, rec.size)
        val playSlice = if (playOffset == 0) play else play.copyOfRange(playOffset, play.size)
        return webDspEchoCancellation(
            handle.pointer,
            SpeexWebPacking.encodeShorts(recSlice),
            SpeexWebPacking.encodeShorts(playSlice),
        )
    }

    actual fun echoCapture(handle: SpeexEchoHandle, rec: ShortArray, offset: Int): Int {
        ensureInitialised()
        val slice = if (offset == 0) rec else rec.copyOfRange(offset, rec.size)
        return webDspEchoCapture(handle.pointer, SpeexWebPacking.encodeShorts(slice))
    }

    actual fun echoPlayback(handle: SpeexEchoHandle, play: ShortArray, offset: Int): Int {
        ensureInitialised()
        val slice = if (offset == 0) play else play.copyOfRange(offset, play.size)
        return webDspEchoPlayback(handle.pointer, SpeexWebPacking.encodeShorts(slice))
    }

    actual fun echoOutput(handle: SpeexEchoHandle): ShortArray? {
        ensureInitialised()
        val encoded = webDspEchoOutput(handle.pointer)
        return if (encoded.isEmpty()) ShortArray(0) else SpeexWebPacking.decodeShorts(encoded)
    }

    actual fun echoCtlInt(handle: SpeexEchoHandle, request: Int, value: Int): Int {
        ensureInitialised()
        return webDspEchoCtlInt(handle.pointer, request, value)
    }

    actual fun echoGetInt(handle: SpeexEchoHandle, request: Int): Int {
        ensureInitialised()
        return webDspEchoGetInt(handle.pointer, request)
    }

    actual fun echoImpulseResponseSize(handle: SpeexEchoHandle): Int {
        ensureInitialised()
        return webDspEchoImpulseResponseSize(handle.pointer)
    }

    actual fun echoImpulseResponse(handle: SpeexEchoHandle): IntArray? {
        ensureInitialised()
        val encoded = webDspEchoImpulseResponse(handle.pointer)
        return if (encoded.isEmpty()) IntArray(0) else SpeexWebPacking.decodeInts(encoded)
    }

    actual fun decorrCreate(rate: Int, channels: Int, frameSize: Int): SpeexDecorrelatorHandle? {
        ensureInitialised()
        val pointer = webDspDecorrCreate(rate, channels, frameSize)
        if (pointer == 0) return null
        return SpeexDecorrelatorHandle().also { it.pointer = pointer }
    }

    actual fun decorrFree(handle: SpeexDecorrelatorHandle) {
        ensureInitialised()
        webDspDecorrFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun decorrChannels(handle: SpeexDecorrelatorHandle): Int {
        ensureInitialised()
        return webDspDecorrChannels(handle.pointer)
    }

    actual fun decorrFrameSize(handle: SpeexDecorrelatorHandle): Int {
        ensureInitialised()
        return webDspDecorrFrameSize(handle.pointer)
    }

    actual fun decorrProcess(
        handle: SpeexDecorrelatorHandle,
        input: ShortArray,
        offset: Int,
        count: Int,
        strength: Int,
    ): Int {
        ensureInitialised()
        return webDspDecorrProcess(
            handle.pointer,
            SpeexWebPacking.encodeShorts(input, offset, count),
            strength,
        )
    }

    actual fun decorrOutput(handle: SpeexDecorrelatorHandle): ShortArray? {
        ensureInitialised()
        val encoded = webDspDecorrOutput(handle.pointer)
        return if (encoded.isEmpty()) ShortArray(0) else SpeexWebPacking.decodeShorts(encoded)
    }

    actual fun jitterCreate(stepSize: Int): SpeexJitterHandle? {
        ensureInitialised()
        val pointer = webDspJitterCreate(stepSize)
        if (pointer == 0) return null
        return SpeexJitterHandle().also { it.pointer = pointer }
    }

    actual fun jitterFree(handle: SpeexJitterHandle) {
        ensureInitialised()
        webDspJitterFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun jitterReset(handle: SpeexJitterHandle) {
        ensureInitialised()
        webDspJitterReset(handle.pointer)
    }

    actual fun jitterPut(
        handle: SpeexJitterHandle,
        data: ByteArray,
        timestamp: Int,
        span: Int,
        sequence: Int,
        userData: Int,
    ): Int {
        ensureInitialised()
        return webDspJitterPut(
            handle.pointer,
            SpeexWebPacking.encodeBytes(data),
            timestamp,
            span,
            sequence,
            userData,
        )
    }

    actual fun jitterGet(handle: SpeexJitterHandle, desiredSpan: Int): Int {
        ensureInitialised()
        return webDspJitterGet(handle.pointer, desiredSpan)
    }

    actual fun jitterGetAnother(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterGetAnother(handle.pointer)
    }

    actual fun jitterPacket(handle: SpeexJitterHandle): ByteArray? {
        ensureInitialised()
        val encoded = webDspJitterPacket(handle.pointer)
        return if (encoded.isEmpty()) ByteArray(0) else SpeexWebPacking.decodeBytes(encoded)
    }

    actual fun jitterTimestamp(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterTimestamp(handle.pointer)
    }

    actual fun jitterSpan(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterSpan(handle.pointer)
    }

    actual fun jitterSequence(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterSequence(handle.pointer)
    }

    actual fun jitterUserData(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterUserData(handle.pointer)
    }

    actual fun jitterPointerTimestamp(handle: SpeexJitterHandle): Int {
        ensureInitialised()
        return webDspJitterPointerTimestamp(handle.pointer)
    }

    actual fun jitterTick(handle: SpeexJitterHandle) {
        ensureInitialised()
        webDspJitterTick(handle.pointer)
    }

    actual fun jitterRemainingSpan(handle: SpeexJitterHandle, remaining: Int) {
        ensureInitialised()
        webDspJitterRemainingSpan(handle.pointer, remaining)
    }

    actual fun jitterCtlInt(handle: SpeexJitterHandle, request: Int, value: Int): Int {
        ensureInitialised()
        return webDspJitterCtlInt(handle.pointer, request, value)
    }

    actual fun jitterGetInt(handle: SpeexJitterHandle, request: Int): Int {
        ensureInitialised()
        return webDspJitterGetInt(handle.pointer, request)
    }

    actual fun ringBufferCreate(size: Int): SpeexRingBufferHandle? {
        ensureInitialised()
        val pointer = webDspRingBufferCreate(size)
        if (pointer == 0) return null
        return SpeexRingBufferHandle().also { it.pointer = pointer }
    }

    actual fun ringBufferFree(handle: SpeexRingBufferHandle) {
        ensureInitialised()
        webDspRingBufferFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun ringBufferWrite(handle: SpeexRingBufferHandle, data: ByteArray, offset: Int, count: Int): Int {
        ensureInitialised()
        return webDspRingBufferWrite(
            handle.pointer,
            SpeexWebPacking.encodeBytes(data, offset, count),
        )
    }

    actual fun ringBufferWriteZeros(handle: SpeexRingBufferHandle, len: Int): Int {
        ensureInitialised()
        return webDspRingBufferWriteZeros(handle.pointer, len)
    }

    actual fun ringBufferRead(handle: SpeexRingBufferHandle, len: Int): ByteArray? {
        ensureInitialised()
        val encoded = webDspRingBufferRead(handle.pointer, len)
        return if (encoded.isEmpty()) null else SpeexWebPacking.decodeBytes(encoded)
    }

    actual fun ringBufferAvailable(handle: SpeexRingBufferHandle): Int {
        ensureInitialised()
        return webDspRingBufferAvailable(handle.pointer)
    }

    actual fun ringBufferResize(handle: SpeexRingBufferHandle, len: Int): Int {
        ensureInitialised()
        return webDspRingBufferResize(handle.pointer, len)
    }
}
