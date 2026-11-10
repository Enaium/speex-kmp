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

import cn.enaium.imgui.ImVec2
import cn.enaium.imgui.ImVec4
import cn.enaium.imgui.extensions.implot.ImPlot
import cn.enaium.imgui.extensions.implot.ImPlotAxisFlags
import cn.enaium.imgui.extensions.implot.ImPlotCond
import cn.enaium.imgui.extensions.implot.ImPlotSpec

/** Colors shared by the pages. */
internal object Colors {
    val INPUT = ImVec4(0.95f, 0.45f, 0.35f, 1f)
    val OUTPUT = ImVec4(0.35f, 0.85f, 0.5f, 1f)
    val REFERENCE = ImVec4(0.55f, 0.55f, 0.65f, 1f)
    val GAIN = ImVec4(0.45f, 0.65f, 0.95f, 1f)
    val PROBABILITY = ImVec4(0.95f, 0.8f, 0.35f, 1f)
    val NOISE = ImVec4(0.7f, 0.4f, 0.9f, 1f)
    val LEVEL = ImVec4(0.4f, 0.9f, 0.9f, 1f)
    val OCCUPANCY = ImVec4(0.4f, 0.75f, 0.95f, 1f)
    val DELAY = ImVec4(0.9f, 0.6f, 0.3f, 1f)
}

/**
 * Draws one or more line series over a fixed rectangle.
 *
 * Wide arrays are reduced to one min/max pair per pixel column, so a ten second
 * waveform costs the same as a one second one: the raw line would be hundreds
 * of thousands of calls into the C++ renderer for a picture that cannot show
 * the difference.
 */
internal object Chart {

    fun plot(
        title: String,
        series: List<PlotSeries>,
        height: Float,
        xMax: Double,
        yMin: Double,
        yMax: Double,
        xLabel: String = "milliseconds",
        yLabel: String = "amplitude",
    ) {
        if (!ImPlot.beginPlot(title, ImVec2(-1f, height))) return
        ImPlot.setupAxes(xLabel, yLabel, ImPlotAxisFlags.NONE, ImPlotAxisFlags.NONE)
        ImPlot.setupAxesLimits(0.0, xMax, yMin, yMax, ImPlotCond.ALWAYS)

        val columns = maxOf(ImPlot.getPlotSize().x.toInt(), 1)
        for (line in series) {
            val valueCount = if (line.length in 0..line.values.size) line.length else line.values.size
            if (valueCount <= 0) continue
            val values = if (valueCount == line.values.size) {
                line.values
            } else {
                if (line.slice.size != valueCount) line.slice = FloatArray(valueCount)
                line.values.copyInto(line.slice, 0, 0, valueCount)
                line.slice
            }
            val samplesPerColumn = (values.size + columns - 1) / columns
            if (samplesPerColumn <= 1) {
                ImPlot.plotLine(
                    line.label,
                    values,
                    xScale = line.xScale,
                    spec = ImPlotSpec(lineColor = line.color, lineWeight = 1f),
                )
            } else {
                val usedColumns = (values.size + samplesPerColumn - 1) / samplesPerColumn
                val written = usedColumns * 2
                if (line.envelope.size != written) line.envelope = FloatArray(written)
                envelopeOf(values, samplesPerColumn, line.envelope)
                ImPlot.plotLine(
                    line.label,
                    line.envelope,
                    xScale = line.xScale * samplesPerColumn / 2.0,
                    spec = ImPlotSpec(lineColor = line.color, lineWeight = 1f),
                )
            }
        }
        ImPlot.endPlot()
    }

    /** Fills [destination] with one min/max pair per column, in sample order. */
    private fun envelopeOf(values: FloatArray, samplesPerColumn: Int, destination: FloatArray) {
        var written = 0
        var start = 0
        while (start < values.size && written + 1 < destination.size) {
            val end = minOf(start + samplesPerColumn, values.size)
            var lowest = start
            var highest = start
            for (i in start until end) {
                if (values[i] < values[lowest]) lowest = i
                if (values[i] > values[highest]) highest = i
            }
            destination[written++] = values[minOf(lowest, highest)]
            destination[written++] = values[maxOf(lowest, highest)]
            start = end
        }
    }
}
