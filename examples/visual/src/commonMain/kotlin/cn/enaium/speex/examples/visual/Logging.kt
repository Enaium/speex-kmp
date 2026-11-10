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

package cn.enaium.speex.examples.visual

import cn.enaium.sdl.SDLLog
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/** Reports a line to the console and to SDL's log (logcat on Android). */
internal fun report(message: String) {
    println(message)
    SDLLog.info(message)
}

/** Formats [value] with [decimals] digits after the point; common to all targets. */
internal fun fixed(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = round(value * factor).toLong()
    val magnitude = if (scaled < 0) -scaled else scaled
    val whole = magnitude / factor
    val fraction = (magnitude % factor).toString().padStart(decimals, '0')
    return "${if (scaled < 0) "-" else ""}$whole.$fraction"
}

/** Formats a linear amplitude as dBFS; silence is spelled out. */
internal fun dbText(amplitude: Float): String =
    if (amplitude <= 0f || !amplitude.isFinite()) "-inf" else fixed(dbfs(amplitude).toDouble(), 1)

/** Linear amplitude to dBFS. */
internal fun dbfs(amplitude: Float): Float =
    if (amplitude <= 0f || !amplitude.isFinite()) -120f else 20f * log10(amplitude.toDouble()).toFloat()

/** dBFS to linear amplitude. */
internal fun dbfsToAmplitude(db: Float): Float = 10f.pow(db / 20f)

/** Root-mean-square amplitude of [samples]. */
internal fun rms(samples: FloatArray, count: Int = samples.size): Float {
    if (count <= 0) return 0f
    var sum = 0.0
    for (i in 0 until count) {
        val value = samples[i].toDouble()
        sum += value * value
    }
    return kotlin.math.sqrt(sum / count).toFloat()
}

/** Root-mean-square amplitude of 16-bit [samples], scaled to `-1..1`. */
internal fun rmsPcm(samples: ShortArray, count: Int = samples.size): Float {
    if (count <= 0) return 0f
    var sum = 0.0
    for (i in 0 until count) {
        val value = samples[i] / 32768.0
        sum += value * value
    }
    return kotlin.math.sqrt(sum / count).toFloat()
}

/** 16-bit PCM sample to the `-1..1` range the plots use. */
internal fun Short.toAmplitude(): Float = this / 32768f

/** Clamps [value] to the signed 16-bit range. */
internal fun Float.toPcm(): Short {
    val scaled = (this * 32767f).toInt()
    return scaled.coerceIn(-32768, 32767).toShort()
}
