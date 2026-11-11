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

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeexDspTest {
    @Test
    fun versionAndResampler() {
        assertEquals("1.2.1", SpeexDsp.libraryVersion)

        SpeexResampler.create(channels = 1, inRate = 16_000, outRate = 8_000).use { resampler ->
            assertEquals(1, resampler.channels)
            assertEquals(16_000, resampler.inputRate)
            assertEquals(8_000, resampler.outputRate)
            assertEquals(2, resampler.ratioNum)
            assertEquals(1, resampler.ratioDen)
            assertTrue(resampler.inputLatency >= 0)
            assertTrue(resampler.outputLatency >= 0)

            var produced = 0
            var energy = 0L
            for (frame in 0 until 20) {
                val output = resampler.resample(sine(320, 16_000, frame))
                produced += output.size
                energy += energyOf(output)
            }
            assertTrue(produced > 0, "resampler produced no output")
            assertTrue(energy > 0, "resampler produced only silence")

            // Zero-length input flushes the filter state through the same path.
            resampler.resample(ShortArray(0))
        }
    }

    @Test
    fun resamplerFractionalRateAndControls() {
        SpeexResampler.createFractional(
            channels = 1,
            ratioNum = 3,
            ratioDen = 2,
            inRate = 48_000,
            outRate = 32_000,
            quality = SpeexResampler.QUALITY_VOIP,
        ).use { resampler ->
            assertEquals(3, resampler.ratioNum)
            assertEquals(2, resampler.ratioDen)
            assertEquals(SpeexResampler.QUALITY_VOIP, resampler.quality)

            resampler.quality = SpeexResampler.QUALITY_DEFAULT
            assertEquals(SpeexResampler.QUALITY_DEFAULT, resampler.quality)

            resampler.inputStride = 1
            resampler.outputStride = 1
            assertEquals(1, resampler.inputStride)
            assertEquals(1, resampler.outputStride)

            resampler.setRate(48_000, 24_000)
            assertEquals(48_000, resampler.inputRate)
            assertEquals(24_000, resampler.outputRate)

            resampler.skipZeros()
            resampler.reset()
        }
    }

    @Test
    fun preprocessorRunsAndExposesSpectra() {
        SpeexPreprocessor.create(frameSize = 160, samplingRate = 8_000).use { preprocessor ->
            preprocessor.denoise = true
            preprocessor.agc = true
            preprocessor.vad = true
            assertTrue(preprocessor.denoise)
            assertTrue(preprocessor.agc)
            assertTrue(preprocessor.vad)

            preprocessor.agcLevel = 8_000f
            assertEquals(8_000f, preprocessor.agcLevel, 1f)
            preprocessor.noiseSuppress = -15
            assertEquals(-15, preprocessor.noiseSuppress)
            preprocessor.probStart = 50
            assertEquals(50, preprocessor.probStart)
            preprocessor.probContinue = 60
            assertEquals(60, preprocessor.probContinue)

            val frame = sine(160, 8_000, 0)
            preprocessor.estimateUpdate(frame)
            preprocessor.process(frame)

            assertTrue(preprocessor.psdSize > 0)
            assertEquals(preprocessor.psdSize, preprocessor.powerSpectrum().size)
            assertTrue(preprocessor.noisePsdSize > 0)
            assertEquals(preprocessor.noisePsdSize, preprocessor.noisePowerSpectrum().size)
            assertTrue(preprocessor.speechProbability in 0..100)
        }
    }

    @Test
    fun echoCancellerAndDecorrelator() {
        SpeexEchoCanceller.create(frameSize = 160, filterLength = 800).use { canceller ->
            canceller.samplingRate = 8_000
            assertEquals(8_000, canceller.samplingRate)

            val play = sine(160, 8_000, 0)
            val microphone = play.copyOf()
            val clean = canceller.cancellation(microphone, play)
            assertEquals(160, clean.size)

            assertTrue(canceller.impulseResponseSize > 0)
            assertEquals(canceller.impulseResponseSize, canceller.impulseResponse().size)

            val captured = canceller.capture(microphone)
            assertEquals(160, captured.size)
            canceller.playback(play)
            canceller.reset()
        }

        SpeexDecorrelator.create(rate = 8_000, channels = 2, frameSize = 160).use { decorrelator ->
            assertEquals(2, decorrelator.channels)
            assertEquals(160, decorrelator.frameSize)
            val input = ShortArray(320) { index -> sine(320, 8_000, 0)[index] }
            val output = decorrelator.process(input, strength = 100)
            assertEquals(320, output.size)
            assertTrue(energyOf(output) > 0, "decorrelator produced only silence")
        }
    }

    @Test
    fun jitterBufferRoundTrip() {
        SpeexJitterBuffer.create(stepSize = 160).use { jitter ->
            val payloads = (0 until 4).map { "packet-$it".encodeToByteArray() }
            payloads.forEachIndexed { index, data ->
                jitter.put(
                    data = data,
                    timestamp = index * 160,
                    span = 160,
                    sequence = index,
                    userData = index,
                )
            }
            assertEquals(4, jitter.available)

            val first = jitter.get(desiredSpan = 160)
            assertEquals(SpeexJitterStatus.OK, first.status)
            assertContentEquals(payloads[0], first.packet.data)
            assertEquals(0, first.packet.timestamp)
            assertEquals(160, first.packet.span)
            assertEquals(0, first.packet.sequence)
            assertEquals(0, first.packet.userData)

            jitter.tick()
            val second = jitter.get(desiredSpan = 160)
            assertEquals(SpeexJitterStatus.OK, second.status)
            assertContentEquals(payloads[1], second.packet.data)
            assertEquals(160, second.packet.timestamp)

            jitter.remainingSpan(320)
            assertTrue(jitter.pointerTimestamp >= 0)
            jitter.reset()
        }
    }

    @Test
    fun ringBufferRoundTrip() {
        SpeexRingBuffer.create(16).use { buffer ->
            assertEquals(0, buffer.write(ByteArray(0)))
            assertEquals(0, buffer.read(0).size)

            val data = ByteArray(8) { it.toByte() }
            assertEquals(8, buffer.write(data))
            assertEquals(8, buffer.available)
            assertContentEquals(data, buffer.read(8))
            assertEquals(0, buffer.available)

            assertEquals(3, buffer.writeZeros(3))
            assertContentEquals(ByteArray(3), buffer.read(3))

            // Shrinking used to leave the ring pointers outside the new
            // allocation; writing then corrupted the native heap.
            assertEquals(4, buffer.resize(4))
            assertEquals(2, buffer.write(byteArrayOf(9, 10)))
            assertContentEquals(byteArrayOf(9, 10), buffer.read(2))
        }
    }

    private fun sine(frameSize: Int, sampleRate: Int, frameIndex: Int): ShortArray {
        val start = frameIndex.toLong() * frameSize
        return ShortArray(frameSize) { index ->
            val phase = 2.0 * PI * 440.0 * (start + index) / sampleRate
            (sin(phase) * 12_000).toInt().toShort()
        }
    }

    private fun energyOf(samples: ShortArray): Long =
        samples.sumOf { value -> value.toLong() * value.toLong() }
}
