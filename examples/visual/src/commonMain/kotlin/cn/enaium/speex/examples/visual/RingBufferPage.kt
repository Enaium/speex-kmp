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
import cn.enaium.speex.SpeexRingBuffer

/**
 * SpeexDSP ring buffer: a producer writes a chunk every tick, a consumer reads
 * another, and the occupancy history shows the buffer filling up or draining.
 */
internal class RingBufferPage : FeaturePage("Ring buffer") {

    private val sizes = intArrayOf(32, 64, 128, 256)
    private var sizeIndex = 1
    private var writeChunk = 8
    private var readChunk = 4

    private val sizeChoice = intArrayOf(sizeIndex)
    private val writeValue = intArrayOf(writeChunk)
    private val readValue = intArrayOf(readChunk)

    private var buffer = SpeexRingBuffer.create(sizes[sizeIndex])
    private val occupancyHistory = HistoryRing(300)
    private var occupancyPlot = FloatArray(300)
    private val occupancySeries = PlotSeries("occupancy", Colors.OCCUPANCY)

    private var counter = 0
    private var totalWritten = 0L
    private var totalRead = 0L
    private var lastRead = ByteArray(0)
    private val chunk = ByteArray(64)

    override fun pump() {
        val writeCount = minOf(writeChunk, chunk.size)
        for (i in 0 until writeCount) {
            chunk[i] = counter++.toByte()
        }
        buffer.write(chunk, 0, writeCount)
        totalWritten += writeCount

        lastRead = buffer.read(readChunk)
        totalRead += lastRead.size
        occupancyHistory.append(buffer.available.toFloat())
    }

    override fun draw() {
        Ui.beginPage()

        sizeChoice[0] = sizeIndex
        Ui.combo("size", sizeChoice, SIZE_LABELS, itemWidth = 100f) {
            sizeIndex = sizeChoice[0]
            recreate()
        }

        writeValue[0] = writeChunk
        Ui.sliderInt("write", writeValue, 0, 32, "%d bytes", itemWidth = 130f) {
            writeChunk = writeValue[0]
        }

        readValue[0] = readChunk
        Ui.sliderInt("read", readValue, 0, 32, "%d bytes", itemWidth = 130f) {
            readChunk = readValue[0]
        }

        Ui.button("reset###ring-reset") { recreate() }

        ImGui.text(
            "capacity=${sizes[sizeIndex]} bytes, available=${buffer.available} | " +
                "written=$totalWritten, read=$totalRead",
        )
        ImGui.text("last read: ${hex(lastRead)}")

        val count = occupancyHistory.copyTo(occupancyPlot)
        occupancySeries.values = occupancyPlot
        occupancySeries.length = count
        occupancySeries.xScale = 1000.0 / 50.0

        val height = Ui.plotHeight(1)
        Chart.plot(
            "ring buffer occupancy",
            listOf(occupancySeries),
            height,
            HISTORY_WINDOW_MS,
            0.0,
            (sizes[sizeIndex] * 1.1),
            yLabel = "bytes",
        )
    }

    override fun reset() {
        recreate()
    }

    override fun summary(): String {
        return "ring buffer: capacity=${sizes[sizeIndex]}, written=$totalWritten, read=$totalRead, " +
            "available=${buffer.available}"
    }

    private fun recreate() {
        buffer.close()
        buffer = SpeexRingBuffer.create(sizes[sizeIndex])
        occupancyHistory.clear()
        counter = 0
        totalWritten = 0
        totalRead = 0
        lastRead = ByteArray(0)
    }

    private fun hex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "-"
        return bytes.joinToString(" ") { byte ->
            val value = byte.toInt() and 0xFF
            val text = value.toString(16)
            if (text.length == 1) "0$text" else text
        }
    }

    private companion object {
        const val HISTORY_WINDOW_MS = 300 * 20.0
        val SIZE_LABELS = arrayOf("32", "64", "128", "256")
    }
}
