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
import cn.enaium.speex.SpeexEchoCanceller
import cn.enaium.speex.SpeexPreprocessor
import kotlin.math.log10
import kotlin.random.Random

/**
 * SpeexDSP echo canceller: the microphone signal (near-end speech plus a
 * delayed, attenuated copy of the far end) and the near-end signal after
 * cancellation, the ERLE history and the adaptive impulse response.
 *
 * The near end is either synthetic speech or the live microphone; the far end
 * stays synthetic so the page always has a deterministic reference signal.
 */
internal class EchoPage(audio: LiveAudio) : FeaturePage("Echo") {

    private val sampleRate = 8_000
    private val frameSize = 160
    private val filterLengths = intArrayOf(400, 800, 1_200)
    private var filterLengthIndex = 1

    private var delaySamples = 240
    private var echoGain = 0.55f
    private var nearLevel = 0.20f
    private var residualSuppression = true

    private val filterChoice = intArrayOf(filterLengthIndex)
    private val delayValue = intArrayOf(delaySamples)
    private val echoGainValue = floatArrayOf(echoGain)
    private val nearLevelValue = floatArrayOf(nearLevel)
    private val residualValue = booleanArrayOf(residualSuppression)

    private val microphone = MicrophoneSource(audio)
    private val sourceControls = SourceControls("echo", microphone)

    private var echo = createEcho()
    private var preprocessor = createPreprocessor()
    private var delayLine = ShortArray(delayLineSize())
    private var writeIndex = 0

    private var farSource = SyntheticSpeech(sampleRate, 110.0, 0x1111)
    private var nearSource = SyntheticSpeech(sampleRate, 210.0, 0x2222)
    private val random = Random(0x3333)

    private var micScope = SignalScope(sampleRate * 2)
    private var cleanScope = SignalScope(sampleRate * 2)
    private var micPlot = FloatArray(sampleRate)
    private var cleanPlot = FloatArray(sampleRate)
    private val erleHistory = HistoryRing(300)
    private var erlePlot = FloatArray(300)
    private var impulsePlot = FloatArray(1_200)
    private var impulseLength = 0

    private val micSeries = PlotSeries("microphone", Colors.INPUT)
    private val cleanSeries = PlotSeries("cancelled", Colors.OUTPUT)
    private val erleSeries = PlotSeries("ERLE", Colors.GAIN)
    private val impulseSeries = PlotSeries("impulse response", Colors.PROBABILITY)

    private var lastErle = -20f
    private var lastMicLevel = -120f
    private var lastCleanLevel = -120f
    private var processedFrames = 0L
    private var shown = false

    init {
        applyLink()
        microphone.configure(sampleRate, frameSize)
    }

    override fun pump() {
        if (sourceControls.selected == PageSource.MICROPHONE) {
            microphone.pump { near -> process(near) }
            return
        }
        process(nearSource.nextPcm(frameSize, nearLevel, noise = 0.004f))
    }

    private fun process(near: ShortArray) {
        val far = farSource.nextPcm(frameSize, 0.25f)
        val mic = ShortArray(frameSize)
        val size = delayLine.size
        for (i in 0 until frameSize) {
            var readIndex = writeIndex - delaySamples
            if (readIndex < 0) readIndex += size
            val echoSample = (delayLine[readIndex] * echoGain).toInt()
            val noise = ((random.nextFloat() * 2f - 1f) * 0.004f * 32768f).toInt()
            mic[i] = (near[i].toInt() + echoSample + noise).coerceIn(-32768, 32767).toShort()
            delayLine[writeIndex] = far[i]
            writeIndex++
            if (writeIndex == size) writeIndex = 0
        }

        val clean = echo.cancellation(mic, far)
        if (residualSuppression) preprocessor.process(clean)

        micScope.appendPcm(mic)
        cleanScope.appendPcm(clean)

        val micLevel = rmsPcm(mic)
        val cleanLevel = rmsPcm(clean)
        lastMicLevel = dbfs(micLevel)
        lastCleanLevel = dbfs(cleanLevel)
        lastErle = if (micLevel > 0f && cleanLevel > 0f) {
            (20.0 * log10((micLevel / cleanLevel).toDouble())).toFloat().coerceIn(-20f, 60f)
        } else {
            -20f
        }
        erleHistory.append(lastErle)
        processedFrames++

        if (processedFrames % 10L == 0L) updateImpulse()
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

        filterChoice[0] = filterLengthIndex
        Ui.combo("filter", filterChoice, FILTER_LABELS, itemWidth = 110f) {
            filterLengthIndex = filterChoice[0]
            recreate()
        }

        delayValue[0] = delaySamples
        Ui.sliderInt("echo delay", delayValue, 80, 600, "%d smp", itemWidth = 150f) {
            delaySamples = delayValue[0]
        }

        echoGainValue[0] = echoGain
        Ui.sliderFloat("echo gain", echoGainValue, 0f, 1f, "%.2f", itemWidth = 120f) {
            echoGain = echoGainValue[0]
        }

        nearLevelValue[0] = nearLevel
        Ui.sliderFloat("near level", nearLevelValue, 0f, 0.5f, "%.2f", itemWidth = 120f) {
            nearLevel = nearLevelValue[0]
        }

        residualValue[0] = residualSuppression
        Ui.checkbox("residual suppression", residualValue) {
            residualSuppression = residualValue[0]
        }

        Ui.button("reset###echo-reset") { reset() }
        sourceControls.drawStatus()

        ImGui.text(
            "filter ${filterLengths[filterLengthIndex]} taps, delay $delaySamples samples, " +
                "frames=$processedFrames",
        )
        ImGui.text(
            "ERLE ${fixed(lastErle.toDouble(), 1)} dB | microphone ${dbText(lastMicLevel)} dBFS | " +
                "cancelled ${dbText(lastCleanLevel)} dBFS",
        )

        micScope.snapshot(micPlot)
        cleanScope.snapshot(cleanPlot)
        micSeries.values = micPlot
        cleanSeries.values = cleanPlot
        val xScale = 1000.0 / sampleRate
        micSeries.xScale = xScale
        cleanSeries.xScale = xScale
        val peak = maxOf(peakOf(micPlot), peakOf(cleanPlot), 0.02f) * 1.15f

        val erleCount = erleHistory.copyTo(erlePlot)
        erleSeries.values = erlePlot
        erleSeries.length = erleCount
        erleSeries.xScale = 1000.0 / 50.0

        impulseSeries.values = impulsePlot
        impulseSeries.length = impulseLength
        impulseSeries.xScale = 1.0

        val height = Ui.plotHeight(3)
        Chart.plot(
            "microphone / cancelled",
            listOf(micSeries, cleanSeries),
            height,
            PLOT_WINDOW_MS,
            -peak.toDouble(),
            peak.toDouble(),
        )
        Chart.plot(
            "ERLE",
            listOf(erleSeries),
            height,
            HISTORY_WINDOW_MS,
            -20.0,
            60.0,
            yLabel = "dB",
        )
        Chart.plot(
            "impulse response",
            listOf(impulseSeries),
            height,
            maxOf(filterLengths[filterLengthIndex], 1).toDouble(),
            -1.0,
            1.0,
            xLabel = "taps",
            yLabel = "normalised",
        )
    }

    override fun reset() {
        microphone.reset()
        echo.close()
        preprocessor.close()
        echo = createEcho()
        preprocessor = createPreprocessor()
        delayLine = ShortArray(delayLineSize())
        writeIndex = 0
        farSource = SyntheticSpeech(sampleRate, 110.0, 0x1111)
        nearSource = SyntheticSpeech(sampleRate, 210.0, 0x2222)
        micScope = SignalScope(sampleRate * 2)
        cleanScope = SignalScope(sampleRate * 2)
        micPlot = FloatArray(sampleRate)
        cleanPlot = FloatArray(sampleRate)
        erleHistory.clear()
        impulseLength = 0
        lastErle = -20f
        lastMicLevel = -120f
        lastCleanLevel = -120f
        processedFrames = 0
        applyLink()
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
        echo.close()
        preprocessor.close()
    }

    override fun summary(): String {
        return "echo: source=${sourceControls.selected.label}, " +
            "filter=${filterLengths[filterLengthIndex]} taps, delay=$delaySamples samples, " +
            "frames=$processedFrames, ERLE=${fixed(lastErle.toDouble(), 1)} dB"
    }

    private fun recreate() {
        echo.close()
        preprocessor.close()
        echo = createEcho()
        preprocessor = createPreprocessor()
        delayLine = ShortArray(delayLineSize())
        writeIndex = 0
        erleHistory.clear()
        impulseLength = 0
        processedFrames = 0
        applyLink()
        microphone.configure(sampleRate, frameSize)
        if (shown && sourceControls.selected == PageSource.MICROPHONE) microphone.start()
    }

    private fun createEcho(): SpeexEchoCanceller =
        SpeexEchoCanceller.create(frameSize, filterLengths[filterLengthIndex])

    private fun createPreprocessor(): SpeexPreprocessor =
        SpeexPreprocessor.create(frameSize, sampleRate).apply {
            denoise = false
            agc = false
            vad = false
        }

    private fun applyLink() {
        preprocessor.setEchoState(echo)
    }

    private fun delayLineSize(): Int = maxOf(filterLengths[filterLengthIndex] + frameSize * 2, 1_400)

    private fun updateImpulse() {
        val impulse = echo.impulseResponse()
        if (impulse.isEmpty()) return
        if (impulsePlot.size < impulse.size) impulsePlot = FloatArray(impulse.size)
        var peak = 0
        for (value in impulse) {
            val magnitude = if (value < 0) -value else value
            if (magnitude > peak) peak = magnitude
        }
        val divisor = if (peak == 0) 1.0f else peak.toFloat()
        for (i in impulse.indices) impulsePlot[i] = impulse[i] / divisor
        impulseLength = impulse.size
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
        val FILTER_LABELS = arrayOf("400", "800", "1200")
    }
}
