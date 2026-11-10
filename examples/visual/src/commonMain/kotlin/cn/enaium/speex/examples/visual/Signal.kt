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

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * A seeded, speech-like source: a rolled-off glottal pulse train gated by a
 * syllable envelope, with optional white noise and 50 Hz hum.
 *
 * The output is deterministic for a given seed, so screenshots and headless
 * runs are comparable.
 */
internal class SyntheticSpeech(
    private val sampleRate: Int,
    private val pitchHz: Double = 120.0,
    seed: Int = 0x5EED,
) {
    private val random = Random(seed)
    private var pitchPhase = 0.0
    private var syllablePhase = 0.0
    private var syllableTarget = 0.4f
    private var envelope = 0f
    private var voiced = 0f
    private var humPhase = 0.0

    /** Fills the first [count] samples of [destination]. */
    fun next(
        destination: FloatArray,
        count: Int,
        amplitude: Float,
        noise: Float = 0f,
        hum: Float = 0f,
    ) {
        val pitchStep = pitchHz / sampleRate
        val syllableStep = SYLLABLE_HZ / sampleRate
        val humStep = 2.0 * PI * 50.0 / sampleRate
        for (i in 0 until count) {
            pitchPhase += pitchStep
            if (pitchPhase >= 1.0) pitchPhase -= 1.0

            syllablePhase += syllableStep
            if (syllablePhase >= 1.0) {
                syllablePhase -= 1.0
                syllableTarget = MIN_SYLLABLE + random.nextFloat() * (1f - MIN_SYLLABLE)
            }
            envelope += (syllableTarget - envelope) * ENVELOPE_RATE

            // Sawtooth pulse train with a one-pole roll-off. The roll-off keeps
            // the harmonics from aliasing while leaving enough edge to look
            // like speech on the waveform.
            voiced += ((2f * pitchPhase.toFloat() - 1f) - voiced) * VOICED_RATE

            humPhase += humStep
            if (humPhase >= 2.0 * PI) humPhase -= 2.0 * PI

            val speech = voiced * envelope * amplitude
            val noiseSample = (random.nextFloat() * 2f - 1f) * noise
            val humSample = sin(humPhase).toFloat() * hum
            destination[i] = speech + noiseSample + humSample
        }
    }

    /** Generates one frame and converts it to 16-bit PCM. */
    fun nextPcm(
        count: Int,
        amplitude: Float,
        noise: Float = 0f,
        hum: Float = 0f,
    ): ShortArray {
        val floatFrame = FloatArray(count)
        next(floatFrame, count, amplitude, noise, hum)
        return ShortArray(count) { floatFrame[it].toPcm() }
    }

    private companion object {
        const val SYLLABLE_HZ = 4.0
        const val MIN_SYLLABLE = 0.25f
        const val ENVELOPE_RATE = 0.0025f
        const val VOICED_RATE = 0.12f
    }
}

/** Converts the first [count] samples of a 16-bit frame to `-1..1`. */
internal fun ShortArray.toFloats(destination: FloatArray, count: Int = size) {
    for (i in 0 until count) destination[i] = this[i].toAmplitude()
}

/** Converts the first [count] samples of a float frame to 16-bit PCM. */
internal fun FloatArray.toPcm(count: Int = size): ShortArray = ShortArray(count) { this[it].toPcm() }

/** Adds two 16-bit frames sample by sample, clamping to the sample range. */
internal fun mixPcm(a: ShortArray, b: ShortArray, count: Int): ShortArray =
    ShortArray(count) { (a[it].toInt() + b[it].toInt()).coerceIn(-32768, 32767).toShort() }

/** Scales a 16-bit frame; [gain] is linear. */
internal fun scalePcm(samples: ShortArray, count: Int, gain: Float): ShortArray =
    ShortArray(count) { (samples[it] * gain).toInt().coerceIn(-32768, 32767).toShort() }
