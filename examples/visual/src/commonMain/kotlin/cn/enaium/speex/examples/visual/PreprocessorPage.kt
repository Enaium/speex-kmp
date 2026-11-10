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
import cn.enaium.speex.SpeexPreprocessor
import kotlin.math.log10

/**
 * SpeexDSP preprocessor: noisy speech before and after denoise/AGC, the VAD
 * and speech probability, and the current power spectrum with its noise
 * estimate. The source is either synthetic speech or the live microphone.
 */
internal class PreprocessorPage(audio: LiveAudio) : FeaturePage("Preprocess") {

    private val sampleRate = 16_000
    private val frameSize = 320

    private var denoise = true
    private var agc = true
    private var vad = true
    private var agcLevel = 8_000f
    private var noiseDb = -42f

    private val denoiseValue = booleanArrayOf(denoise)
    private val agcValue = booleanArrayOf(agc)
    private val vadValue = booleanArrayOf(vad)
    private val agcLevelValue = floatArrayOf(agcLevel)
    private val noiseValue = floatArrayOf(noiseDb)

    private val microphone = MicrophoneSource(audio)
    private val sourceControls = SourceControls("preprocess", microphone)

    private var preprocessor = SpeexPreprocessor.create(frameSize, sampleRate)
    private var source = SyntheticSpeech(sampleRate, 130.0, 0x0BAD)
    private var rawScope = SignalScope(sampleRate * 2)
    private var processedScope = SignalScope(sampleRate * 2)
    private var rawPlot = FloatArray(sampleRate)
    private var processedPlot = FloatArray(sampleRate)
    private val probabilityHistory = HistoryRing(300)
    private val gainHistory = HistoryRing(300)
    private var probabilityPlot = FloatArray(300)
    private var gainPlot = FloatArray(300)

    private val rawSeries = PlotSeries("raw", Colors.INPUT)
    private val processedSeries = PlotSeries("processed", Colors.OUTPUT)
    private val probabilitySeries = PlotSeries("VAD probability", Colors.PROBABILITY)
    private val gainSeries = PlotSeries("AGC gain", Colors.GAIN)
    private val psdSeries = PlotSeries("PSD", Colors.LEVEL)
    private val noisePsdSeries = PlotSeries("noise PSD", Colors.NOISE)

    private var lastVad = false
    private var psdPlot = FloatArray(0)
    private var noisePsdPlot = FloatArray(0)
    private var spectrumBins = 0
    private var shown = false

    init {
        applySettings()
        microphone.configure(sampleRate, frameSize)
    }

    override fun pump() {
        if (sourceControls.selected == PageSource.MICROPHONE) {
            microphone.pump { pcm -> process(pcm) }
            return
        }
        process(
            source.nextPcm(
                frameSize,
                amplitude = 0.06f,
                noise = dbfsToAmplitude(noiseDb),
                hum = 0.01f,
            ),
        )
    }

    private fun process(pcm: ShortArray) {
        val raw = pcm.copyOf()
        val processed = pcm.copyOf()
        lastVad = preprocessor.process(processed)

        rawScope.appendPcm(raw)
        processedScope.appendPcm(processed)
        probabilityHistory.append(preprocessor.speechProbability.toFloat())
        gainHistory.append(preprocessor.agcGain.toFloat())
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

        denoiseValue[0] = denoise
        Ui.checkbox("denoise", denoiseValue) {
            denoise = denoiseValue[0]
            preprocessor.denoise = denoise
        }

        agcValue[0] = agc
        Ui.checkbox("AGC", agcValue) {
            agc = agcValue[0]
            preprocessor.agc = agc
        }

        vadValue[0] = vad
        Ui.checkbox("VAD", vadValue) {
            vad = vadValue[0]
            preprocessor.vad = vad
        }

        agcLevelValue[0] = agcLevel
        Ui.sliderFloat("AGC level", agcLevelValue, 1_000f, 20_000f, "%.0f", itemWidth = 140f) {
            agcLevel = agcLevelValue[0]
            preprocessor.agcLevel = agcLevel
        }

        noiseValue[0] = noiseDb
        Ui.sliderFloat("noise", noiseValue, -60f, -10f, "%.0f dB", itemWidth = 140f) {
            noiseDb = noiseValue[0]
        }

        Ui.button("reset###preprocess-reset") { reset() }
        sourceControls.drawStatus()

        ImGui.text(
            "frame $frameSize samples @ $sampleRate Hz | VAD ${if (lastVad) "speech" else "noise"} | " +
                "probability ${preprocessor.speechProbability}%",
        )
        ImGui.text(
            "AGC gain ${preprocessor.agcGain} dB | loudness ${preprocessor.agcLoudness} | " +
                "PSD bins ${preprocessor.psdSize}",
        )

        rawScope.snapshot(rawPlot)
        processedScope.snapshot(processedPlot)
        rawSeries.values = rawPlot
        processedSeries.values = processedPlot
        rawSeries.xScale = 1000.0 / sampleRate
        processedSeries.xScale = 1000.0 / sampleRate
        val peak = maxOf(peakOf(rawPlot), peakOf(processedPlot), 0.02f) * 1.15f

        val probabilityCount = probabilityHistory.copyTo(probabilityPlot)
        val gainCount = gainHistory.copyTo(gainPlot)
        probabilitySeries.values = probabilityPlot
        probabilitySeries.length = probabilityCount
        probabilitySeries.xScale = 1000.0 / 50.0
        gainSeries.values = gainPlot
        gainSeries.length = gainCount
        gainSeries.xScale = 1000.0 / 50.0

        updateSpectrum(preprocessor.powerSpectrum(), preprocessor.noisePowerSpectrum())
        psdSeries.values = psdPlot
        psdSeries.length = spectrumBins
        psdSeries.xScale = 1.0
        noisePsdSeries.values = noisePsdPlot
        noisePsdSeries.length = spectrumBins
        noisePsdSeries.xScale = 1.0

        val height = Ui.plotHeight(3)
        Chart.plot(
            "waveform",
            listOf(rawSeries, processedSeries),
            height,
            PLOT_WINDOW_MS,
            -peak.toDouble(),
            peak.toDouble(),
        )
        Chart.plot(
            "VAD / gain",
            listOf(probabilitySeries, gainSeries),
            height,
            HISTORY_WINDOW_MS,
            -20.0,
            100.0,
        )
        Chart.plot(
            "power spectrum",
            listOf(psdSeries, noisePsdSeries),
            height,
            maxOf(spectrumBins, 1).toDouble(),
            0.0,
            120.0,
            xLabel = "bin",
            yLabel = "dB",
        )
    }

    override fun reset() {
        preprocessor.close()
        preprocessor = SpeexPreprocessor.create(frameSize, sampleRate)
        source = SyntheticSpeech(sampleRate, 130.0, 0x0BAD)
        rawScope = SignalScope(sampleRate * 2)
        processedScope = SignalScope(sampleRate * 2)
        rawPlot = FloatArray(sampleRate)
        processedPlot = FloatArray(sampleRate)
        probabilityHistory.clear()
        gainHistory.clear()
        lastVad = false
        applySettings()

        microphone.reset()
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
        preprocessor.close()
    }

    override fun summary(): String {
        return "preprocessor: source=${sourceControls.selected.label}, denoise=$denoise, " +
            "AGC=$agc, VAD=$vad, last VAD=$lastVad, " +
            "probability=${preprocessor.speechProbability}%, gain=${preprocessor.agcGain} dB"
    }

    private fun applySettings() {
        preprocessor.denoise = denoise
        preprocessor.agc = agc
        preprocessor.vad = vad
        preprocessor.agcLevel = agcLevel
    }

    private fun updateSpectrum(psd: IntArray, noise: IntArray) {
        val bins = minOf(psd.size, noise.size)
        if (bins <= 0) {
            spectrumBins = 0
            return
        }
        if (psdPlot.size != bins) {
            psdPlot = FloatArray(bins)
            noisePsdPlot = FloatArray(bins)
        }
        for (i in 0 until bins) {
            psdPlot[i] = amplitudeDb(psd[i])
            noisePsdPlot[i] = amplitudeDb(noise[i])
        }
        spectrumBins = bins
    }

    private fun amplitudeDb(value: Int): Float {
        val magnitude = if (value < 1) 1 else value
        return (10.0 * log10(magnitude.toDouble())).toFloat().coerceIn(0f, 120f)
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
        const val HISTORY_WINDOW_MS = 300 * 20.0
    }
}
