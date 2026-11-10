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
import cn.enaium.speex.SpeexDecorrelator
import kotlin.math.sqrt

/**
 * SpeexDSP channel decorrelator: two almost identical channels before and
 * after decorrelation, plus the measured inter-channel correlation. The
 * decorrelator alters the phase and adds inaudible noise to remove the
 * correlation that makes multi-channel echo cancellation ill-conditioned.
 *
 * The microphone is mono, so it becomes the left channel and a 0.98 gain copy
 * becomes the right channel before decorrelation.
 */
internal class DecorrelatorPage(audio: LiveAudio) : FeaturePage("Decorrelate") {

    private val sampleRate = 16_000
    private val frameSize = 320
    private val channels = 2

    private var strength = 100
    private val strengthValue = intArrayOf(strength)

    private val microphone = MicrophoneSource(audio)
    private val sourceControls = SourceControls("decorrelate", microphone)

    private var decorrelator = SpeexDecorrelator.create(sampleRate, channels, frameSize)
    private var source = SyntheticSpeech(sampleRate, 140.0, 0x4444)

    private var inputLeftScope = SignalScope(sampleRate * 2)
    private var inputRightScope = SignalScope(sampleRate * 2)
    private var outputLeftScope = SignalScope(sampleRate * 2)
    private var outputRightScope = SignalScope(sampleRate * 2)
    private var inputDifferenceScope = SignalScope(sampleRate * 2)
    private var outputDifferenceScope = SignalScope(sampleRate * 2)
    private var inputLeftPlot = FloatArray(sampleRate)
    private var inputRightPlot = FloatArray(sampleRate)
    private var outputLeftPlot = FloatArray(sampleRate)
    private var outputRightPlot = FloatArray(sampleRate)
    private var inputDifferencePlot = FloatArray(sampleRate)
    private var outputDifferencePlot = FloatArray(sampleRate)

    private val inputLeftSeries = PlotSeries("input L", Colors.INPUT)
    private val inputRightSeries = PlotSeries("input R", Colors.REFERENCE)
    private val outputLeftSeries = PlotSeries("output L", Colors.OUTPUT)
    private val outputRightSeries = PlotSeries("output R", Colors.GAIN)
    private val inputDifferenceSeries = PlotSeries("input L-R", Colors.INPUT)
    private val outputDifferenceSeries = PlotSeries("output L-R", Colors.OUTPUT)

    private var beforeCorrelation = 1f
    private var afterCorrelation = 1f
    private var frames = 0L
    private var shown = false

    init {
        microphone.configure(sampleRate, frameSize)
    }

    override fun pump() {
        if (sourceControls.selected == PageSource.MICROPHONE) {
            microphone.pump { pcm -> processMicrophone(pcm) }
            return
        }

        val mono = FloatArray(frameSize)
        source.next(mono, frameSize, 0.22f, noise = 0.002f)
        val interleaved = ShortArray(frameSize * channels)
        for (i in 0 until frameSize) {
            // Two near-identical channels: exactly the case decorrelation exists
            // for. The tiny gain difference keeps the correlation below 1.
            interleaved[i * 2] = mono[i].toPcm()
            interleaved[i * 2 + 1] = (mono[i] * 0.98f).toPcm()
        }
        process(interleaved)
    }

    private fun processMicrophone(pcm: ShortArray) {
        val interleaved = ShortArray(frameSize * channels)
        for (i in 0 until frameSize) {
            interleaved[i * 2] = pcm[i]
            interleaved[i * 2 + 1] = (pcm[i] * 0.98f).toInt().coerceIn(-32768, 32767).toShort()
        }
        process(interleaved)
    }

    private fun process(interleaved: ShortArray) {
        beforeCorrelation = correlation(interleaved, frameSize)
        inputLeftScope.appendChannel(interleaved, frameSize, 0, channels)
        inputRightScope.appendChannel(interleaved, frameSize, 1, channels)

        val output = decorrelator.process(interleaved, strength)
        afterCorrelation = correlation(output, frameSize)
        outputLeftScope.appendChannel(output, frameSize, 0, channels)
        outputRightScope.appendChannel(output, frameSize, 1, channels)

        val inputDifference = FloatArray(frameSize)
        val outputDifference = FloatArray(frameSize)
        for (i in 0 until frameSize) {
            inputDifference[i] = (interleaved[i * 2].toInt() - interleaved[i * 2 + 1]) / 32768f
            outputDifference[i] = (output[i * 2].toInt() - output[i * 2 + 1]) / 32768f
        }
        inputDifferenceScope.append(inputDifference)
        outputDifferenceScope.append(outputDifference)
        frames++
    }

    override fun draw() {
        Ui.beginPage()

        sourceControls.draw { source ->
            if (source == PageSource.MICROPHONE) {
                microphone.configure(sampleRate, frameSize)
                if (shown) microphone.start()
            } else {
                microphone.stop()
            }
        }

        strengthValue[0] = strength
        Ui.sliderInt("strength", strengthValue, 0, 100, "%d", itemWidth = 160f) {
            strength = strengthValue[0]
        }

        Ui.button("reset###decorrelate-reset") { reset() }
        sourceControls.drawStatus()

        ImGui.text(
            "frame $frameSize samples x $channels channels @ $sampleRate Hz, frames=$frames | " +
                "correlation ${fixed(beforeCorrelation.toDouble(), 3)} -> ${fixed(afterCorrelation.toDouble(), 3)}",
        )

        inputLeftScope.snapshot(inputLeftPlot)
        inputRightScope.snapshot(inputRightPlot)
        outputLeftScope.snapshot(outputLeftPlot)
        outputRightScope.snapshot(outputRightPlot)
        inputDifferenceScope.snapshot(inputDifferencePlot)
        outputDifferenceScope.snapshot(outputDifferencePlot)
        val xScale = 1000.0 / sampleRate
        inputLeftSeries.values = inputLeftPlot
        inputRightSeries.values = inputRightPlot
        outputLeftSeries.values = outputLeftPlot
        outputRightSeries.values = outputRightPlot
        inputDifferenceSeries.values = inputDifferencePlot
        outputDifferenceSeries.values = outputDifferencePlot
        inputLeftSeries.xScale = xScale
        inputRightSeries.xScale = xScale
        outputLeftSeries.xScale = xScale
        outputRightSeries.xScale = xScale
        inputDifferenceSeries.xScale = xScale
        outputDifferenceSeries.xScale = xScale
        val peak = maxOf(
            peakOf(inputLeftPlot),
            peakOf(inputRightPlot),
            peakOf(outputLeftPlot),
            peakOf(outputRightPlot),
            0.02f,
        ) * 1.15f
        val differencePeak = maxOf(
            peakOf(inputDifferencePlot),
            peakOf(outputDifferencePlot),
            0.005f,
        ) * 1.15f

        val height = Ui.plotHeight(2)
        Chart.plot(
            "channels",
            listOf(inputLeftSeries, inputRightSeries, outputLeftSeries, outputRightSeries),
            height,
            PLOT_WINDOW_MS,
            -peak.toDouble(),
            peak.toDouble(),
        )
        Chart.plot(
            "inter-channel difference",
            listOf(inputDifferenceSeries, outputDifferenceSeries),
            height,
            PLOT_WINDOW_MS,
            -differencePeak.toDouble(),
            differencePeak.toDouble(),
        )
    }

    override fun reset() {
        microphone.reset()
        decorrelator.close()
        decorrelator = SpeexDecorrelator.create(sampleRate, channels, frameSize)
        source = SyntheticSpeech(sampleRate, 140.0, 0x4444)
        inputLeftScope = SignalScope(sampleRate * 2)
        inputRightScope = SignalScope(sampleRate * 2)
        outputLeftScope = SignalScope(sampleRate * 2)
        outputRightScope = SignalScope(sampleRate * 2)
        inputDifferenceScope = SignalScope(sampleRate * 2)
        outputDifferenceScope = SignalScope(sampleRate * 2)
        inputLeftPlot = FloatArray(sampleRate)
        inputRightPlot = FloatArray(sampleRate)
        outputLeftPlot = FloatArray(sampleRate)
        outputRightPlot = FloatArray(sampleRate)
        inputDifferencePlot = FloatArray(sampleRate)
        outputDifferencePlot = FloatArray(sampleRate)
        beforeCorrelation = 1f
        afterCorrelation = 1f
        frames = 0
        microphone.configure(sampleRate, frameSize)
        if (shown && sourceControls.selected == PageSource.MICROPHONE) microphone.start()
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
        microphone.configure(sampleRate, frameSize)
        microphone.start()
    }

    override fun replayMicrophone() {
        sourceControls.replay()
    }

    override fun close() {
        microphone.close()
        decorrelator.close()
    }

    override fun summary(): String {
        return "decorrelator: source=${sourceControls.selected.label}, strength=$strength, frames=$frames, " +
            "correlation ${fixed(beforeCorrelation.toDouble(), 3)} -> ${fixed(afterCorrelation.toDouble(), 3)}"
    }

    /** Pearson correlation of the two interleaved channels. */
    private fun correlation(interleaved: ShortArray, frames: Int): Float {
        var sumLR = 0.0
        var sumLL = 0.0
        var sumRR = 0.0
        for (i in 0 until frames) {
            val left = interleaved[i * 2] / 32768.0
            val right = interleaved[i * 2 + 1] / 32768.0
            sumLR += left * right
            sumLL += left * left
            sumRR += right * right
        }
        val denominator = sqrt(sumLL * sumRR)
        return if (denominator <= 0.0) 0f else (sumLR / denominator).toFloat().coerceIn(-1f, 1f)
    }

    private fun peakOf(values: FloatArray): Float {
        var peak = 0f
        for (value in values) {
            val magnitude = if (value < 0f) -value else value
            if (magnitude > peak) peak = magnitude
        }
        return peak
    }

    private companion object {
        const val PLOT_WINDOW_MS = 1000.0
    }
}
