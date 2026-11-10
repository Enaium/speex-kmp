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

/** Speex codec mode: the sampling rate (and therefore frame size) of the stream. */
enum class SpeexMode(
    val id: Int,
    /** Samples per second this mode operates at. */
    val sampleRate: Int,
) {
    /** 8 kHz narrowband, 160 samples (20 ms) per frame. */
    NARROWBAND(0, 8_000),

    /** 16 kHz wideband, 320 samples (20 ms) per frame. */
    WIDEBAND(1, 16_000),

    /** 32 kHz ultra-wideband, 640 samples (20 ms) per frame. */
    ULTRA_WIDEBAND(2, 32_000),
    ;

    /** Name reported by the bundled library, e.g. `"narrowband (8 kHz)"`. */
    val libraryName: String get() = SpeexNative.modeName(id)

    /** Frame size of this mode in samples, as reported by `SPEEX_MODE_FRAME_SIZE`. */
    val frameSize: Int get() = SpeexNative.modeFrameSize(id)

    companion object {
        internal fun fromId(id: Int): SpeexMode =
            entries.firstOrNull { it.id == id } ?: throw SpeexException("unknown Speex mode $id")

        /** The mode used for `sampleRate`; Speex supports 8, 16 and 32 kHz only. */
        fun fromSampleRate(sampleRate: Int): SpeexMode = when (sampleRate) {
            8_000 -> NARROWBAND
            16_000 -> WIDEBAND
            32_000 -> ULTRA_WIDEBAND
            else -> throw SpeexException(
                "unsupported Speex sample rate $sampleRate; use 8000, 16000 or 32000",
            )
        }
    }
}

/** Version information of the bundled Speex build and of this binding. */
object Speex {
    /** Version of the Kotlin bindings, independent of the Speex library version. */
    const val VERSION: String = "1.2.1.1"

    /** Speex library version reported by `SPEEX_LIB_GET_VERSION_STRING`, e.g. `"1.2.1"`. */
    val libraryVersion: String by lazy { SpeexNative.versionString() }

    val versionMajor: Int by lazy { SpeexNative.versionMajor() }

    val versionMinor: Int by lazy { SpeexNative.versionMinor() }

    val versionMicro: Int by lazy { SpeexNative.versionMicro() }

    /** Version suffix; normally empty. */
    val versionExtra: String by lazy { SpeexNative.versionExtra() }

    /** Frame size, in samples, used by [mode]. */
    fun frameSize(mode: SpeexMode): Int = mode.frameSize

    /**
     * Number of bits used by one sub-mode frame; -1 for a sub-mode the mode
     * does not define. Sub-mode 0 means "sub-mode header included".
     */
    fun submodeBits(mode: SpeexMode, submode: Int): Int = SpeexNative.modeSubmodeBits(mode.id, submode)

    override fun toString(): String = "Speex $libraryVersion (bindings $VERSION)"
}
