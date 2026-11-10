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

/**
 * A sliding window of mono samples, written by a page's pump and read by its
 * draw pass. The example runs both on the render thread, so no synchronization
 * is needed.
 */
internal class SignalScope(val capacity: Int) {

    private val samples = FloatArray(capacity)
    private var writeIndex = 0
    private var size = 0

    fun clear() {
        writeIndex = 0
        size = 0
    }

    fun append(values: FloatArray, count: Int = values.size) {
        for (i in 0 until count) {
            samples[writeIndex] = values[i]
            if (++writeIndex == capacity) writeIndex = 0
        }
        size = minOf(size + count, capacity)
    }

    fun appendPcm(values: ShortArray, count: Int = values.size) {
        for (i in 0 until count) {
            samples[writeIndex] = values[i].toAmplitude()
            if (++writeIndex == capacity) writeIndex = 0
        }
        size = minOf(size + count, capacity)
    }

    /** Appends one channel of an interleaved buffer. */
    fun appendChannel(values: ShortArray, frames: Int, channel: Int, channels: Int) {
        for (i in 0 until frames) {
            samples[writeIndex] = values[i * channels + channel].toAmplitude()
            if (++writeIndex == capacity) writeIndex = 0
        }
        size = minOf(size + frames, capacity)
    }

    /**
     * Copies the newest `destination.size` samples into [destination], oldest
     * first, zero-padding the front when the scope is not full. Returns the
     * peak magnitude of the copied samples, so a plot can size its axis in the
     * same pass.
     */
    fun snapshot(destination: FloatArray): Float {
        val count = minOf(size, destination.size)
        val pad = destination.size - count
        destination.fill(0f, 0, pad)

        var index = writeIndex - count
        if (index < 0) index += capacity
        var peak = 0f
        for (i in 0 until count) {
            val value = samples[index]
            destination[pad + i] = value
            val magnitude = if (value < 0f) -value else value
            if (magnitude > peak) peak = magnitude
            if (++index == capacity) index = 0
        }
        return peak
    }
}

/** A fixed-capacity history of one value per processed frame. */
internal class HistoryRing(val capacity: Int) {
    private val values = FloatArray(capacity)
    private var writeIndex = 0
    private var size = 0

    fun clear() {
        writeIndex = 0
        size = 0
    }

    fun append(value: Float) {
        values[writeIndex] = value
        if (++writeIndex == capacity) writeIndex = 0
        size = minOf(size + 1, capacity)
    }

    /** Copies the newest `min(count, capacity)` values, oldest first. */
    fun copyTo(destination: FloatArray, count: Int = destination.size): Int {
        val copied = minOf(size, count, destination.size)
        var index = writeIndex - copied
        if (index < 0) index += capacity
        for (i in 0 until copied) {
            destination[i] = values[index]
            if (++index == capacity) index = 0
        }
        return copied
    }
}

/** One line passed to [Chart]. */
internal class PlotSeries(
    val label: String,
    val color: cn.enaium.imgui.ImVec4,
    var xScale: Double = 1.0,
) {
    var values: FloatArray = FloatArray(0)
    /** Number of valid values in [values]; negative means the whole array. */
    var length: Int = -1
    internal var envelope: FloatArray = FloatArray(0)
    internal var slice: FloatArray = FloatArray(0)
}
