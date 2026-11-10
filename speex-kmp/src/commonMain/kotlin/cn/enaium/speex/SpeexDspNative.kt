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

/** Opaque resampler state owned by SpeexDSP. */
internal expect class SpeexResamplerHandle()

/** Opaque preprocessor state owned by SpeexDSP. */
internal expect class SpeexPreprocessorHandle()

/** Opaque echo-canceller state owned by SpeexDSP. */
internal expect class SpeexEchoHandle()

/** Opaque channel-decorrelator state owned by SpeexDSP. */
internal expect class SpeexDecorrelatorHandle()

/** Opaque jitter buffer owned by SpeexDSP. */
internal expect class SpeexJitterHandle()

/** Opaque ring buffer owned by SpeexDSP. */
internal expect class SpeexRingBufferHandle()

/**
 * Bridge to the SpeexDSP half of the C shim (`jni/include/speexdsp_kmp.h`).
 *
 * The contract matches the libspeex bridge in [SpeexNative]: handles are never
 * reused after their free call, and arrays are copied in and out of the native
 * layer by the platform implementation.
 */
internal expect object SpeexDspNative {
    fun versionString(): String

    fun resamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): SpeexResamplerHandle?

    fun resamplerCreateFrac(
        channels: Int,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
        quality: Int,
    ): SpeexResamplerHandle?

    fun resamplerError(handle: SpeexResamplerHandle): Int

    fun resamplerFree(handle: SpeexResamplerHandle)

    fun resamplerChannels(handle: SpeexResamplerHandle): Int

    fun resamplerSetRate(handle: SpeexResamplerHandle, inRate: Int, outRate: Int): Int

    fun resamplerSetRateFrac(
        handle: SpeexResamplerHandle,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
    ): Int

    fun resamplerInputRate(handle: SpeexResamplerHandle): Int

    fun resamplerOutputRate(handle: SpeexResamplerHandle): Int

    fun resamplerRatioNum(handle: SpeexResamplerHandle): Int

    fun resamplerRatioDen(handle: SpeexResamplerHandle): Int

    fun resamplerSetQuality(handle: SpeexResamplerHandle, quality: Int): Int

    fun resamplerQuality(handle: SpeexResamplerHandle): Int

    fun resamplerSetInputStride(handle: SpeexResamplerHandle, stride: Int)

    fun resamplerInputStride(handle: SpeexResamplerHandle): Int

    fun resamplerSetOutputStride(handle: SpeexResamplerHandle, stride: Int)

    fun resamplerOutputStride(handle: SpeexResamplerHandle): Int

    fun resamplerInputLatency(handle: SpeexResamplerHandle): Int

    fun resamplerOutputLatency(handle: SpeexResamplerHandle): Int

    fun resamplerSkipZeros(handle: SpeexResamplerHandle): Int

    fun resamplerResetMem(handle: SpeexResamplerHandle): Int

    fun resamplerProcess(handle: SpeexResamplerHandle, samples: ShortArray, offset: Int, count: Int): Int

    fun resamplerOutput(handle: SpeexResamplerHandle): ShortArray?

    fun resamplerStrerror(error: Int): String

    fun preprocessCreate(frameSize: Int, samplingRate: Int): SpeexPreprocessorHandle?

    fun preprocessFree(handle: SpeexPreprocessorHandle)

    fun preprocessFrameSize(handle: SpeexPreprocessorHandle): Int

    fun preprocessRun(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int): Int

    fun preprocessEstimateUpdate(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int)

    fun preprocessCtlInt(handle: SpeexPreprocessorHandle, request: Int, value: Int): Int

    fun preprocessGetInt(handle: SpeexPreprocessorHandle, request: Int): Int

    fun preprocessCtlFloat(handle: SpeexPreprocessorHandle, request: Int, value: Float): Int

    fun preprocessGetFloat(handle: SpeexPreprocessorHandle, request: Int): Float

    fun preprocessSetEcho(handle: SpeexPreprocessorHandle, echo: SpeexEchoHandle?): Int

    fun preprocessPsdSize(handle: SpeexPreprocessorHandle): Int

    fun preprocessPsd(handle: SpeexPreprocessorHandle): IntArray?

    fun preprocessNoisePsdSize(handle: SpeexPreprocessorHandle): Int

    fun preprocessNoisePsd(handle: SpeexPreprocessorHandle): IntArray?

    fun echoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): SpeexEchoHandle?

    fun echoFree(handle: SpeexEchoHandle)

    fun echoReset(handle: SpeexEchoHandle)

    fun echoFrameSize(handle: SpeexEchoHandle): Int

    fun echoMicrophones(handle: SpeexEchoHandle): Int

    fun echoSpeakers(handle: SpeexEchoHandle): Int

    fun echoCancellation(
        handle: SpeexEchoHandle,
        rec: ShortArray,
        recOffset: Int,
        play: ShortArray,
        playOffset: Int,
    ): Int

    fun echoCapture(handle: SpeexEchoHandle, rec: ShortArray, offset: Int): Int

    fun echoPlayback(handle: SpeexEchoHandle, play: ShortArray, offset: Int): Int

    fun echoOutput(handle: SpeexEchoHandle): ShortArray?

    fun echoCtlInt(handle: SpeexEchoHandle, request: Int, value: Int): Int

    fun echoGetInt(handle: SpeexEchoHandle, request: Int): Int

    fun echoImpulseResponseSize(handle: SpeexEchoHandle): Int

    fun echoImpulseResponse(handle: SpeexEchoHandle): IntArray?

    fun decorrCreate(rate: Int, channels: Int, frameSize: Int): SpeexDecorrelatorHandle?

    fun decorrFree(handle: SpeexDecorrelatorHandle)

    fun decorrChannels(handle: SpeexDecorrelatorHandle): Int

    fun decorrFrameSize(handle: SpeexDecorrelatorHandle): Int

    fun decorrProcess(handle: SpeexDecorrelatorHandle, input: ShortArray, offset: Int, count: Int, strength: Int): Int

    fun decorrOutput(handle: SpeexDecorrelatorHandle): ShortArray?

    fun jitterCreate(stepSize: Int): SpeexJitterHandle?

    fun jitterFree(handle: SpeexJitterHandle)

    fun jitterReset(handle: SpeexJitterHandle)

    fun jitterPut(
        handle: SpeexJitterHandle,
        data: ByteArray,
        timestamp: Int,
        span: Int,
        sequence: Int,
        userData: Int,
    ): Int

    fun jitterGet(handle: SpeexJitterHandle, desiredSpan: Int): Int

    fun jitterGetAnother(handle: SpeexJitterHandle): Int

    fun jitterPacket(handle: SpeexJitterHandle): ByteArray?

    fun jitterTimestamp(handle: SpeexJitterHandle): Int

    fun jitterSpan(handle: SpeexJitterHandle): Int

    fun jitterSequence(handle: SpeexJitterHandle): Int

    fun jitterUserData(handle: SpeexJitterHandle): Int

    fun jitterPointerTimestamp(handle: SpeexJitterHandle): Int

    fun jitterTick(handle: SpeexJitterHandle)

    fun jitterRemainingSpan(handle: SpeexJitterHandle, remaining: Int)

    fun jitterCtlInt(handle: SpeexJitterHandle, request: Int, value: Int): Int

    fun jitterGetInt(handle: SpeexJitterHandle, request: Int): Int

    fun ringBufferCreate(size: Int): SpeexRingBufferHandle?

    fun ringBufferFree(handle: SpeexRingBufferHandle)

    fun ringBufferWrite(handle: SpeexRingBufferHandle, data: ByteArray, offset: Int, count: Int): Int

    fun ringBufferWriteZeros(handle: SpeexRingBufferHandle, len: Int): Int

    fun ringBufferRead(handle: SpeexRingBufferHandle, len: Int): ByteArray?

    fun ringBufferAvailable(handle: SpeexRingBufferHandle): Int

    fun ringBufferResize(handle: SpeexRingBufferHandle, len: Int): Int
}

/** `XDSP_INVALID`. */
internal const val XDSP_INVALID: Int = -3

/** Resampler creation succeeded. */
internal const val XDSP_RESAMPLER_OK: Int = 0

/** Integer-valued `SPEEX_PREPROCESS_*` requests. */
internal object SpeexPreprocessRequest {
    const val SET_DENOISE = 0
    const val GET_DENOISE = 1
    const val SET_AGC = 2
    const val GET_AGC = 3
    const val SET_VAD = 4
    const val GET_VAD = 5
    const val SET_DEREVERB = 8
    const val GET_DEREVERB = 9
    const val SET_PROB_START = 14
    const val GET_PROB_START = 15
    const val SET_PROB_CONTINUE = 16
    const val GET_PROB_CONTINUE = 17
    const val SET_NOISE_SUPPRESS = 18
    const val GET_NOISE_SUPPRESS = 19
    const val SET_ECHO_SUPPRESS = 20
    const val GET_ECHO_SUPPRESS = 21
    const val SET_ECHO_SUPPRESS_ACTIVE = 22
    const val GET_ECHO_SUPPRESS_ACTIVE = 23
    const val SET_AGC_INCREMENT = 26
    const val GET_AGC_INCREMENT = 27
    const val SET_AGC_DECREMENT = 28
    const val GET_AGC_DECREMENT = 29
    const val SET_AGC_MAX_GAIN = 30
    const val GET_AGC_MAX_GAIN = 31
    const val GET_AGC_LOUDNESS = 33
    const val GET_AGC_GAIN = 35
    const val GET_PSD_SIZE = 37
    const val GET_NOISE_PSD_SIZE = 41
    const val GET_PROB = 45
    const val SET_AGC_TARGET = 46
    const val GET_AGC_TARGET = 47
}

/** Float-valued `SPEEX_PREPROCESS_*` requests. */
internal object SpeexPreprocessFloatRequest {
    const val SET_AGC_LEVEL = 6
    const val GET_AGC_LEVEL = 7
}

/** Integer-valued `SPEEX_ECHO_*` requests. */
internal object SpeexEchoRequest {
    const val GET_FRAME_SIZE = 3
    const val SET_SAMPLING_RATE = 24
    const val GET_SAMPLING_RATE = 25
    const val GET_IMPULSE_RESPONSE_SIZE = 27
}

/** Integer-valued `JITTER_BUFFER_*` requests. */
internal object SpeexJitterRequest {
    const val SET_MARGIN = 0
    const val GET_MARGIN = 1
    const val GET_AVAILABLE_COUNT = 3
    const val SET_DELAY_STEP = 6
    const val GET_DELAY_STEP = 7
    const val SET_CONCEALMENT_SIZE = 8
    const val GET_CONCEALMENT_SIZE = 9
    const val SET_MAX_LATE_RATE = 10
    const val GET_MAX_LATE_RATE = 11
    const val SET_LATE_COST = 12
    const val GET_LATE_COST = 13
}
