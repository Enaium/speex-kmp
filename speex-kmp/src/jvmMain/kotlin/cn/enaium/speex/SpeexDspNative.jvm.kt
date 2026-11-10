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

internal object JniSpeexDsp {
    init {
        NativeLoader.load()
    }

    external fun nativeVersionString(): String

    external fun nativeResamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): Long

    external fun nativeResamplerCreateFrac(
        channels: Int,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
        quality: Int,
    ): Long

    external fun nativeResamplerError(handle: Long): Int

    external fun nativeResamplerFree(handle: Long)

    external fun nativeResamplerChannels(handle: Long): Int

    external fun nativeResamplerSetRate(handle: Long, inRate: Int, outRate: Int): Int

    external fun nativeResamplerSetRateFrac(
        handle: Long,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
    ): Int

    external fun nativeResamplerInputRate(handle: Long): Int

    external fun nativeResamplerOutputRate(handle: Long): Int

    external fun nativeResamplerRatioNum(handle: Long): Int

    external fun nativeResamplerRatioDen(handle: Long): Int

    external fun nativeResamplerSetQuality(handle: Long, quality: Int): Int

    external fun nativeResamplerQuality(handle: Long): Int

    external fun nativeResamplerSetInputStride(handle: Long, stride: Int)

    external fun nativeResamplerInputStride(handle: Long): Int

    external fun nativeResamplerSetOutputStride(handle: Long, stride: Int)

    external fun nativeResamplerOutputStride(handle: Long): Int

    external fun nativeResamplerInputLatency(handle: Long): Int

    external fun nativeResamplerOutputLatency(handle: Long): Int

    external fun nativeResamplerSkipZeros(handle: Long): Int

    external fun nativeResamplerResetMem(handle: Long): Int

    external fun nativeResamplerProcess(handle: Long, samples: ShortArray, offset: Int, count: Int): Int

    external fun nativeResamplerOutput(handle: Long): ShortArray?

    external fun nativeResamplerStrerror(error: Int): String

    external fun nativePreprocessCreate(frameSize: Int, samplingRate: Int): Long

    external fun nativePreprocessFree(handle: Long)

    external fun nativePreprocessFrameSize(handle: Long): Int

    external fun nativePreprocessRun(handle: Long, samples: ShortArray, offset: Int, count: Int): Int

    external fun nativePreprocessEstimateUpdate(handle: Long, samples: ShortArray, offset: Int, count: Int)

    external fun nativePreprocessCtlInt(handle: Long, request: Int, value: Int): Int

    external fun nativePreprocessGetInt(handle: Long, request: Int): Int

    external fun nativePreprocessCtlFloat(handle: Long, request: Int, value: Float): Int

    external fun nativePreprocessGetFloat(handle: Long, request: Int): Float

    external fun nativePreprocessSetEcho(handle: Long, echo: Long): Int

    external fun nativePreprocessPsdSize(handle: Long): Int

    external fun nativePreprocessPsd(handle: Long): IntArray?

    external fun nativePreprocessNoisePsdSize(handle: Long): Int

    external fun nativePreprocessNoisePsd(handle: Long): IntArray?

    external fun nativeEchoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): Long

    external fun nativeEchoFree(handle: Long)

    external fun nativeEchoReset(handle: Long)

    external fun nativeEchoFrameSize(handle: Long): Int

    external fun nativeEchoMicrophones(handle: Long): Int

    external fun nativeEchoSpeakers(handle: Long): Int

    external fun nativeEchoCancellation(
        handle: Long,
        rec: ShortArray,
        recOffset: Int,
        play: ShortArray,
        playOffset: Int,
    ): Int

    external fun nativeEchoCapture(handle: Long, rec: ShortArray, offset: Int): Int

    external fun nativeEchoPlayback(handle: Long, play: ShortArray, offset: Int): Int

    external fun nativeEchoOutput(handle: Long): ShortArray?

    external fun nativeEchoCtlInt(handle: Long, request: Int, value: Int): Int

    external fun nativeEchoGetInt(handle: Long, request: Int): Int

    external fun nativeEchoImpulseResponseSize(handle: Long): Int

    external fun nativeEchoImpulseResponse(handle: Long): IntArray?

    external fun nativeDecorrCreate(rate: Int, channels: Int, frameSize: Int): Long

    external fun nativeDecorrFree(handle: Long)

    external fun nativeDecorrChannels(handle: Long): Int

    external fun nativeDecorrFrameSize(handle: Long): Int

    external fun nativeDecorrProcess(handle: Long, input: ShortArray, offset: Int, count: Int, strength: Int): Int

    external fun nativeDecorrOutput(handle: Long): ShortArray?

    external fun nativeJitterCreate(stepSize: Int): Long

    external fun nativeJitterFree(handle: Long)

    external fun nativeJitterReset(handle: Long)

    external fun nativeJitterPut(
        handle: Long,
        data: ByteArray,
        timestamp: Int,
        span: Int,
        sequence: Int,
        userData: Int,
    ): Int

    external fun nativeJitterGet(handle: Long, desiredSpan: Int): Int

    external fun nativeJitterGetAnother(handle: Long): Int

    external fun nativeJitterPacket(handle: Long): ByteArray?

    external fun nativeJitterTimestamp(handle: Long): Int

    external fun nativeJitterSpan(handle: Long): Int

    external fun nativeJitterSequence(handle: Long): Int

    external fun nativeJitterUserData(handle: Long): Int

    external fun nativeJitterPointerTimestamp(handle: Long): Int

    external fun nativeJitterTick(handle: Long)

    external fun nativeJitterRemainingSpan(handle: Long, remaining: Int)

    external fun nativeJitterCtlInt(handle: Long, request: Int, value: Int): Int

    external fun nativeJitterGetInt(handle: Long, request: Int): Int

    external fun nativeRingBufferCreate(size: Int): Long

    external fun nativeRingBufferFree(handle: Long)

    external fun nativeRingBufferWrite(handle: Long, data: ByteArray, offset: Int, count: Int): Int

    external fun nativeRingBufferWriteZeros(handle: Long, len: Int): Int

    external fun nativeRingBufferRead(handle: Long, len: Int): ByteArray?

    external fun nativeRingBufferAvailable(handle: Long): Int

    external fun nativeRingBufferResize(handle: Long, len: Int): Int
}

internal actual class SpeexResamplerHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexPreprocessorHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexEchoHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexDecorrelatorHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexJitterHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual class SpeexRingBufferHandle actual constructor() {
    internal var pointer: Long = 0
}

internal actual object SpeexDspNative {
    actual fun versionString(): String = JniSpeexDsp.nativeVersionString()

    actual fun resamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): SpeexResamplerHandle? {
        val pointer = JniSpeexDsp.nativeResamplerCreate(channels, inRate, outRate, quality)
        if (pointer == 0L) return null
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
        val pointer = JniSpeexDsp.nativeResamplerCreateFrac(
            channels,
            ratioNum,
            ratioDen,
            inRate,
            outRate,
            quality,
        )
        if (pointer == 0L) return null
        return SpeexResamplerHandle().also { it.pointer = pointer }
    }

    actual fun resamplerError(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerError(handle.pointer)

    actual fun resamplerFree(handle: SpeexResamplerHandle) {
        JniSpeexDsp.nativeResamplerFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun resamplerChannels(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerChannels(handle.pointer)

    actual fun resamplerSetRate(handle: SpeexResamplerHandle, inRate: Int, outRate: Int): Int =
        JniSpeexDsp.nativeResamplerSetRate(handle.pointer, inRate, outRate)

    actual fun resamplerSetRateFrac(
        handle: SpeexResamplerHandle,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
    ): Int = JniSpeexDsp.nativeResamplerSetRateFrac(handle.pointer, ratioNum, ratioDen, inRate, outRate)

    actual fun resamplerInputRate(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerInputRate(handle.pointer)

    actual fun resamplerOutputRate(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerOutputRate(handle.pointer)

    actual fun resamplerRatioNum(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerRatioNum(handle.pointer)

    actual fun resamplerRatioDen(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerRatioDen(handle.pointer)

    actual fun resamplerSetQuality(handle: SpeexResamplerHandle, quality: Int): Int =
        JniSpeexDsp.nativeResamplerSetQuality(handle.pointer, quality)

    actual fun resamplerQuality(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerQuality(handle.pointer)

    actual fun resamplerSetInputStride(handle: SpeexResamplerHandle, stride: Int) =
        JniSpeexDsp.nativeResamplerSetInputStride(handle.pointer, stride)

    actual fun resamplerInputStride(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerInputStride(handle.pointer)

    actual fun resamplerSetOutputStride(handle: SpeexResamplerHandle, stride: Int) =
        JniSpeexDsp.nativeResamplerSetOutputStride(handle.pointer, stride)

    actual fun resamplerOutputStride(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerOutputStride(handle.pointer)

    actual fun resamplerInputLatency(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerInputLatency(handle.pointer)

    actual fun resamplerOutputLatency(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerOutputLatency(handle.pointer)

    actual fun resamplerSkipZeros(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerSkipZeros(handle.pointer)

    actual fun resamplerResetMem(handle: SpeexResamplerHandle): Int =
        JniSpeexDsp.nativeResamplerResetMem(handle.pointer)

    actual fun resamplerProcess(handle: SpeexResamplerHandle, samples: ShortArray, offset: Int, count: Int): Int =
        JniSpeexDsp.nativeResamplerProcess(handle.pointer, samples, offset, count)

    actual fun resamplerOutput(handle: SpeexResamplerHandle): ShortArray? =
        JniSpeexDsp.nativeResamplerOutput(handle.pointer)

    actual fun resamplerStrerror(error: Int): String = JniSpeexDsp.nativeResamplerStrerror(error)

    actual fun preprocessCreate(frameSize: Int, samplingRate: Int): SpeexPreprocessorHandle? {
        val pointer = JniSpeexDsp.nativePreprocessCreate(frameSize, samplingRate)
        if (pointer == 0L) return null
        return SpeexPreprocessorHandle().also { it.pointer = pointer }
    }

    actual fun preprocessFree(handle: SpeexPreprocessorHandle) {
        JniSpeexDsp.nativePreprocessFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun preprocessFrameSize(handle: SpeexPreprocessorHandle): Int =
        JniSpeexDsp.nativePreprocessFrameSize(handle.pointer)

    actual fun preprocessRun(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int): Int =
        JniSpeexDsp.nativePreprocessRun(handle.pointer, samples, offset, count)

    actual fun preprocessEstimateUpdate(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int) =
        JniSpeexDsp.nativePreprocessEstimateUpdate(handle.pointer, samples, offset, count)

    actual fun preprocessCtlInt(handle: SpeexPreprocessorHandle, request: Int, value: Int): Int =
        JniSpeexDsp.nativePreprocessCtlInt(handle.pointer, request, value)

    actual fun preprocessGetInt(handle: SpeexPreprocessorHandle, request: Int): Int =
        JniSpeexDsp.nativePreprocessGetInt(handle.pointer, request)

    actual fun preprocessCtlFloat(handle: SpeexPreprocessorHandle, request: Int, value: Float): Int =
        JniSpeexDsp.nativePreprocessCtlFloat(handle.pointer, request, value)

    actual fun preprocessGetFloat(handle: SpeexPreprocessorHandle, request: Int): Float =
        JniSpeexDsp.nativePreprocessGetFloat(handle.pointer, request)

    actual fun preprocessSetEcho(handle: SpeexPreprocessorHandle, echo: SpeexEchoHandle?) =
        JniSpeexDsp.nativePreprocessSetEcho(handle.pointer, echo?.pointer ?: 0L)

    actual fun preprocessPsdSize(handle: SpeexPreprocessorHandle): Int =
        JniSpeexDsp.nativePreprocessPsdSize(handle.pointer)

    actual fun preprocessPsd(handle: SpeexPreprocessorHandle): IntArray? =
        JniSpeexDsp.nativePreprocessPsd(handle.pointer)

    actual fun preprocessNoisePsdSize(handle: SpeexPreprocessorHandle): Int =
        JniSpeexDsp.nativePreprocessNoisePsdSize(handle.pointer)

    actual fun preprocessNoisePsd(handle: SpeexPreprocessorHandle): IntArray? =
        JniSpeexDsp.nativePreprocessNoisePsd(handle.pointer)

    actual fun echoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): SpeexEchoHandle? {
        val pointer = JniSpeexDsp.nativeEchoCreate(frameSize, filterLength, microphones, speakers)
        if (pointer == 0L) return null
        return SpeexEchoHandle().also { it.pointer = pointer }
    }

    actual fun echoFree(handle: SpeexEchoHandle) {
        JniSpeexDsp.nativeEchoFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun echoReset(handle: SpeexEchoHandle) = JniSpeexDsp.nativeEchoReset(handle.pointer)

    actual fun echoFrameSize(handle: SpeexEchoHandle): Int = JniSpeexDsp.nativeEchoFrameSize(handle.pointer)

    actual fun echoMicrophones(handle: SpeexEchoHandle): Int =
        JniSpeexDsp.nativeEchoMicrophones(handle.pointer)

    actual fun echoSpeakers(handle: SpeexEchoHandle): Int = JniSpeexDsp.nativeEchoSpeakers(handle.pointer)

    actual fun echoCancellation(
        handle: SpeexEchoHandle,
        rec: ShortArray,
        recOffset: Int,
        play: ShortArray,
        playOffset: Int,
    ): Int = JniSpeexDsp.nativeEchoCancellation(handle.pointer, rec, recOffset, play, playOffset)

    actual fun echoCapture(handle: SpeexEchoHandle, rec: ShortArray, offset: Int): Int =
        JniSpeexDsp.nativeEchoCapture(handle.pointer, rec, offset)

    actual fun echoPlayback(handle: SpeexEchoHandle, play: ShortArray, offset: Int): Int =
        JniSpeexDsp.nativeEchoPlayback(handle.pointer, play, offset)

    actual fun echoOutput(handle: SpeexEchoHandle): ShortArray? = JniSpeexDsp.nativeEchoOutput(handle.pointer)

    actual fun echoCtlInt(handle: SpeexEchoHandle, request: Int, value: Int): Int =
        JniSpeexDsp.nativeEchoCtlInt(handle.pointer, request, value)

    actual fun echoGetInt(handle: SpeexEchoHandle, request: Int): Int =
        JniSpeexDsp.nativeEchoGetInt(handle.pointer, request)

    actual fun echoImpulseResponseSize(handle: SpeexEchoHandle): Int =
        JniSpeexDsp.nativeEchoImpulseResponseSize(handle.pointer)

    actual fun echoImpulseResponse(handle: SpeexEchoHandle): IntArray? =
        JniSpeexDsp.nativeEchoImpulseResponse(handle.pointer)

    actual fun decorrCreate(rate: Int, channels: Int, frameSize: Int): SpeexDecorrelatorHandle? {
        val pointer = JniSpeexDsp.nativeDecorrCreate(rate, channels, frameSize)
        if (pointer == 0L) return null
        return SpeexDecorrelatorHandle().also { it.pointer = pointer }
    }

    actual fun decorrFree(handle: SpeexDecorrelatorHandle) {
        JniSpeexDsp.nativeDecorrFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun decorrChannels(handle: SpeexDecorrelatorHandle): Int =
        JniSpeexDsp.nativeDecorrChannels(handle.pointer)

    actual fun decorrFrameSize(handle: SpeexDecorrelatorHandle): Int =
        JniSpeexDsp.nativeDecorrFrameSize(handle.pointer)

    actual fun decorrProcess(
        handle: SpeexDecorrelatorHandle,
        input: ShortArray,
        offset: Int,
        count: Int,
        strength: Int,
    ): Int = JniSpeexDsp.nativeDecorrProcess(handle.pointer, input, offset, count, strength)

    actual fun decorrOutput(handle: SpeexDecorrelatorHandle): ShortArray? =
        JniSpeexDsp.nativeDecorrOutput(handle.pointer)

    actual fun jitterCreate(stepSize: Int): SpeexJitterHandle? {
        val pointer = JniSpeexDsp.nativeJitterCreate(stepSize)
        if (pointer == 0L) return null
        return SpeexJitterHandle().also { it.pointer = pointer }
    }

    actual fun jitterFree(handle: SpeexJitterHandle) {
        JniSpeexDsp.nativeJitterFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun jitterReset(handle: SpeexJitterHandle) = JniSpeexDsp.nativeJitterReset(handle.pointer)

    actual fun jitterPut(
        handle: SpeexJitterHandle,
        data: ByteArray,
        timestamp: Int,
        span: Int,
        sequence: Int,
        userData: Int,
    ): Int = JniSpeexDsp.nativeJitterPut(handle.pointer, data, timestamp, span, sequence, userData)

    actual fun jitterGet(handle: SpeexJitterHandle, desiredSpan: Int): Int =
        JniSpeexDsp.nativeJitterGet(handle.pointer, desiredSpan)

    actual fun jitterGetAnother(handle: SpeexJitterHandle): Int =
        JniSpeexDsp.nativeJitterGetAnother(handle.pointer)

    actual fun jitterPacket(handle: SpeexJitterHandle): ByteArray? =
        JniSpeexDsp.nativeJitterPacket(handle.pointer)

    actual fun jitterTimestamp(handle: SpeexJitterHandle): Int =
        JniSpeexDsp.nativeJitterTimestamp(handle.pointer)

    actual fun jitterSpan(handle: SpeexJitterHandle): Int = JniSpeexDsp.nativeJitterSpan(handle.pointer)

    actual fun jitterSequence(handle: SpeexJitterHandle): Int =
        JniSpeexDsp.nativeJitterSequence(handle.pointer)

    actual fun jitterUserData(handle: SpeexJitterHandle): Int =
        JniSpeexDsp.nativeJitterUserData(handle.pointer)

    actual fun jitterPointerTimestamp(handle: SpeexJitterHandle): Int =
        JniSpeexDsp.nativeJitterPointerTimestamp(handle.pointer)

    actual fun jitterTick(handle: SpeexJitterHandle) = JniSpeexDsp.nativeJitterTick(handle.pointer)

    actual fun jitterRemainingSpan(handle: SpeexJitterHandle, remaining: Int) =
        JniSpeexDsp.nativeJitterRemainingSpan(handle.pointer, remaining)

    actual fun jitterCtlInt(handle: SpeexJitterHandle, request: Int, value: Int): Int =
        JniSpeexDsp.nativeJitterCtlInt(handle.pointer, request, value)

    actual fun jitterGetInt(handle: SpeexJitterHandle, request: Int): Int =
        JniSpeexDsp.nativeJitterGetInt(handle.pointer, request)

    actual fun ringBufferCreate(size: Int): SpeexRingBufferHandle? {
        val pointer = JniSpeexDsp.nativeRingBufferCreate(size)
        if (pointer == 0L) return null
        return SpeexRingBufferHandle().also { it.pointer = pointer }
    }

    actual fun ringBufferFree(handle: SpeexRingBufferHandle) {
        JniSpeexDsp.nativeRingBufferFree(handle.pointer)
        handle.pointer = 0
    }

    actual fun ringBufferWrite(handle: SpeexRingBufferHandle, data: ByteArray, offset: Int, count: Int): Int =
        JniSpeexDsp.nativeRingBufferWrite(handle.pointer, data, offset, count)

    actual fun ringBufferWriteZeros(handle: SpeexRingBufferHandle, len: Int): Int =
        JniSpeexDsp.nativeRingBufferWriteZeros(handle.pointer, len)

    actual fun ringBufferRead(handle: SpeexRingBufferHandle, len: Int): ByteArray? =
        JniSpeexDsp.nativeRingBufferRead(handle.pointer, len)

    actual fun ringBufferAvailable(handle: SpeexRingBufferHandle): Int =
        JniSpeexDsp.nativeRingBufferAvailable(handle.pointer)

    actual fun ringBufferResize(handle: SpeexRingBufferHandle, len: Int): Int =
        JniSpeexDsp.nativeRingBufferResize(handle.pointer, len)
}
