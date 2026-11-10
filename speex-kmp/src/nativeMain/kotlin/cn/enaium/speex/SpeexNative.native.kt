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

import cnames.structs.xk_bits
import cnames.structs.xk_decoder
import cnames.structs.xk_encoder
import cnames.structs.xk_header
import cnames.structs.xk_stereo
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import speex_kmp.xk_bits_advance
import speex_kmp.xk_bits_create
import speex_kmp.xk_bits_free
import speex_kmp.xk_bits_insert_terminator
import speex_kmp.xk_bits_nbytes
import speex_kmp.xk_bits_pack
import speex_kmp.xk_bits_peek
import speex_kmp.xk_bits_peek_unsigned
import speex_kmp.xk_bits_read_from
import speex_kmp.xk_bits_read_whole_bytes
import speex_kmp.xk_bits_remaining
import speex_kmp.xk_bits_reset
import speex_kmp.xk_bits_rewind
import speex_kmp.xk_bits_unpack_signed
import speex_kmp.xk_bits_unpack_unsigned
import speex_kmp.xk_bits_write
import speex_kmp.xk_decoder_create
import speex_kmp.xk_decoder_ctl
import speex_kmp.xk_decoder_decode
import speex_kmp.xk_decoder_decode_stereo
import speex_kmp.xk_decoder_frame_size
import speex_kmp.xk_decoder_free
import speex_kmp.xk_decoder_get
import speex_kmp.xk_decoder_samples
import speex_kmp.xk_encoder_create
import speex_kmp.xk_encoder_ctl
import speex_kmp.xk_encoder_encode
import speex_kmp.xk_encoder_encode_stereo
import speex_kmp.xk_encoder_frame_size
import speex_kmp.xk_encoder_free
import speex_kmp.xk_encoder_get
import speex_kmp.xk_encoder_get_relative_quality
import speex_kmp.xk_encoder_get_vbr_quality
import speex_kmp.xk_encoder_packet
import speex_kmp.xk_encoder_set_vbr_quality
import speex_kmp.xk_header_create
import speex_kmp.xk_header_free
import speex_kmp.xk_header_get_int
import speex_kmp.xk_header_packet
import speex_kmp.xk_header_parse
import speex_kmp.xk_header_set_int
import speex_kmp.xk_header_string
import speex_kmp.xk_mode_frame_size
import speex_kmp.xk_mode_name
import speex_kmp.xk_mode_submode_bits
import speex_kmp.xk_stereo_create
import speex_kmp.xk_stereo_decode
import speex_kmp.xk_stereo_encode
import speex_kmp.xk_stereo_free
import speex_kmp.xk_stereo_reset
import speex_kmp.xk_version_extra
import speex_kmp.xk_version_major
import speex_kmp.xk_version_micro
import speex_kmp.xk_version_minor
import speex_kmp.xk_version_string

internal actual class SpeexBitsHandle actual constructor() {
    internal var pointer: CPointer<xk_bits>? = null
}

internal actual class SpeexEncoderHandle actual constructor() {
    internal var pointer: CPointer<xk_encoder>? = null
}

internal actual class SpeexDecoderHandle actual constructor() {
    internal var pointer: CPointer<xk_decoder>? = null
}

internal actual class SpeexStereoHandle actual constructor() {
    internal var pointer: CPointer<xk_stereo>? = null
}

internal actual class SpeexHeaderHandle actual constructor() {
    internal var pointer: CPointer<xk_header>? = null
}

internal actual object SpeexNative {
    actual fun versionString(): String = xk_version_string()?.toKString() ?: ""

    actual fun versionMajor(): Int = xk_version_major()

    actual fun versionMinor(): Int = xk_version_minor()

    actual fun versionMicro(): Int = xk_version_micro()

    actual fun versionExtra(): String = xk_version_extra()?.toKString() ?: ""

    actual fun modeFrameSize(mode: Int): Int = xk_mode_frame_size(mode)

    actual fun modeSubmodeBits(mode: Int, submode: Int): Int = xk_mode_submode_bits(mode, submode)

    actual fun modeName(mode: Int): String = xk_mode_name(mode)?.toKString() ?: ""

    actual fun bitsCreate(): SpeexBitsHandle? {
        val pointer = xk_bits_create() ?: return null
        return SpeexBitsHandle().also { it.pointer = pointer }
    }

    actual fun bitsFree(handle: SpeexBitsHandle) {
        xk_bits_free(handle.pointer)
        handle.pointer = null
    }

    actual fun bitsReset(handle: SpeexBitsHandle) = xk_bits_reset(handle.pointer)

    actual fun bitsRewind(handle: SpeexBitsHandle) = xk_bits_rewind(handle.pointer)

    actual fun bitsReadFrom(handle: SpeexBitsHandle, data: ByteArray): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (data.isEmpty()) return xk_bits_read_from(pointer, null, 0)
        return data.usePinned {
            xk_bits_read_from(pointer, it.addressOf(0).reinterpret(), data.size)
        }
    }

    actual fun bitsReadWholeBytes(handle: SpeexBitsHandle, data: ByteArray): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (data.isEmpty()) return xk_bits_read_whole_bytes(pointer, null, 0)
        return data.usePinned {
            xk_bits_read_whole_bytes(pointer, it.addressOf(0).reinterpret(), data.size)
        }
    }

    actual fun bitsPack(handle: SpeexBitsHandle, data: Int, nbits: Int) =
        xk_bits_pack(handle.pointer, data, nbits)

    actual fun bitsUnpackSigned(handle: SpeexBitsHandle, nbits: Int): Int =
        xk_bits_unpack_signed(handle.pointer, nbits)

    actual fun bitsUnpackUnsigned(handle: SpeexBitsHandle, nbits: Int): Int =
        xk_bits_unpack_unsigned(handle.pointer, nbits).toInt()

    actual fun bitsPeek(handle: SpeexBitsHandle): Int = xk_bits_peek(handle.pointer)

    actual fun bitsPeekUnsigned(handle: SpeexBitsHandle, nbits: Int): Int =
        xk_bits_peek_unsigned(handle.pointer, nbits).toInt()

    actual fun bitsAdvance(handle: SpeexBitsHandle, nbits: Int) =
        xk_bits_advance(handle.pointer, nbits)

    actual fun bitsRemaining(handle: SpeexBitsHandle): Int = xk_bits_remaining(handle.pointer)

    actual fun bitsNbytes(handle: SpeexBitsHandle): Int = xk_bits_nbytes(handle.pointer)

    actual fun bitsInsertTerminator(handle: SpeexBitsHandle) =
        xk_bits_insert_terminator(handle.pointer)

    actual fun bitsWrite(handle: SpeexBitsHandle): ByteArray {
        val pointer = handle.pointer ?: return ByteArray(0)
        val size = xk_bits_nbytes(pointer)
        if (size <= 0) return ByteArray(0)
        val out = ByteArray(size)
        val written = out.usePinned {
            xk_bits_write(pointer, it.addressOf(0).reinterpret(), size)
        }
        return if (written in 1..size) out.copyOf(written) else ByteArray(0)
    }

    actual fun encoderCreate(mode: Int): SpeexEncoderHandle? {
        val pointer = xk_encoder_create(mode) ?: return null
        return SpeexEncoderHandle().also { it.pointer = pointer }
    }

    actual fun encoderFree(handle: SpeexEncoderHandle) {
        xk_encoder_free(handle.pointer)
        handle.pointer = null
    }

    actual fun encoderFrameSize(handle: SpeexEncoderHandle): Int = xk_encoder_frame_size(handle.pointer)

    actual fun encoderEncode(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (offset < 0 || count <= 0 || offset > samples.size - count) return XK_INVALID
        return samples.usePinned {
            xk_encoder_encode(pointer, it.addressOf(offset), count)
        }
    }

    actual fun encoderEncodeStereo(handle: SpeexEncoderHandle, samples: ShortArray, offset: Int, count: Int): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (offset < 0 || count <= 0 || offset > samples.size - count) return XK_INVALID
        return samples.usePinned {
            xk_encoder_encode_stereo(pointer, it.addressOf(offset), count)
        }
    }

    actual fun encoderPacket(handle: SpeexEncoderHandle): ByteArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val size = alloc<IntVar>()
            val packet = xk_encoder_packet(pointer, size.ptr) ?: return@memScoped null
            if (size.value <= 0) null else packet.reinterpret<ByteVar>().readBytes(size.value)
        }
    }

    actual fun encoderCtl(handle: SpeexEncoderHandle, request: Int, value: Int): Int =
        xk_encoder_ctl(handle.pointer, request, value)

    actual fun encoderGet(handle: SpeexEncoderHandle, request: Int): Int =
        xk_encoder_get(handle.pointer, request)

    actual fun encoderSetVbrQuality(handle: SpeexEncoderHandle, value: Float): Int =
        xk_encoder_set_vbr_quality(handle.pointer, value)

    actual fun encoderGetVbrQuality(handle: SpeexEncoderHandle): Float =
        xk_encoder_get_vbr_quality(handle.pointer)

    actual fun encoderGetRelativeQuality(handle: SpeexEncoderHandle): Float =
        xk_encoder_get_relative_quality(handle.pointer)

    actual fun decoderCreate(mode: Int): SpeexDecoderHandle? {
        val pointer = xk_decoder_create(mode) ?: return null
        return SpeexDecoderHandle().also { it.pointer = pointer }
    }

    actual fun decoderFree(handle: SpeexDecoderHandle) {
        xk_decoder_free(handle.pointer)
        handle.pointer = null
    }

    actual fun decoderFrameSize(handle: SpeexDecoderHandle): Int = xk_decoder_frame_size(handle.pointer)

    actual fun decoderDecode(handle: SpeexDecoderHandle, packet: ByteArray?): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (packet == null || packet.isEmpty()) return xk_decoder_decode(pointer, null, 0)
        return packet.usePinned {
            xk_decoder_decode(pointer, it.addressOf(0).reinterpret(), packet.size)
        }
    }

    actual fun decoderDecodeStereo(
        handle: SpeexDecoderHandle,
        packet: ByteArray?,
        stereo: SpeexStereoHandle,
    ): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        val stereoPointer = stereo.pointer ?: return XK_INVALID
        if (packet == null || packet.isEmpty()) {
            return xk_decoder_decode_stereo(pointer, null, 0, stereoPointer)
        }
        return packet.usePinned {
            xk_decoder_decode_stereo(pointer, it.addressOf(0).reinterpret(), packet.size, stereoPointer)
        }
    }

    actual fun decoderSamples(handle: SpeexDecoderHandle): ShortArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val count = alloc<IntVar>()
            val samples = xk_decoder_samples(pointer, count.ptr) ?: return@memScoped null
            val size = count.value
            if (size <= 0) return@memScoped null
            val bytes = samples.reinterpret<ByteVar>().readBytes(size * 2)
            ShortArray(size) { index ->
                val low = bytes[index * 2].toInt() and 0xFF
                val high = bytes[index * 2 + 1].toInt()
                ((high shl 8) or low).toShort()
            }
        }
    }

    actual fun decoderCtl(handle: SpeexDecoderHandle, request: Int, value: Int): Int =
        xk_decoder_ctl(handle.pointer, request, value)

    actual fun decoderGet(handle: SpeexDecoderHandle, request: Int): Int =
        xk_decoder_get(handle.pointer, request)

    actual fun stereoCreate(): SpeexStereoHandle? {
        val pointer = xk_stereo_create() ?: return null
        return SpeexStereoHandle().also { it.pointer = pointer }
    }

    actual fun stereoFree(handle: SpeexStereoHandle) {
        xk_stereo_free(handle.pointer)
        handle.pointer = null
    }

    actual fun stereoReset(handle: SpeexStereoHandle) = xk_stereo_reset(handle.pointer)

    actual fun stereoEncode(
        handle: SpeexStereoHandle,
        samples: ShortArray,
        offset: Int,
        count: Int,
        bits: SpeexBitsHandle,
    ): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        val bitsPointer = bits.pointer ?: return XK_INVALID
        if (offset < 0 || count <= 0 || offset > samples.size - count) return XK_INVALID
        return samples.usePinned {
            xk_stereo_encode(pointer, it.addressOf(offset), count, bitsPointer)
        }
    }

    actual fun stereoDecode(handle: SpeexStereoHandle, samples: ShortArray, count: Int): Int {
        val pointer = handle.pointer ?: return XK_INVALID
        if (count <= 0 || count > samples.size) return XK_INVALID
        return samples.usePinned {
            xk_stereo_decode(pointer, it.addressOf(0), count)
        }
    }

    actual fun headerCreate(rate: Int, channels: Int, mode: Int): SpeexHeaderHandle? {
        val pointer = xk_header_create(rate, channels, mode) ?: return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerParse(packet: ByteArray): SpeexHeaderHandle? {
        if (packet.isEmpty()) return null
        val pointer = packet.usePinned {
            xk_header_parse(it.addressOf(0).reinterpret(), packet.size)
        } ?: return null
        return SpeexHeaderHandle().also { it.pointer = pointer }
    }

    actual fun headerFree(handle: SpeexHeaderHandle) {
        xk_header_free(handle.pointer)
        handle.pointer = null
    }

    actual fun headerGetInt(handle: SpeexHeaderHandle, field: Int): Int =
        xk_header_get_int(handle.pointer, field)

    actual fun headerSetInt(handle: SpeexHeaderHandle, field: Int, value: Int): Int =
        xk_header_set_int(handle.pointer, field, value)

    actual fun headerString(handle: SpeexHeaderHandle, which: Int): String =
        xk_header_string(handle.pointer, which)?.toKString() ?: ""

    actual fun headerPacket(handle: SpeexHeaderHandle): ByteArray? {
        val pointer = handle.pointer ?: return null
        return memScoped {
            val size = alloc<IntVar>()
            val packet = xk_header_packet(pointer, size.ptr) ?: return@memScoped null
            if (size.value <= 0) null else packet.reinterpret<ByteVar>().readBytes(size.value)
        }
    }
}
