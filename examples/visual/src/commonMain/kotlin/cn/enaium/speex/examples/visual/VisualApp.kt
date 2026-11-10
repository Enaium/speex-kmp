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
import cn.enaium.imgui.ImGuiCond
import cn.enaium.imgui.ImGuiWindowFlags
import cn.enaium.imgui.ImVec2

/**
 * The visual example: one fullscreen ImGui window with a page selector.
 *
 * Only the selected page is pumped. That keeps histories focused on what is
 * visible and, more importantly, lets a page own the microphone while it is on
 * screen without several pages competing for the capture stream.
 */
internal class VisualApp(startPage: Int = 0) {

    private val audio = LiveAudio()

    private val pages: List<FeaturePage> = listOf(
        CodecPage(audio),
        ResamplerPage(audio),
        PreprocessorPage(audio),
        EchoPage(audio),
        DecorrelatorPage(audio),
        JitterPage(),
        RingBufferPage(),
    )

    private var selected = startPage.coerceIn(0, pages.lastIndex)
    private var running = true
    private var frameIndex = 0
    private var replayAt = -1

    init {
        pages[selected].onShown()
    }

    fun draw() {
        if (frameIndex == replayAt) replayMicrophone()
        frameIndex++
        if (running) {
            pages[selected].pump()
        }

        val displaySize = ImGui.getIO().displaySize
        ImGui.setNextWindowPos(ImVec2(0f, 0f), ImGuiCond.ALWAYS)
        ImGui.setNextWindowSize(displaySize, ImGuiCond.ALWAYS)
        if (ImGui.begin("speex-kmp visual example", null, WINDOW_FLAGS)) {
            Ui.beginPage()
            for ((index, page) in pages.withIndex()) {
                Ui.button("${if (index == selected) "[${page.title}]" else page.title}###nav-$index") {
                    select(index)
                }
            }
            Ui.button(if (running) "Pause###global-transport" else "Resume###global-transport") { running = !running }
            Ui.button("Reset###global-reset") { pages.forEach { it.reset() } }
            ImGui.separator()
            pages[selected].draw()
        }
        ImGui.end()
    }

    fun printSummary() {
        for (page in pages) report(page.summary())
    }

    fun close() {
        for (page in pages) page.close()
        audio.close()
    }

    /** Selects the microphone on the visible page, used by `--record`. */
    fun startMicrophone() {
        pages[selected].startMicrophone()
    }

    /** Replays the visible page's newest microphone window, used by `--replay`. */
    fun replayMicrophone() {
        pages[selected].replayMicrophone()
    }

    /** Schedules one replay after [frames] UI frames; negative disables it. */
    fun scheduleReplay(frames: Int) {
        replayAt = frames
    }

    private fun select(index: Int) {
        val next = index.coerceIn(0, pages.lastIndex)
        if (next == selected) return
        pages[selected].onHidden()
        selected = next
        pages[selected].onShown()
    }

    private companion object {
        /** Fullscreen host window: no title bar, nothing to drag or resize. */
        const val WINDOW_FLAGS = ImGuiWindowFlags.NO_TITLE_BAR or
            ImGuiWindowFlags.NO_RESIZE or
            ImGuiWindowFlags.NO_MOVE or
            ImGuiWindowFlags.NO_SCROLLBAR or
            ImGuiWindowFlags.NO_COLLAPSE or
            ImGuiWindowFlags.NO_SAVED_SETTINGS or
            ImGuiWindowFlags.NO_BRING_TO_FRONT_ON_FOCUS or
            ImGuiWindowFlags.NO_NAV_FOCUS
    }
}

/**
 * Runs the visual example until the window closes or [frames] frames were
 * drawn. Returns `false` when no window could be opened.
 */
fun runVisualExample(
    frames: Int = Int.MAX_VALUE,
    page: Int = 0,
    record: Boolean = false,
    replay: Boolean = false,
): Boolean {
    report("speex-kmp visual example (frames=$frames, page=$page, record=$record, replay=$replay)")
    val app = VisualApp(page)
    // Replay needs a capture window, so it implies the microphone source too.
    if (record || replay) app.startMicrophone()
    if (replay) app.scheduleReplay(if (frames == Int.MAX_VALUE) 90 else (frames * 3 / 4).coerceIn(1, 240))
    var closed = false
    val displayed = ImGuiSdlApp.run(
        "speex-kmp visual example",
        frames,
        onClose = {
            app.printSummary()
            app.close()
            closed = true
        },
    ) { app.draw() }
    // No window was opened, so the normal close hook did not run.
    if (!closed) {
        app.printSummary()
        app.close()
    }
    report(if (displayed) "done" else "aborted")
    return displayed
}
