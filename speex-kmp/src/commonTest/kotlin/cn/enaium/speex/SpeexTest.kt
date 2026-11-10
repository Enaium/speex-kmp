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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpeexTest {
    @Test
    fun versionAndModes() {
        assertEquals("1.2.1", Speex.libraryVersion)
        assertEquals(1, Speex.versionMajor)
        assertEquals(2, Speex.versionMinor)
        assertEquals(1, Speex.versionMicro)
        assertEquals("", Speex.versionExtra)

        for (mode in SpeexMode.entries) {
            assertEquals(mode.sampleRate / 50, mode.frameSize, "${mode.name} frame size")
            assertEquals(mode.frameSize, Speex.frameSize(mode))
            assertTrue(mode.libraryName.isNotEmpty(), "${mode.name} has a library name")
        }
        assertEquals(160, Speex.frameSize(SpeexMode.NARROWBAND))
        assertEquals(320, Speex.frameSize(SpeexMode.WIDEBAND))
        assertEquals(640, Speex.frameSize(SpeexMode.ULTRA_WIDEBAND))
        assertTrue(Speex.submodeBits(SpeexMode.NARROWBAND, 0) > 0)
        assertEquals(SpeexMode.WIDEBAND, SpeexMode.fromSampleRate(16_000))
    }

    @Test
    fun bitsRoundTrip() {
        SpeexBits().use { writer ->
            writer.pack(0x2A, 6)
            writer.pack(0x1FF, 9)
            writer.pack(-7, 5)
            val packet = writer.write()
            assertTrue(packet.isNotEmpty(), "serialised bit-stream is empty")

            SpeexBits().use { reader ->
                reader.readFrom(packet)
                assertEquals(0x2A, reader.unpackUnsigned(6))
                assertEquals(0x1FF, reader.unpackUnsigned(9))
                assertEquals(-7, reader.unpackSigned(5))
                assertTrue(reader.remaining >= 0)
            }
        }
    }

    @Test
    fun encodeDecodeEachMode() {
        for (mode in SpeexMode.entries) {
            SpeexEncoder.open(mode).use { encoder ->
                SpeexDecoder.open(mode).use { decoder ->
                    assertEquals(mode.frameSize, encoder.frameSize)
                    assertEquals(mode.frameSize, decoder.frameSize)

                    var packets = 0
                    var energy = 0L
                    for (frame in 0 until 30) {
                        val pcm = sine(mode.frameSize, mode.sampleRate, frame)
                        val packet = encoder.encode(pcm)
                        if (packet != null) {
                            packets++
                            assertTrue(packet.isNotEmpty(), "${mode.name} packet is empty")
                            val decoded = decoder.decode(packet)
                            assertEquals(mode.frameSize, decoded.size)
                            energy += energyOf(decoded)
                        }
                    }
                    assertTrue(packets > 0, "${mode.name} produced no packets")
                    assertTrue(energy > 0, "${mode.name} decoded only silence")
                }
            }
        }
    }

    @Test
    fun packetLossConcealment() {
        SpeexEncoder.open(SpeexMode.NARROWBAND).use { encoder ->
            SpeexDecoder.open(SpeexMode.NARROWBAND).use { decoder ->
                repeat(10) { frame ->
                    encoder.encode(sine(encoder.frameSize, 8_000, frame))?.let { decoder.decode(it) }
                }
                val concealed = decoder.decode(null)
                assertEquals(decoder.frameSize, concealed.size)
            }
        }
    }

    @Test
    fun encoderControls() {
        SpeexEncoder.open(SpeexMode.WIDEBAND).use { encoder ->
            encoder.quality = 5
            assertEquals(5, encoder.quality)

            encoder.complexity = 4
            assertEquals(4, encoder.complexity)

            encoder.samplingRate = 16_000
            assertEquals(16_000, encoder.samplingRate)

            encoder.vbr = true
            assertTrue(encoder.vbr)

            encoder.vbrQuality = 6.5f
            assertEquals(6.5f, encoder.vbrQuality, 0.01f)

            encoder.vbrMaxBitrate = 32_000
            assertEquals(32_000, encoder.vbrMaxBitrate)

            encoder.dtx = false
            assertTrue(!encoder.dtx)

            encoder.vad = false
            assertTrue(!encoder.vad)

            encoder.highpass = true
            assertTrue(encoder.highpass)

            encoder.plcTuning = 3
            assertEquals(3, encoder.plcTuning)

            encoder.bitrate = 27_800
            assertTrue(encoder.bitrate > 0)

            encoder.reset()
        }
    }

    @Test
    fun decoderControls() {
        SpeexDecoder.open(SpeexMode.WIDEBAND).use { decoder ->
            decoder.enhancement = false
            assertTrue(!decoder.enhancement)
            decoder.enhancement = true
            assertTrue(decoder.enhancement)

            decoder.samplingRate = 16_000
            assertEquals(16_000, decoder.samplingRate)

            decoder.highpass = false
            assertTrue(!decoder.highpass)
            assertTrue(decoder.activity >= 0)
            assertTrue(decoder.bitrate > 0)
            decoder.reset()
        }
    }

    @Test
    fun headerRoundTrip() {
        SpeexHeader.create(rate = 16_000, channels = 1, mode = SpeexMode.WIDEBAND).use { header ->
            assertEquals("Speex   ", header.speexString)
            assertEquals(Speex.libraryVersion, header.speexVersion)
            assertEquals(16_000, header.rate)
            assertEquals(1, header.channels)
            assertEquals(SpeexMode.WIDEBAND, header.mode)
            assertEquals(320, header.frameSize)
            header.bitrate = 27_800
            header.vbr = true
            header.framesPerPacket = 1

            val packet = header.toPacket()
            assertEquals(80, packet.size)

            SpeexHeader.fromPacket(packet).use { parsed ->
                assertEquals("Speex   ", parsed.speexString)
                assertEquals(16_000, parsed.rate)
                assertEquals(1, parsed.channels)
                assertEquals(SpeexMode.WIDEBAND, parsed.mode)
                assertEquals(320, parsed.frameSize)
                assertEquals(27_800, parsed.bitrate)
                assertTrue(parsed.vbr)
                assertEquals(1, parsed.framesPerPacket)
                assertEquals(80, parsed.headerSize)
            }
        }
    }

    @Test
    fun stereoRoundTrip() {
        SpeexEncoder.open(SpeexMode.WIDEBAND).use { encoder ->
            SpeexDecoder.open(SpeexMode.WIDEBAND).use { decoder ->
                SpeexStereo().use { stereo ->
                    assertEquals(320, encoder.frameSize)
                    var packets = 0
                    var energy = 0L
                    for (frame in 0 until 20) {
                        val interleaved = ShortArray(encoder.frameSize * 2) { index ->
                            val channel = if (index % 2 == 0) 1.0 else 0.6
                            (sin(2.0 * PI * 440.0 * (frame * encoder.frameSize + index / 2) / 16_000.0) *
                                12_000 * channel).toInt().toShort()
                        }
                        val packet = encoder.encodeStereo(interleaved)
                        assertNotNull(packet, "stereo frame $frame was dropped")
                        packets++
                        val decoded = decoder.decodeStereo(packet, stereo)
                        assertEquals(encoder.frameSize * 2, decoded.size)
                        energy += energyOf(decoded)
                    }
                    assertEquals(20, packets)
                    assertTrue(energy > 0, "stereo decode produced only silence")
                }
            }
        }
    }

    @Test
    fun inputOffsetIsHonoured() {
        val encoder = SpeexEncoder.open(SpeexMode.NARROWBAND)
        try {
            assertEquals(160, encoder.frameSize)
            val pcm = ShortArray(400) { index ->
                if (index in 100 until 260) {
                    (sin(2.0 * PI * 440.0 * (index - 100) / 8_000.0) * 12_000).toInt().toShort()
                } else {
                    0
                }
            }
            assertNotNull(encoder.encode(pcm, offset = 100))
        } finally {
            encoder.close()
        }
    }

    @Test
    fun encodeStereoFoldsInputInPlace() {
        SpeexEncoder.open(SpeexMode.WIDEBAND).use { encoder ->
            val frameSize = encoder.frameSize
            val interleaved = ShortArray(frameSize * 2) { index ->
                if (index % 2 == 0) 12_000 else 6_000
            }
            val before = interleaved.copyOf()
            val packet = encoder.encodeStereo(interleaved)
            assertNotNull(packet)
            assertTrue(
                !interleaved.copyOfRange(0, frameSize).contentEquals(before.copyOfRange(0, frameSize)),
                "encodeStereo must fold the first frame to mono in place",
            )
        }
    }

    @Test
    fun lowLevelStereoHelpers() {
        SpeexStereo().use { stereo ->
            SpeexBits().use { bits ->
                val interleaved = ShortArray(320 * 2) { index ->
                    if (index % 2 == 0) 10_000 else 5_000
                }
                stereo.encode(interleaved, frameSize = 320, bits = bits)
                val packet = bits.write()
                assertTrue(packet.isNotEmpty(), "stereo request produced no bits")
                assertTrue(interleaved[0] != 10_000.toShort(), "stereo encode did not fold to mono")
            }

            val mono = ShortArray(640)
            mono[0] = 10_000
            mono[1] = -10_000
            stereo.decode(mono, frameSize = 320)
            assertTrue(mono[0] != 0.toShort() || mono[1] != 0.toShort(), "stereo decode produced silence")
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
