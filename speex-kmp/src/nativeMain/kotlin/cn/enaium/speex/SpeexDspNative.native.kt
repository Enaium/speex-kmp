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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.enaium.speex

import cnames.structs.xdsp_buffer
import cnames.structs.xdsp_decorr
import cnames.structs.xdsp_echo
import cnames.structs.xdsp_jitter
import cnames.structs.xdsp_preprocess
import cnames.structs.xdsp_resampler
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import speex_kmp.xdsp_buffer_available
import speex_kmp.xdsp_buffer_create
import speex_kmp.xdsp_buffer_data
import speex_kmp.xdsp_buffer_free
import speex_kmp.xdsp_buffer_read
import speex_kmp.xdsp_buffer_resize
import speex_kmp.xdsp_buffer_write
import speex_kmp.xdsp_buffer_write_zeros
import speex_kmp.xdsp_decorr_channels
import speex_kmp.xdsp_decorr_create
import speex_kmp.xdsp_decorr_frame_size
import speex_kmp.xdsp_decorr_free
import speex_kmp.xdsp_decorr_output
import speex_kmp.xdsp_decorr_process
import speex_kmp.xdsp_echo_cancellation
import speex_kmp.xdsp_echo_capture
import speex_kmp.xdsp_echo_create
import speex_kmp.xdsp_echo_ctl_int
import speex_kmp.xdsp_echo_frame_size
import speex_kmp.xdsp_echo_free
import speex_kmp.xdsp_echo_get_int
import speex_kmp.xdsp_echo_impulse_response
import speex_kmp.xdsp_echo_impulse_response_size
import speex_kmp.xdsp_echo_microphones
import speex_kmp.xdsp_echo_output
import speex_kmp.xdsp_echo_playback
import speex_kmp.xdsp_echo_reset
import speex_kmp.xdsp_echo_speakers
import speex_kmp.xdsp_jitter_ctl_int
import speex_kmp.xdsp_jitter_create
import speex_kmp.xdsp_jitter_free
import speex_kmp.xdsp_jitter_get
import speex_kmp.xdsp_jitter_get_another
import speex_kmp.xdsp_jitter_get_int
import speex_kmp.xdsp_jitter_packet
import speex_kmp.xdsp_jitter_pointer_timestamp
import speex_kmp.xdsp_jitter_put
import speex_kmp.xdsp_jitter_remaining_span
import speex_kmp.xdsp_jitter_reset
import speex_kmp.xdsp_jitter_sequence
import speex_kmp.xdsp_jitter_span
import speex_kmp.xdsp_jitter_tick
import speex_kmp.xdsp_jitter_timestamp
import speex_kmp.xdsp_jitter_user_data
import speex_kmp.xdsp_preprocess_create
import speex_kmp.xdsp_preprocess_ctl_float
import speex_kmp.xdsp_preprocess_ctl_int
import speex_kmp.xdsp_preprocess_estimate_update
import speex_kmp.xdsp_preprocess_frame_size
import speex_kmp.xdsp_preprocess_free
import speex_kmp.xdsp_preprocess_get_float
import speex_kmp.xdsp_preprocess_get_int
import speex_kmp.xdsp_preprocess_noise_psd
import speex_kmp.xdsp_preprocess_noise_psd_size
import speex_kmp.xdsp_preprocess_psd
import speex_kmp.xdsp_preprocess_psd_size
import speex_kmp.xdsp_preprocess_run
import speex_kmp.xdsp_preprocess_set_echo
import speex_kmp.xdsp_resampler_channels
import speex_kmp.xdsp_resampler_create
import speex_kmp.xdsp_resampler_create_frac
import speex_kmp.xdsp_resampler_error
import speex_kmp.xdsp_resampler_free
import speex_kmp.xdsp_resampler_input_latency
import speex_kmp.xdsp_resampler_input_rate
import speex_kmp.xdsp_resampler_input_stride
import speex_kmp.xdsp_resampler_output
import speex_kmp.xdsp_resampler_output_latency
import speex_kmp.xdsp_resampler_output_rate
import speex_kmp.xdsp_resampler_output_stride
import speex_kmp.xdsp_resampler_process_interleaved
import speex_kmp.xdsp_resampler_quality
import speex_kmp.xdsp_resampler_ratio_den
import speex_kmp.xdsp_resampler_ratio_num
import speex_kmp.xdsp_resampler_reset_mem
import speex_kmp.xdsp_resampler_set_input_stride
import speex_kmp.xdsp_resampler_set_output_stride
import speex_kmp.xdsp_resampler_set_quality
import speex_kmp.xdsp_resampler_set_rate
import speex_kmp.xdsp_resampler_set_rate_frac
import speex_kmp.xdsp_resampler_skip_zeros
import speex_kmp.xdsp_resampler_strerror
import speex_kmp.xdsp_version_string

internal actual class SpeexResamplerHandle actual constructor() {
    internal var pointer: CPointer<xdsp_resampler>? = null
}

internal actual class SpeexPreprocessorHandle actual constructor() {
    internal var pointer: CPointer<xdsp_preprocess>? = null
}

internal actual class SpeexEchoHandle actual constructor() {
    internal var pointer: CPointer<xdsp_echo>? = null
}

internal actual class SpeexDecorrelatorHandle actual constructor() {
    internal var pointer: CPointer<xdsp_decorr>? = null
}

internal actual class SpeexJitterHandle actual constructor() {
    internal var pointer: CPointer<xdsp_jitter>? = null
}

internal actual class SpeexRingBufferHandle actual constructor() {
    internal var pointer: CPointer<xdsp_buffer>? = null
}

private fun CPointer<ShortVar>.readShortArray(count: Int): ShortArray {
    if (count <= 0) return ShortArray(0)
    val bytes = reinterpret<ByteVar>().readBytes(count * 2)
    return ShortArray(count) { index ->
        val low = bytes[index * 2].toInt() and 0xFF
        val high = bytes[index * 2 + 1].toInt() and 0xFF
        ((high shl 8) or low).toShort()
    }
}

private fun CPointer<IntVar>.readIntArray(count: Int): IntArray {
    if (count <= 0) return IntArray(0)
    val bytes = reinterpret<ByteVar>().readBytes(count * 4)
    return IntArray(count) { index ->
        val base = index * 4
        (bytes[base].toInt() and 0xFF) or
            ((bytes[base + 1].toInt() and 0xFF) shl 8) or
            ((bytes[base + 2].toInt() and 0xFF) shl 16) or
            ((bytes[base + 3].toInt() and 0xFF) shl 24)
    }
}

internal actual object SpeexDspNative {
    actual fun versionString(): String = xdsp_version_string()?.toKString() ?: ""

    actual fun resamplerCreate(channels: Int, inRate: Int, outRate: Int, quality: Int): SpeexResamplerHandle? {
        val pointer = xdsp_resampler_create(channels.toUInt(), inRate.toUInt(), outRate.toUInt(), quality)
            ?: return null
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
        val pointer = xdsp_resampler_create_frac(
            channels.toUInt(),
            ratioNum.toUInt(),
            ratioDen.toUInt(),
            inRate.toUInt(),
            outRate.toUInt(),
            quality,
        ) ?: return null
        return SpeexResamplerHandle().also { it.pointer = pointer }
    }

    actual fun resamplerError(handle: SpeexResamplerHandle): Int = xdsp_resampler_error(handle.pointer)

    actual fun resamplerFree(handle: SpeexResamplerHandle) {
        xdsp_resampler_free(handle.pointer)
        handle.pointer = null
    }

    actual fun resamplerChannels(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_channels(handle.pointer).toInt()

    actual fun resamplerSetRate(handle: SpeexResamplerHandle, inRate: Int, outRate: Int): Int =
        xdsp_resampler_set_rate(handle.pointer, inRate.toUInt(), outRate.toUInt())

    actual fun resamplerSetRateFrac(
        handle: SpeexResamplerHandle,
        ratioNum: Int,
        ratioDen: Int,
        inRate: Int,
        outRate: Int,
    ): Int = xdsp_resampler_set_rate_frac(
        handle.pointer,
        ratioNum.toUInt(),
        ratioDen.toUInt(),
        inRate.toUInt(),
        outRate.toUInt(),
    )

    actual fun resamplerInputRate(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_input_rate(handle.pointer).toInt()

    actual fun resamplerOutputRate(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_output_rate(handle.pointer).toInt()

    actual fun resamplerRatioNum(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_ratio_num(handle.pointer).toInt()

    actual fun resamplerRatioDen(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_ratio_den(handle.pointer).toInt()

    actual fun resamplerSetQuality(handle: SpeexResamplerHandle, quality: Int): Int =
        xdsp_resampler_set_quality(handle.pointer, quality)

    actual fun resamplerQuality(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_quality(handle.pointer)

    actual fun resamplerSetInputStride(handle: SpeexResamplerHandle, stride: Int) =
        xdsp_resampler_set_input_stride(handle.pointer, stride.toUInt())

    actual fun resamplerInputStride(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_input_stride(handle.pointer).toInt()

    actual fun resamplerSetOutputStride(handle: SpeexResamplerHandle, stride: Int) =
        xdsp_resampler_set_output_stride(handle.pointer, stride.toUInt())

    actual fun resamplerOutputStride(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_output_stride(handle.pointer).toInt()

    actual fun resamplerInputLatency(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_input_latency(handle.pointer)

    actual fun resamplerOutputLatency(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_output_latency(handle.pointer)

    actual fun resamplerSkipZeros(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_skip_zeros(handle.pointer)

    actual fun resamplerResetMem(handle: SpeexResamplerHandle): Int =
        xdsp_resampler_reset_mem(handle.pointer)

    actual fun resamplerProcess(
        handle: SpeexResamplerHandle,
        samples: ShortArray,
        offset: Int,
        count: Int,
    ): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (count == 0) return xdsp_resampler_process_interleaved(pointer, null, 0)
        if (offset < 0 || count < 0 || offset > samples.size) return XDSP_INVALID
        return samples.usePinned {
            xdsp_resampler_process_interleaved(pointer, it.addressOf(offset), count)
        }
    }

    actual fun resamplerOutput(handle: SpeexResamplerHandle): ShortArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val outLen = alloc<IntVar>()
            val output = xdsp_resampler_output(pointer, outLen.ptr) ?: return@memScoped null
            val channels = xdsp_resampler_channels(pointer).toInt()
            output.readShortArray(outLen.value * channels)
        }
    }

    actual fun resamplerStrerror(error: Int): String = xdsp_resampler_strerror(error)?.toKString() ?: ""

    actual fun preprocessCreate(frameSize: Int, samplingRate: Int): SpeexPreprocessorHandle? {
        val pointer = xdsp_preprocess_create(frameSize, samplingRate) ?: return null
        return SpeexPreprocessorHandle().also { it.pointer = pointer }
    }

    actual fun preprocessFree(handle: SpeexPreprocessorHandle) {
        xdsp_preprocess_free(handle.pointer)
        handle.pointer = null
    }

    actual fun preprocessFrameSize(handle: SpeexPreprocessorHandle): Int =
        xdsp_preprocess_frame_size(handle.pointer)

    actual fun preprocessRun(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (offset < 0 || count <= 0 || offset > samples.size - count) return XDSP_INVALID
        return samples.usePinned {
            xdsp_preprocess_run(pointer, it.addressOf(offset), count)
        }
    }

    actual fun preprocessEstimateUpdate(handle: SpeexPreprocessorHandle, samples: ShortArray, offset: Int, count: Int) {
        val pointer = handle.pointer ?: return
        if (offset < 0 || count <= 0 || offset > samples.size - count) return
        samples.usePinned {
            xdsp_preprocess_estimate_update(pointer, it.addressOf(offset), count)
        }
    }

    actual fun preprocessCtlInt(handle: SpeexPreprocessorHandle, request: Int, value: Int): Int =
        xdsp_preprocess_ctl_int(handle.pointer, request, value)

    actual fun preprocessGetInt(handle: SpeexPreprocessorHandle, request: Int): Int =
        xdsp_preprocess_get_int(handle.pointer, request)

    actual fun preprocessCtlFloat(handle: SpeexPreprocessorHandle, request: Int, value: Float): Int =
        xdsp_preprocess_ctl_float(handle.pointer, request, value)

    actual fun preprocessGetFloat(handle: SpeexPreprocessorHandle, request: Int): Float =
        xdsp_preprocess_get_float(handle.pointer, request)

    actual fun preprocessSetEcho(handle: SpeexPreprocessorHandle, echo: SpeexEchoHandle?) =
        xdsp_preprocess_set_echo(handle.pointer, echo?.pointer)

    actual fun preprocessPsdSize(handle: SpeexPreprocessorHandle): Int =
        xdsp_preprocess_psd_size(handle.pointer)

    actual fun preprocessPsd(handle: SpeexPreprocessorHandle): IntArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val size = alloc<IntVar>()
            val psd = xdsp_preprocess_psd(pointer, size.ptr) ?: return@memScoped null
            psd.readIntArray(size.value)
        }
    }

    actual fun preprocessNoisePsdSize(handle: SpeexPreprocessorHandle): Int =
        xdsp_preprocess_noise_psd_size(handle.pointer)

    actual fun preprocessNoisePsd(handle: SpeexPreprocessorHandle): IntArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val size = alloc<IntVar>()
            val psd = xdsp_preprocess_noise_psd(pointer, size.ptr) ?: return@memScoped null
            psd.readIntArray(size.value)
        }
    }

    actual fun echoCreate(frameSize: Int, filterLength: Int, microphones: Int, speakers: Int): SpeexEchoHandle? {
        val pointer = xdsp_echo_create(frameSize, filterLength, microphones, speakers) ?: return null
        return SpeexEchoHandle().also { it.pointer = pointer }
    }

    actual fun echoFree(handle: SpeexEchoHandle) {
        xdsp_echo_free(handle.pointer)
        handle.pointer = null
    }

    actual fun echoReset(handle: SpeexEchoHandle) = xdsp_echo_reset(handle.pointer)

    actual fun echoFrameSize(handle: SpeexEchoHandle): Int = xdsp_echo_frame_size(handle.pointer)

    actual fun echoMicrophones(handle: SpeexEchoHandle): Int = xdsp_echo_microphones(handle.pointer)

    actual fun echoSpeakers(handle: SpeexEchoHandle): Int = xdsp_echo_speakers(handle.pointer)

    actual fun echoCancellation(
        handle: SpeexEchoHandle,
        rec: ShortArray,
        recOffset: Int,
        play: ShortArray,
        playOffset: Int,
    ): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (recOffset < 0 || playOffset < 0) return XDSP_INVALID
        return rec.usePinned { recPinned ->
            play.usePinned { playPinned ->
                xdsp_echo_cancellation(
                    pointer,
                    recPinned.addressOf(recOffset),
                    rec.size - recOffset,
                    playPinned.addressOf(playOffset),
                    play.size - playOffset,
                )
            }
        }
    }

    actual fun echoCapture(handle: SpeexEchoHandle, rec: ShortArray, offset: Int): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (offset < 0) return XDSP_INVALID
        return rec.usePinned {
            xdsp_echo_capture(pointer, it.addressOf(offset), rec.size - offset)
        }
    }

    actual fun echoPlayback(handle: SpeexEchoHandle, play: ShortArray, offset: Int): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (offset < 0) return XDSP_INVALID
        return play.usePinned {
            xdsp_echo_playback(pointer, it.addressOf(offset), play.size - offset)
        }
    }

    actual fun echoOutput(handle: SpeexEchoHandle): ShortArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val count = alloc<IntVar>()
            val output = xdsp_echo_output(pointer, count.ptr) ?: return@memScoped null
            output.readShortArray(count.value)
        }
    }

    actual fun echoCtlInt(handle: SpeexEchoHandle, request: Int, value: Int): Int =
        xdsp_echo_ctl_int(handle.pointer, request, value)

    actual fun echoGetInt(handle: SpeexEchoHandle, request: Int): Int =
        xdsp_echo_get_int(handle.pointer, request)

    actual fun echoImpulseResponseSize(handle: SpeexEchoHandle): Int =
        xdsp_echo_impulse_response_size(handle.pointer)

    actual fun echoImpulseResponse(handle: SpeexEchoHandle): IntArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val size = alloc<IntVar>()
            val impulse = xdsp_echo_impulse_response(pointer, size.ptr) ?: return@memScoped null
            impulse.readIntArray(size.value)
        }
    }

    actual fun decorrCreate(rate: Int, channels: Int, frameSize: Int): SpeexDecorrelatorHandle? {
        val pointer = xdsp_decorr_create(rate, channels, frameSize) ?: return null
        return SpeexDecorrelatorHandle().also { it.pointer = pointer }
    }

    actual fun decorrFree(handle: SpeexDecorrelatorHandle) {
        xdsp_decorr_free(handle.pointer)
        handle.pointer = null
    }

    actual fun decorrChannels(handle: SpeexDecorrelatorHandle): Int = xdsp_decorr_channels(handle.pointer)

    actual fun decorrFrameSize(handle: SpeexDecorrelatorHandle): Int = xdsp_decorr_frame_size(handle.pointer)

    actual fun decorrProcess(
        handle: SpeexDecorrelatorHandle,
        input: ShortArray,
        offset: Int,
        count: Int,
        strength: Int,
    ): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (offset < 0 || count <= 0 || offset > input.size - count) return XDSP_INVALID
        return input.usePinned {
            xdsp_decorr_process(pointer, it.addressOf(offset), count, strength)
        }
    }

    actual fun decorrOutput(handle: SpeexDecorrelatorHandle): ShortArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val count = alloc<IntVar>()
            val output = xdsp_decorr_output(pointer, count.ptr) ?: return@memScoped null
            output.readShortArray(count.value)
        }
    }

    actual fun jitterCreate(stepSize: Int): SpeexJitterHandle? {
        val pointer = xdsp_jitter_create(stepSize) ?: return null
        return SpeexJitterHandle().also { it.pointer = pointer }
    }

    actual fun jitterFree(handle: SpeexJitterHandle) {
        xdsp_jitter_free(handle.pointer)
        handle.pointer = null
    }

    actual fun jitterReset(handle: SpeexJitterHandle) = xdsp_jitter_reset(handle.pointer)

    actual fun jitterPut(
        handle: SpeexJitterHandle,
        data: ByteArray,
        timestamp: Int,
        span: Int,
        sequence: Int,
        userData: Int,
    ): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (data.isEmpty()) {
            return xdsp_jitter_put(
                pointer,
                null,
                0,
                timestamp.toUInt(),
                span.toUInt(),
                sequence.toUShort(),
                userData.toUInt(),
            )
        }
        return data.usePinned {
            xdsp_jitter_put(
                pointer,
                it.addressOf(0).reinterpret(),
                data.size,
                timestamp.toUInt(),
                span.toUInt(),
                sequence.toUShort(),
                userData.toUInt(),
            )
        }
    }

    actual fun jitterGet(handle: SpeexJitterHandle, desiredSpan: Int): Int =
        xdsp_jitter_get(handle.pointer, desiredSpan)

    actual fun jitterGetAnother(handle: SpeexJitterHandle): Int = xdsp_jitter_get_another(handle.pointer)

    actual fun jitterPacket(handle: SpeexJitterHandle): ByteArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val length = alloc<IntVar>()
            val data = xdsp_jitter_packet(pointer, length.ptr) ?: return@memScoped null
            if (length.value <= 0) null else data.readBytes(length.value)
        }
    }

    actual fun jitterTimestamp(handle: SpeexJitterHandle): Int =
        xdsp_jitter_timestamp(handle.pointer).toInt()

    actual fun jitterSpan(handle: SpeexJitterHandle): Int = xdsp_jitter_span(handle.pointer).toInt()

    actual fun jitterSequence(handle: SpeexJitterHandle): Int =
        xdsp_jitter_sequence(handle.pointer).toInt()

    actual fun jitterUserData(handle: SpeexJitterHandle): Int =
        xdsp_jitter_user_data(handle.pointer).toInt()

    actual fun jitterPointerTimestamp(handle: SpeexJitterHandle): Int =
        xdsp_jitter_pointer_timestamp(handle.pointer)

    actual fun jitterTick(handle: SpeexJitterHandle) = xdsp_jitter_tick(handle.pointer)

    actual fun jitterRemainingSpan(handle: SpeexJitterHandle, remaining: Int) =
        xdsp_jitter_remaining_span(handle.pointer, remaining.toUInt())

    actual fun jitterCtlInt(handle: SpeexJitterHandle, request: Int, value: Int): Int =
        xdsp_jitter_ctl_int(handle.pointer, request, value)

    actual fun jitterGetInt(handle: SpeexJitterHandle, request: Int): Int =
        xdsp_jitter_get_int(handle.pointer, request)

    actual fun ringBufferCreate(size: Int): SpeexRingBufferHandle? {
        val pointer = xdsp_buffer_create(size) ?: return null
        return SpeexRingBufferHandle().also { it.pointer = pointer }
    }

    actual fun ringBufferFree(handle: SpeexRingBufferHandle) {
        xdsp_buffer_free(handle.pointer)
        handle.pointer = null
    }

    actual fun ringBufferWrite(handle: SpeexRingBufferHandle, data: ByteArray, offset: Int, count: Int): Int {
        val pointer = handle.pointer ?: return XDSP_INVALID
        if (offset < 0 || count < 0 || offset > data.size - count) return XDSP_INVALID
        if (count == 0) return xdsp_buffer_write(pointer, null, 0)
        return data.usePinned {
            xdsp_buffer_write(pointer, it.addressOf(offset).reinterpret(), count)
        }
    }

    actual fun ringBufferWriteZeros(handle: SpeexRingBufferHandle, len: Int): Int =
        xdsp_buffer_write_zeros(handle.pointer, len)

    actual fun ringBufferRead(handle: SpeexRingBufferHandle, len: Int): ByteArray? {
        val pointer = handle.pointer ?: return null
        val result = xdsp_buffer_read(pointer, len)
        if (result < 0) return null
        return memScoped {
            val size = alloc<IntVar>()
            val data = xdsp_buffer_data(pointer, size.ptr) ?: return@memScoped null
            if (size.value <= 0) ByteArray(0) else data.readBytes(size.value)
        }
    }

    actual fun ringBufferAvailable(handle: SpeexRingBufferHandle): Int =
        xdsp_buffer_available(handle.pointer)

    actual fun ringBufferResize(handle: SpeexRingBufferHandle, len: Int): Int =
        xdsp_buffer_resize(handle.pointer, len)
}
