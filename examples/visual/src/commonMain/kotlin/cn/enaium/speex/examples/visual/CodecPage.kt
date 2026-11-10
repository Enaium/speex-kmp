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

import cn.enaium.imgui.ImGui
import cn.enaium.speex.SpeexDecoder
import cn.enaium.speex.SpeexEncoder
import cn.enaium.speex.SpeexMode
import cn.enaium.speex.SpeexStereo

/**
 * Speex codec: the input and the decoded output overlaid, plus the packet size
 * history. With DTX on, dropped frames show as zero-sized packets and a flat
 * output. The source is either synthetic speech or the live microphone.
 */
internal class CodecPage(audio: LiveAudio) : FeaturePage("Codec") {

    private val modes = SpeexMode.entries
    private var modeIndex = 1
    private var quality = 8
    private var vbr = false
    private var dtx = false
    private var stereo = false

    private val modeChoice = intArrayOf(modeIndex)
    private val qualityValue = intArrayOf(quality)
    private val vbrValue = booleanArrayOf(vbr)
    private val dtxValue = booleanArrayOf(dtx)
    private val stereoValue = booleanArrayOf(stereo)

    private val microphone = MicrophoneSource(audio)
    private val sourceControls = SourceControls("codec", microphone)

    private var encoder: SpeexEncoder? = null
    private var decoder: SpeexDecoder? = null
    private var stereoState: SpeexStereo? = null
    private var source = SyntheticSpeech(16_000, 120.0, 0x1234)
    private var rightSource = SyntheticSpeech(16_000, 150.0, 0x5678)

    private var inputScope = SignalScope(32_000)
    private var outputScope = SignalScope(32_000)
    private var inputPlot = FloatArray(16_000)
    private var outputPlot = FloatArray(16_000)
    private val packetHistory = HistoryRing(240)
    private var packetPlot = FloatArray(240)

    private val inputSeries = PlotSeries("input", Colors.INPUT)
    private val outputSeries = PlotSeries("decoded", Colors.OUTPUT)
    private val packetSeries = PlotSeries("packet bytes", Colors.OUTPUT)

    private var frames = 0L
    private var packets = 0L
    private var dtxDrops = 0L
    private var totalPacketBytes = 0L
    private var lastPacketBytes = 0
    private var shown = false

    init {
        recreate()
    }

    override fun pump() {
        if (sourceControls.selected == PageSource.MICROPHONE) {
            microphone.pump { pcm -> processMono(pcm) }
            return
        }

        val frameSize = encoder?.frameSize ?: return
        if (stereo) {
            val left = FloatArray(frameSize)
            val right = FloatArray(frameSize)
            source.next(left, frameSize, 0.30f, noise = 0.003f)
            rightSource.next(right, frameSize, 0.18f, noise = 0.003f)
            val interleaved = ShortArray(frameSize * 2)
            for (i in 0 until frameSize) {
                interleaved[i * 2] = left[i].toPcm()
                interleaved[i * 2 + 1] = right[i].toPcm()
            }
            processStereo(interleaved)
        } else {
            processMono(source.nextPcm(frameSize, 0.30f, noise = 0.003f))
        }
    }

    private fun processMono(pcm: ShortArray) {
        val encoder = encoder ?: return
        val decoder = decoder ?: return
        val frameSize = encoder.frameSize
        if (pcm.size != frameSize) return

        if (stereo) {
            val interleaved = ShortArray(frameSize * 2)
            for (i in 0 until frameSize) {
                interleaved[i * 2] = pcm[i]
                interleaved[i * 2 + 1] = pcm[i]
            }
            processStereo(interleaved)
            return
        }

        inputScope.appendPcm(pcm)
        val packet = encoder.encode(pcm)
        if (packet != null) {
            outputScope.appendPcm(decoder.decode(packet))
        } else {
            outputScope.append(FloatArray(frameSize), frameSize)
        }
        account(packet)
    }

    private fun processStereo(interleaved: ShortArray) {
        val encoder = encoder ?: return
        val decoder = decoder ?: return
        val stereoState = stereoState ?: return
        val frameSize = encoder.frameSize
        if (interleaved.size != frameSize * 2) return

        inputScope.appendChannel(interleaved, frameSize, channel = 0, channels = 2)
        val packet = encoder.encodeStereo(interleaved)
        if (packet != null) {
            val decoded = decoder.decodeStereo(packet, stereoState)
            outputScope.appendChannel(decoded, frameSize, channel = 0, channels = 2)
        } else {
            outputScope.append(FloatArray(frameSize), frameSize)
        }
        account(packet)
    }

    private fun account(packet: ByteArray?) {
        frames++
        if (packet != null) {
            packets++
            totalPacketBytes += packet.size
            lastPacketBytes = packet.size
            packetHistory.append(packet.size.toFloat())
        } else {
            dtxDrops++
            lastPacketBytes = 0
            packetHistory.append(0f)
        }
    }

    override fun draw() {
        Ui.beginPage()

        sourceControls.draw { source ->
            if (source == PageSource.MICROPHONE) {
                microphone.configure(modes[modeIndex].sampleRate, modes[modeIndex].frameSize)
                if (shown) microphone.start()
            } else {
                microphone.stop()
            }
        }

        modeChoice[0] = modeIndex
        Ui.combo("mode", modeChoice, MODE_LABELS, itemWidth = 150f) {
            modeIndex = modeChoice[0]
            recreate()
        }

        qualityValue[0] = quality
        Ui.sliderInt("quality", qualityValue, 0, 10, "%d", itemWidth = 120f) {
            quality = qualityValue[0]
            encoder?.quality = quality
        }

        vbrValue[0] = vbr
        Ui.checkbox("VBR", vbrValue) {
            vbr = vbrValue[0]
            encoder?.vbr = vbr
        }

        dtxValue[0] = dtx
        Ui.checkbox("DTX", dtxValue) {
            dtx = dtxValue[0]
            encoder?.dtx = dtx
        }

        stereoValue[0] = stereo
        Ui.checkbox("stereo", stereoValue) {
            stereo = stereoValue[0]
            recreate()
        }

        Ui.button("reset###codec-reset") { reset() }
        sourceControls.drawStatus()

        val sampleRate = modes[modeIndex].sampleRate
        val averageBitrate = if (frames > 0) {
            totalPacketBytes * 8.0 / (frames * (modes[modeIndex].frameSize.toDouble() / sampleRate))
        } else {
            0.0
        }
        ImGui.text(
            "${modes[modeIndex].name.lowercase()}: ${modes[modeIndex].frameSize} samples/frame, " +
                "frames=$frames, packets=$packets, DTX drops=$dtxDrops",
        )
        ImGui.text(
            "last packet $lastPacketBytes bytes | average ${fixed(averageBitrate / 1000.0, 1)} kbit/s" +
                if (stereo) " | stereo (left channel plotted)" else "",
        )

        val inputPeak = inputScope.snapshot(inputPlot)
        val outputPeak = outputScope.snapshot(outputPlot)
        val scale = maxOf(inputPeak, outputPeak, 0.02f) * 1.15f

        inputSeries.values = inputPlot
        outputSeries.values = outputPlot
        inputSeries.xScale = 1000.0 / sampleRate
        outputSeries.xScale = 1000.0 / sampleRate

        val packetCount = packetHistory.copyTo(packetPlot)
        packetSeries.values = packetPlot
        packetSeries.length = packetCount
        packetSeries.xScale = 1000.0 / 50.0

        val height = Ui.plotHeight(2)
        Chart.plot(
            "codec waveform",
            listOf(inputSeries, outputSeries),
            height,
            PLOT_WINDOW_MS,
            -scale.toDouble(),
            scale.toDouble(),
        )

        var maxPacket = 16f
        for (i in 0 until packetCount) if (packetPlot[i] > maxPacket) maxPacket = packetPlot[i]
        Chart.plot(
            "packet size",
            listOf(packetSeries),
            height,
            PACKET_WINDOW_MS,
            0.0,
            (maxPacket * 1.2),
            yLabel = "bytes",
        )
    }

    override fun reset() {
        microphone.reset()
        recreate()
    }

    override fun onShown() {
        shown = true
        if (sourceControls.selected == PageSource.MICROPHONE) microphone.start()
    }

    override fun onHidden() {
        shown = false
        microphone.stop()
    }

    override fun startMicrophone() {
        sourceControls.record()
        microphone.configure(modes[modeIndex].sampleRate, modes[modeIndex].frameSize)
        microphone.start()
    }

    override fun replayMicrophone() {
        sourceControls.replay()
    }

    override fun close() {
        microphone.close()
        encoder?.close()
        decoder?.close()
        stereoState?.close()
    }

    override fun summary(): String {
        val mode = modes[modeIndex]
        return "codec: ${mode.name.lowercase()}, source=${sourceControls.selected.label}, " +
            "frames=$frames, packets=$packets, DTX drops=$dtxDrops, last=$lastPacketBytes bytes, stereo=$stereo"
    }

    private fun recreate() {
        encoder?.close()
        decoder?.close()
        stereoState?.close()

        val mode = modes[modeIndex]
        val effectiveQuality = quality
        val effectiveVbr = vbr
        val effectiveDtx = dtx
        encoder = SpeexEncoder.open(mode).apply {
            quality = effectiveQuality
            vbr = effectiveVbr
            dtx = effectiveDtx
        }
        decoder = SpeexDecoder.open(mode)
        stereoState = if (stereo) SpeexStereo() else null

        source = SyntheticSpeech(mode.sampleRate, 120.0, 0x1234)
        rightSource = SyntheticSpeech(mode.sampleRate, 150.0, 0x5678)
        inputScope = SignalScope(mode.sampleRate * 2)
        outputScope = SignalScope(mode.sampleRate * 2)
        inputPlot = FloatArray(mode.sampleRate)
        outputPlot = FloatArray(mode.sampleRate)
        packetHistory.clear()
        frames = 0
        packets = 0
        dtxDrops = 0
        totalPacketBytes = 0
        lastPacketBytes = 0

        microphone.configure(mode.sampleRate, mode.frameSize)
        if (shown && sourceControls.selected == PageSource.MICROPHONE) microphone.start()
    }

    private companion object {
        const val PLOT_WINDOW_MS = 1000.0
        const val PACKET_WINDOW_MS = 240 * 20.0
        val MODE_LABELS = SpeexMode.entries.map { it.name.lowercase() }.toTypedArray()
    }
}
