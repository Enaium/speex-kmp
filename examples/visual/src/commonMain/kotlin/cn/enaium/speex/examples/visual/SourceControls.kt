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

/**
 * The source combo shared by every page that analyses a time-domain signal.
 *
 * Synthetic speech is the default so a page has something to show without a
 * device. Choosing the microphone makes that page consume live audio instead;
 * Record/Pause, Monitor and Replay then operate on the same source. Each page
 * passes a unique [id] so the widgets cannot collide with another page's.
 */
internal class SourceControls(
    private val id: String,
    val microphone: MicrophoneSource,
    initial: PageSource = PageSource.SYNTHETIC,
) {

    var selected: PageSource = initial
        private set

    private val sourceChoice = intArrayOf(initial.ordinal)
    private val monitorValue = booleanArrayOf(false)

    fun draw(onChange: (PageSource) -> Unit) {
        sourceChoice[0] = selected.ordinal
        Ui.combo("source###$id-source", sourceChoice, SOURCE_LABELS, itemWidth = 150f) {
            val next = PageSource.entries[sourceChoice[0]]
            if (next != selected) {
                selected = next
                onChange(next)
            }
        }

        if (selected != PageSource.MICROPHONE) return

        if (microphone.recording) {
            Ui.button("Pause###$id-mic-pause") { microphone.stop() }
        } else {
            Ui.button("Record###$id-mic-record") { microphone.start() }
        }

        monitorValue[0] = microphone.monitoring
        Ui.checkbox("monitor###$id-mic-monitor", monitorValue) {
            microphone.setMonitoring(monitorValue[0])
        }

        Ui.button("Replay###$id-mic-replay") { microphone.replayLast() }
    }

    /** Draws the device/status line under the controls. */
    fun drawStatus() {
        if (selected != PageSource.MICROPHONE) return
        ImGui.textWrapped("mic: ${microphone.status}")
        Ui.newRow()
    }

    /** Selects a source without drawing, for the automation flags. */
    fun select(source: PageSource) {
        selected = source
        sourceChoice[0] = source.ordinal
    }

    /** Starts the microphone after [select]. */
    fun record() {
        select(PageSource.MICROPHONE)
        microphone.start()
    }

    /** Replays the newest captured window. */
    fun replay() {
        microphone.replayLast()
    }

    private companion object {
        val SOURCE_LABELS: Array<String> = PageSource.entries.map { it.label }.toTypedArray()
    }
}
