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
import cn.enaium.speex.SpeexJitterBuffer
import cn.enaium.speex.SpeexJitterStatus
import kotlin.random.Random

/**
 * SpeexDSP jitter buffer: a packet stream with configurable network jitter and
 * loss, consumed at a steady playback pace. The plots show the occupancy and
 * the buffering delay; the counters show how many packets were returned,
 * concealed or inserted to grow the buffer.
 */
internal class JitterPage : FeaturePage("Jitter") {

    private val frameUnits = 160
    private val stepSize = 160

    private var jitterFrames = 3
    private var lossPercent = 5

    private val jitterValue = intArrayOf(jitterFrames)
    private val lossValue = intArrayOf(lossPercent)

    private var jitter = SpeexJitterBuffer.create(stepSize)
    private val random = Random(0x7777)
    private val pending = ArrayList<Pending>()

    private val occupancyHistory = HistoryRing(300)
    private val delayHistory = HistoryRing(300)
    private var occupancyPlot = FloatArray(300)
    private var delayPlot = FloatArray(300)

    private val occupancySeries = PlotSeries("buffered packets", Colors.OCCUPANCY)
    private val delaySeries = PlotSeries("delay", Colors.DELAY)

    private var tick = 0L
    private var nextTimestamp = 0L
    private var sequence = 0
    private var generated = 0L
    private var dropped = 0L
    private var delivered = 0L
    private var ok = 0L
    private var missing = 0L
    private var insertion = 0L
    private var expectedPlayback = 0L

    override fun pump() {
        // A packet is produced every tick; it may be lost, and otherwise
        // arrives after a random network delay of 0..jitterFrames ticks.
        val timestamp = nextTimestamp
        nextTimestamp += frameUnits
        sequence++
        if (random.nextInt(100) >= lossPercent) {
            val due = tick + random.nextInt(0, jitterFrames + 1)
            pending.add(
                Pending(
                    due = due,
                    timestamp = timestamp.toInt(),
                    sequence = sequence,
                    data = byteArrayOf(
                        (timestamp and 0xFF).toByte(),
                        ((timestamp ushr 8) and 0xFF).toByte(),
                        (sequence and 0xFF).toByte(),
                    ),
                ),
            )
            generated++
        } else {
            dropped++
        }
        tick++

        // Deliver every packet whose arrival time has come. Sorting keeps
        // out-of-order arrivals arriving in the order the network delivered
        // them, which is what the jitter buffer has to reorder.
        pending.sortBy { it.due }
        while (pending.isNotEmpty() && pending.first().due <= tick) {
            val packet = pending.removeAt(0)
            jitter.put(
                data = packet.data,
                timestamp = packet.timestamp,
                span = frameUnits,
                sequence = packet.sequence,
                userData = 0,
            )
            delivered++
        }

        val result = jitter.get(frameUnits)
        when (result.status) {
            SpeexJitterStatus.OK -> ok++
            SpeexJitterStatus.MISSING -> missing++
            SpeexJitterStatus.INSERTION -> insertion++
            else -> Unit
        }
        jitter.tick()

        occupancyHistory.append(jitter.available.toFloat())
        val delay = if (result.status == SpeexJitterStatus.OK) {
            (result.packet.timestamp - expectedPlayback).toFloat() / frameUnits
        } else {
            0f
        }
        delayHistory.append(delay)
        expectedPlayback += frameUnits
    }

    override fun draw() {
        Ui.beginPage()

        jitterValue[0] = jitterFrames
        Ui.sliderInt("network jitter", jitterValue, 0, 8, "%d frames", itemWidth = 150f) {
            jitterFrames = jitterValue[0]
        }

        lossValue[0] = lossPercent
        Ui.sliderInt("loss", lossValue, 0, 30, "%d %%", itemWidth = 150f) {
            lossPercent = lossValue[0]
        }

        Ui.button("reset###jitter-reset") { reset() }

        ImGui.text(
            "tick=$tick, packets generated=$generated, delivered=$delivered, lost=$dropped | " +
                "buffer occupancy=${jitter.available}",
        )
        ImGui.text(
            "playback: OK=$ok, missing=$missing, insertion=$insertion | " +
                "pointer timestamp=${jitter.pointerTimestamp}",
        )

        val occupancyCount = occupancyHistory.copyTo(occupancyPlot)
        occupancySeries.values = occupancyPlot
        occupancySeries.length = occupancyCount
        occupancySeries.xScale = 1000.0 / 50.0

        val delayCount = delayHistory.copyTo(delayPlot)
        delaySeries.values = delayPlot
        delaySeries.length = delayCount
        delaySeries.xScale = 1000.0 / 50.0

        val height = Ui.plotHeight(2)
        Chart.plot(
            "jitter buffer occupancy",
            listOf(occupancySeries),
            height,
            HISTORY_WINDOW_MS,
            0.0,
            10.0,
            yLabel = "packets",
        )
        Chart.plot(
            "buffering delay",
            listOf(delaySeries),
            height,
            HISTORY_WINDOW_MS,
            -2.0,
            8.0,
            yLabel = "frames",
        )
    }

    override fun reset() {
        jitter.close()
        jitter = SpeexJitterBuffer.create(stepSize)
        pending.clear()
        occupancyHistory.clear()
        delayHistory.clear()
        tick = 0
        nextTimestamp = 0
        sequence = 0
        generated = 0
        dropped = 0
        delivered = 0
        ok = 0
        missing = 0
        insertion = 0
        expectedPlayback = 0
    }

    override fun summary(): String {
        return "jitter: jitter=$jitterFrames frames, loss=$lossPercent%, generated=$generated, " +
            "delivered=$delivered, OK=$ok, MISSING=$missing, INSERTION=$insertion"
    }

    private class Pending(
        val due: Long,
        val timestamp: Int,
        val sequence: Int,
        val data: ByteArray,
    )

    private companion object {
        const val HISTORY_WINDOW_MS = 300 * 20.0
    }
}
