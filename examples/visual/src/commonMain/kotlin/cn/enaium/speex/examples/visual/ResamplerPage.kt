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
import cn.enaium.speex.SpeexResampler

/**
 * SpeexDSP resampler: the same source resampled from [inputRate] to
 * [outputRate]. Both waveforms share a millisecond axis, so the change in
 * sample density is visible while the signal stays recognisable. The source is
 * either synthetic speech or the live microphone.
 */
internal class ResamplerPage(audio: LiveAudio) : FeaturePage("Resampler") {

    private val rates = intArrayOf(8_000, 16_000, 32_000, 48_000)
    private var inputRateIndex = 1
    private var outputRateIndex = 0
    private var quality = SpeexResampler.QUALITY_DEFAULT

    private val inputRateChoice = intArrayOf(inputRateIndex)
    private val outputRateChoice = intArrayOf(outputRateIndex)
    private val qualityValue = intArrayOf(quality)

    private val microphone = MicrophoneSource(audio)
    private val sourceControls = SourceControls("resampler", microphone)

    private var resampler: SpeexResampler? = null
    private var source = SyntheticSpeech(16_000, 220.0, 0xCAFE)
    private var inputScope = SignalScope(32_000)
    private var outputScope = SignalScope(32_000)
    private var inputPlot = FloatArray(16_000)
    private var outputPlot = FloatArray(8_000)
    private val levelHistory = HistoryRing(300)
    private var levelPlot = FloatArray(300)

    private val inputSeries = PlotSeries("input", Colors.INPUT)
    private val outputSeries = PlotSeries("output", Colors.OUTPUT)
    private val levelSeries = PlotSeries("output level", Colors.LEVEL)

    private var inputSamples = 0L
    private var outputSamples = 0L
    private var shown = false

    init {
        recreate()
    }

    private val inputRate: Int get() = rates[inputRateIndex]
    private val outputRate: Int get() = rates[outputRateIndex]
    private val frameSize: Int get() = maxOf(inputRate / 100, 1)

    override fun pump() {
        if (resampler == null) return
        if (sourceControls.selected == PageSource.MICROPHONE) {
            microphone.pump { pcm -> process(pcm) }
            return
        }
        val input = FloatArray(frameSize)
        source.next(input, frameSize, 0.25f, noise = 0.002f)
        process(input.toPcm(frameSize))
    }

    private fun process(inputPcm: ShortArray) {
        val resampler = resampler ?: return
        inputScope.appendPcm(inputPcm)
        inputSamples += inputPcm.size

        val output = resampler.resample(inputPcm)
        outputScope.appendPcm(output)
        outputSamples += output.size
        levelHistory.append(dbfs(rmsPcm(output)))
    }

    override fun draw() {
        Ui.beginPage()

        sourceControls.draw { source ->
            if (source == PageSource.MICROPHONE) {
                microphone.configure(inputRate, frameSize)
                if (shown) microphone.start()
            } else {
                microphone.stop()
            }
        }

        inputRateChoice[0] = inputRateIndex
        Ui.combo("input rate", inputRateChoice, RATE_LABELS, itemWidth = 130f) {
            inputRateIndex = inputRateChoice[0]
            recreate()
        }

        outputRateChoice[0] = outputRateIndex
        Ui.combo("output rate", outputRateChoice, RATE_LABELS, itemWidth = 130f) {
            outputRateIndex = outputRateChoice[0]
            recreate()
        }

        qualityValue[0] = quality
        Ui.sliderInt("quality", qualityValue, SpeexResampler.QUALITY_MIN, SpeexResampler.QUALITY_MAX, "%d", itemWidth = 120f) {
            quality = qualityValue[0]
            resampler?.quality = quality
        }

        Ui.button("reset###resampler-reset") { reset() }
        sourceControls.drawStatus()

        val resampler = resampler
        ImGui.text(
            "$inputRate Hz -> $outputRate Hz, ratio ${resampler?.ratioNum ?: 0}/${resampler?.ratioDen ?: 0}, " +
                "quality $quality",
        )
        ImGui.text(
            "in=$inputSamples samples, out=$outputSamples samples, " +
                "latency ${resampler?.inputLatency ?: 0} in / ${resampler?.outputLatency ?: 0} out",
        )

        inputScope.snapshot(inputPlot)
        outputScope.snapshot(outputPlot)
        inputSeries.values = inputPlot
        outputSeries.values = outputPlot
        inputSeries.xScale = 1000.0 / inputRate
        outputSeries.xScale = 1000.0 / outputRate

        val levelCount = levelHistory.copyTo(levelPlot)
        levelSeries.values = levelPlot
        levelSeries.length = levelCount
        levelSeries.xScale = 1000.0 / 50.0

        val height = Ui.plotHeight(2)
        Chart.plot(
            "waveform",
            listOf(inputSeries, outputSeries),
            height,
            PLOT_WINDOW_MS,
            -0.35,
            0.35,
        )
        Chart.plot(
            "output level",
            listOf(levelSeries),
            height,
            LEVEL_WINDOW_MS,
            -90.0,
            0.0,
            yLabel = "dBFS",
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
        microphone.configure(inputRate, frameSize)
        microphone.start()
    }

    override fun replayMicrophone() {
        sourceControls.replay()
    }

    override fun close() {
        microphone.close()
        resampler?.close()
    }

    override fun summary(): String {
        return "resampler: $inputRate -> $outputRate Hz, quality=$quality, " +
            "source=${sourceControls.selected.label}, " +
            "in=$inputSamples samples, out=$outputSamples samples"
    }

    private fun recreate() {
        resampler?.close()
        resampler = SpeexResampler.create(
            channels = 1,
            inRate = inputRate,
            outRate = outputRate,
            quality = quality,
        )
        source = SyntheticSpeech(inputRate, 220.0, 0xCAFE)
        inputScope = SignalScope(inputRate * 2)
        outputScope = SignalScope(outputRate * 2)
        inputPlot = FloatArray(inputRate)
        outputPlot = FloatArray(outputRate)
        levelHistory.clear()
        inputSamples = 0
        outputSamples = 0

        microphone.configure(inputRate, frameSize)
        if (shown && sourceControls.selected == PageSource.MICROPHONE) microphone.start()
    }

    private companion object {
        const val PLOT_WINDOW_MS = 1000.0
        const val LEVEL_WINDOW_MS = 300 * 20.0
        val RATE_LABELS = arrayOf("8000", "16000", "32000", "48000")
    }
}
